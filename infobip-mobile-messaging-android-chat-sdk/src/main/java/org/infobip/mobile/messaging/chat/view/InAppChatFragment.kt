package org.infobip.mobile.messaging.chat.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.ActionBar
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.infobip.mobile.messaging.ConfigurationException
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.InAppChat
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachmentHelper
import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment
import org.infobip.mobile.messaging.chat.core.InAppChatWidgetView
import org.infobip.mobile.messaging.chat.databinding.IbFragmentChatBinding
import org.infobip.mobile.messaging.chat.utils.*
import org.infobip.mobile.messaging.chat.view.styles.InAppChatToolbarStyle
import org.infobip.mobile.messaging.chat.view.styles.apply
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.mobileapi.InternalSdkError
import org.infobip.mobile.messaging.permissions.PermissionsRequestManager
import org.infobip.mobile.messaging.permissions.PermissionsRequestManager.PermissionsRequester
import org.infobip.mobile.messaging.util.SystemInformation
import java.util.*

class InAppChatFragment : Fragment(), PermissionsRequester, InAppChatView.EventsListener {

    /**
     * Implement InAppChatActionBarProvider in your Activity, where InAppChatWebViewFragment will be added.
     */
    interface InAppChatActionBarProvider {
        /**
         * Provide original ActionBar, to give in-app chat ability to hide it and use it's own ActionBar.
         * It will be hidden when in-app Chat fragment shown and returned back, when in-app Chat fragment hidden.
         */
        val originalSupportActionBar: ActionBar?

        /**
         * Implement back button behaviour.
         * <br>
         * Call following method with corresponding parameter:
         * <br>
         * [InAppChat.hideInAppChatFragment]
         */
        fun onInAppChatBackPressed()
    }

    companion object {
        private const val USER_INPUT_CHECKER_DELAY_MS = 250
    }

    private var _binding: IbFragmentChatBinding? = null
    private val binding
        get() = _binding!!

    @ColorInt
    private var originalStatusBarColor = 0
    private var originalLightStatusBar: Boolean? = null
    private lateinit var permissionsRequestManager: PermissionsRequestManager
    private lateinit var localizationUtils: LocalizationUtils
    private var capturedImageUri: Uri? = null
    private var capturedVideoUri: Uri? = null
    private var widgetInfo: WidgetInfo? = null
    private var widgetView: InAppChatWidgetView? = null
    private val isMultiThread
        get() = binding.ibLcChat.isMultiThread
    private val backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigateBack()
        }
    }
    private val inputFinishChecker: InAppChatInputFinishChecker =
        InAppChatInputFinishChecker {
            if (_binding != null)
                binding.ibLcChat.sendInputDraft(it)
        }
    private val inputCheckerHandler = Handler(Looper.getMainLooper())

    /**
     * Allows another way how to inject InAppChatActionBarProvider to InAppChatFragment.
     * It is used in React Native plugin to handle multithread navigation.
     */
    var inAppChatActionBarProvider: InAppChatActionBarProvider? = null
        get() {
            return field ?: (requireActivity() as? InAppChatActionBarProvider)
        }

    /**
     * Allows to hide Toolbar in InAppChatFragment.
     * It is used in React Native plugin, ChatView UI component is without toolbar.
     */
    var withToolbar = true
        set(value) {
            field = value
            _binding?.ibLcChatToolbar?.show(value)
        }

    //region Lifecycle
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val contextThemeWrapper = ContextThemeWrapper(
            requireContext(),
            InAppChatThemeResolver.getChatViewTheme(requireContext())
        )
        return IbFragmentChatBinding.inflate(
            inflater.cloneInContext(contextThemeWrapper),
            container,
            false
        ).also { _binding = it }.root
    }

    override fun getContext(): Context {
        val context = super.getContext() ?: requireActivity()
        return ContextThemeWrapper(context, InAppChatThemeResolver.getChatViewTheme(context))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (this.isHidden)
            return //on configChange (uiMode) fragment is recreated and this fun is called, skip init views
        permissionsRequestManager = PermissionsRequestManager(this, this)
        localizationUtils = LocalizationUtils.getInstance(requireContext())
        initViews()
        initBackPressHandler()
    }

    /**
     * Logic needed to support InAppChat.showInAppChatFragment()/InAppChat.hideInAppChatFragment()
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            initToolbar()
            widgetInfo?.let { updateViews(it) }
        }
        backPressedCallback.isEnabled = !isHidden
    }

    override fun onPause() {
        super.onPause()
        sendInputDraftImmediately()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        revertStatusBarStyle() //called because of react native plugin UI component
        removeBackPressHandler()
        binding.ibLcChat.eventsListener = null
        _binding = null
    }

    private fun initViews() {
        binding.ibLcChatInput.isEnabled = false
        initChat()
        initToolbar()
        initChatInput()
    }

    private fun updateViews(widgetInfo: WidgetInfo) {
        applyToolbarStyle(widgetInfo)
        applyChatInputStyle(widgetInfo)
        if (!isMultiThread) {
            setChatInputVisibility(true)
        }
    }
    //endregion

    //region Public
    fun setLanguage(locale: Locale) {
        MobileMessagingLogger.d("InAppChatFragment", "setLanguage($locale)")
        binding.ibLcChat.setLanguage(locale)
    }

    fun sendContextualMetaData(data: String, allMultiThreadStrategy: Boolean) {
        binding.ibLcChat.sendContextualMetaData(data, allMultiThreadStrategy)
    }
    //endregion

    //region Toolbar
    private fun initToolbar() {
        if (!withToolbar) {
            if (binding.ibLcChatToolbar.isVisible)
                binding.ibLcChatToolbar.hide()
            return
        }
        //If Activity has it's own ActionBar, it should be hidden.
        inAppChatActionBarProvider?.originalSupportActionBar?.hide()
        binding.ibLcChatToolbar.setNavigationOnClickListener { navigateBack() }
    }

    private fun navigateBack() {
        if (!withToolbar)
            return
        binding.ibLcChatInput.hideKeyboard()
        val view = widgetView
        if (isMultiThread && view != null) {
            when (view) {
                InAppChatWidgetView.LOADING, InAppChatWidgetView.THREAD_LIST, InAppChatWidgetView.SINGLE_MODE_THREAD -> closeChatPage()
                InAppChatWidgetView.THREAD, InAppChatWidgetView.LOADING_THREAD, InAppChatWidgetView.CLOSED_THREAD -> showThreadList()
            }
        } else {
            closeChatPage()
        }
    }

    private fun closeChatPage() {
        revertStatusBarStyle()
        inAppChatActionBarProvider?.originalSupportActionBar?.show()
        backPressedCallback.isEnabled = false //when InAppChat is used as Activity need to disable callback before onBackPressed() is called to avoid endless loop
        inAppChatActionBarProvider?.onInAppChatBackPressed()
    }

    fun showThreadList() = binding.ibLcChat.showThreadList()

    private fun applyToolbarStyle(widgetInfo: WidgetInfo) {
        if (!withToolbar)
            return
        val style = InAppChatToolbarStyle.createChatToolbarStyle(requireContext(), widgetInfo)
        binding.ibLcChatToolbar.setNavigationIcon(style.navigationIcon)
        style.apply(binding.ibLcChatToolbar)
        applyStatusBarStyle(style)
    }

    private fun applyStatusBarStyle(style: InAppChatToolbarStyle) {
        requireActivity().apply {
            val currentColor = getStatusBarColor() ?: 0
            if (currentColor != style.statusBarBackgroundColor)
                this@InAppChatFragment.originalStatusBarColor = currentColor
            setStatusBarColor(style.statusBarBackgroundColor)
            val currentMode = isLightStatusBarMode()
            if (currentMode != (!style.lightStatusBarIcons))
                this@InAppChatFragment.originalLightStatusBar = currentMode
            setLightStatusBarMode(!style.lightStatusBarIcons)
        }
    }

    private fun revertStatusBarStyle() {
        if (originalStatusBarColor != 0)
            requireActivity().setStatusBarColor(originalStatusBarColor)
        originalLightStatusBar?.let {
            requireActivity().setLightStatusBarMode(it)
        }
    }
    //endregion

    //region Chat
    private fun initChat() = with(binding.ibLcChat) {
        eventsListener = this@InAppChatFragment
        init(viewLifecycleOwner.lifecycle)
    }

    override fun onChatLoaded(controlsEnabled: Boolean) {
        binding.ibLcChatInput.isEnabled = controlsEnabled
    }

    override fun onChatControlsVisibilityChanged(isVisible: Boolean) {
        setChatInputVisibility(isVisible)
    }

    override fun onAttachmentPreviewOpened(url: String?, type: String?, caption: String?) {
        val intent =
            InAppChatAttachmentPreviewActivity.startIntent(requireContext(), url, type, caption)
        startActivity(intent)
    }

    override fun onChatViewChanged(widgetView: InAppChatWidgetView) {
        this.widgetView = widgetView
        updateViewsVisibilityByMultiThreadView(widgetView)
    }

    override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {
        this.widgetInfo = widgetInfo
        updateViews(widgetInfo)
    }
    //endregion

    //region ChatInput
    private fun initChatInput() {
        initTextBar()
        initSendButton()
        initAttachmentButton()
    }

    private fun initTextBar() {
        binding.ibLcChatInput.addInputTextChangeListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // nothing
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                inputCheckerHandler.removeCallbacks(inputFinishChecker)
                binding.ibLcChatInput.setSendButtonEnabled(s.isNotEmpty())
            }

            override fun afterTextChanged(s: Editable) {
                inputFinishChecker.setInputValue(s.toString())
                inputCheckerHandler.postDelayed(
                    inputFinishChecker,
                    USER_INPUT_CHECKER_DELAY_MS.toLong()
                )
            }
        })
        binding.ibLcChatInput.setInputFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus)
                binding.ibLcChatInput.hideKeyboard()
        }
    }

    private fun initSendButton() = with(binding.ibLcChatInput) {
        setSendButtonClickListener {
            getInputText()?.let { msg ->
                if (msg.isNotBlank()) {
                    binding.ibLcChat.sendChatMessage(CommonUtils.escapeJsonString(msg))
                }
                clearInputText()
            }
        }
    }

    private fun initAttachmentButton() {
        binding.ibLcChatInput.setAttachmentButtonClickListener { chooseFile() }
    }

    private fun applyChatInputStyle(widgetInfo: WidgetInfo) {
        binding.ibLcChatInput.applyWidgetInfoStyle(widgetInfo)
    }

    private fun updateViewsVisibilityByMultiThreadView(widgetView: InAppChatWidgetView) {
        if (isMultiThread) {
            when (widgetView) {
                InAppChatWidgetView.THREAD, InAppChatWidgetView.SINGLE_MODE_THREAD -> setChatInputVisibility(
                    true
                )
                InAppChatWidgetView.LOADING, InAppChatWidgetView.THREAD_LIST, InAppChatWidgetView.CLOSED_THREAD, InAppChatWidgetView.LOADING_THREAD -> setChatInputVisibility(
                    false
                )
            }
        } else {
            setChatInputVisibility(true)
        }
    }

    private fun setChatInputVisibility(isVisible: Boolean) {
        val canShowInMultiThread =
            isMultiThread && (widgetView == InAppChatWidgetView.THREAD || widgetView == InAppChatWidgetView.SINGLE_MODE_THREAD)
        val isNotMultiThread: Boolean = !isMultiThread
        val isVisibleMultiThreadSafe = isVisible && (canShowInMultiThread || isNotMultiThread)
        if (binding.ibLcChatInput.isVisible == isVisibleMultiThreadSafe) {
            return
        } else if (isVisibleMultiThreadSafe) {
            binding.ibLcChatInput.show(true)
        } else {
            binding.ibLcChatInput.show(false)
        }
    }

    private fun sendInputDraftImmediately() {
        inputCheckerHandler.removeCallbacks(inputFinishChecker)
        inputCheckerHandler.post(inputFinishChecker)
    }

    //region Attachment picker
    private fun getCapturedMediaUrl(data: Intent?): Uri? {
        val uri = data?.data
        val mimeType =
            uri?.let { InAppChatMobileAttachment.getMimeType(requireActivity(), data, it) }
        return when {
            capturedImageUri != null && (mimeType == InAppChatAttachmentHelper.MIME_TYPE_IMAGE_JPEG || InAppChatAttachmentHelper.isUriFileEmpty(
                requireContext(),
                capturedImageUri
            ) == false) -> capturedImageUri
            capturedVideoUri != null && (mimeType == InAppChatAttachmentHelper.MIME_TYPE_VIDEO_MP_4 || InAppChatAttachmentHelper.isUriFileEmpty(
                requireContext(),
                capturedVideoUri
            ) == false) -> capturedVideoUri
            else -> null
        }
    }

    private val attachmentChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val listener =
                    object : InAppChatAttachmentHelper.InAppChatAttachmentHelperListener {
                        override fun onAttachmentCreated(attachment: InAppChatMobileAttachment?) {
                            if (attachment != null) {
                                MobileMessagingLogger.w(
                                    "InAppChatFragment",
                                    "Attachment created, will send Attachment"
                                )
                                binding.ibLcChat.sendChatMessage(null, attachment)
                            } else {
                                MobileMessagingLogger.e(
                                    "InAppChatFragment",
                                    "Can't create attachment"
                                )
                                Toast.makeText(
                                    requireContext(),
                                    localizationUtils.getString(R.string.ib_chat_cant_create_attachment),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            deleteEmptyMediaFiles()
                        }

                        override fun onError(
                            context: Context?,
                            exception: InternalSdkError.InternalSdkException?
                        ) {
                            if (exception!!.message == InternalSdkError.ERROR_ATTACHMENT_MAX_SIZE_EXCEEDED.get()) {
                                MobileMessagingLogger.e(
                                    "InAppChatFragment",
                                    "Maximum allowed attachment size exceeded" + widgetInfo?.getMaxUploadContentSize()
                                )
                                Toast.makeText(
                                    context,
                                    localizationUtils.getString(R.string.ib_chat_allowed_attachment_size_exceeded),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                MobileMessagingLogger.e(
                                    "InAppChatFragment",
                                    "Attachment content is not valid."
                                )
                                Toast.makeText(
                                    context,
                                    localizationUtils.getString(R.string.ib_chat_cant_create_attachment),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            deleteEmptyMediaFiles()
                        }

                    }
                val data = result.data

                InAppChatAttachmentHelper.makeAttachment(
                    requireActivity(),
                    data,
                    getCapturedMediaUrl(data),
                    listener
                )
            } else {
                deleteEmptyMediaFiles()
            }
        }

    private fun deleteEmptyMediaFiles() {
        InAppChatAttachmentHelper.deleteEmptyFileByUri(context, capturedImageUri)
        InAppChatAttachmentHelper.deleteEmptyFileByUri(context, capturedVideoUri)
    }

    private fun chooseFile() {
        if (!isRequiredPermissionsGranted()) {
            if (SystemInformation.isTiramisuOrAbove()) {
                MobileMessagingLogger.e(
                    "InAppChatFragment",
                    "Permissions required for attachments not granted " + ConfigurationException(
                        ConfigurationException.Reason.MISSING_REQUIRED_PERMISSION,
                        Manifest.permission.CAMERA + ", " + Manifest.permission.READ_MEDIA_IMAGES + ", " + Manifest.permission.READ_MEDIA_VIDEO + ", " + Manifest.permission.READ_MEDIA_AUDIO + ", " + Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ).message
                )
            } else {
                MobileMessagingLogger.e(
                    "InAppChatFragment",
                    "Permissions required for attachments not granted " + ConfigurationException(
                        ConfigurationException.Reason.MISSING_REQUIRED_PERMISSION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ).message
                )
            }
            return
        }
        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, prepareIntentForChooser())
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, prepareInitialIntentsForChooser())
        attachmentChooserLauncher.launch(chooserIntent)
    }

    private fun prepareIntentForChooser(): Intent {
        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.type = "*/*"
        return contentSelectionIntent
    }

    private fun prepareInitialIntentsForChooser(): Array<Intent> {
        val packageManager: PackageManager = requireActivity().packageManager
        val intentsForChooser: MutableList<Intent> = ArrayList()

        //picture
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        capturedImageUri = if (Build.VERSION.SDK_INT < 29) {
            InAppChatAttachmentHelper.getOutputImageUri(requireActivity())
        } else {
            InAppChatAttachmentHelper.getOutputImageUrlAPI29(requireActivity())
        }
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri)
        if (takePictureIntent.resolveActivity(packageManager) != null && capturedImageUri != null) {
            intentsForChooser.add(takePictureIntent)
        }

        //video
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (Build.VERSION.SDK_INT > 30) {
            capturedVideoUri = InAppChatAttachmentHelper.getOutputVideoUrl(requireActivity())
            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedVideoUri)
        }
        if (takeVideoIntent.resolveActivity(packageManager) != null) {
            intentsForChooser.add(takeVideoIntent)
        }

        return intentsForChooser.toTypedArray()
    }
    //endregion
    //endregion

    //region PermissionsRequester
    override fun requiredPermissions(): Array<String?> {
        return if (SystemInformation.isTiramisuOrAbove()) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override fun shouldShowPermissionsNotGrantedDialogIfShownOnce(): Boolean = true

    override fun permissionsNotGrantedDialogTitle(): Int =
        R.string.ib_chat_permissions_not_granted_title

    override fun permissionsNotGrantedDialogMessage(): Int =
        R.string.ib_chat_permissions_not_granted_message

    override fun onPermissionGranted() {
//        chooseFile()
    }

    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    fun isRequiredPermissionsGranted(): Boolean {
        val hasCameraFeature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        } else {
            requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        }
        return if (!hasCameraFeature || Camera.getNumberOfCameras() == 0)
            false
        else
            permissionsRequestManager.isRequiredPermissionsGranted
    }
    //endregion

    private fun initBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )
    }

    private fun removeBackPressHandler() {
        backPressedCallback.remove()
    }

}