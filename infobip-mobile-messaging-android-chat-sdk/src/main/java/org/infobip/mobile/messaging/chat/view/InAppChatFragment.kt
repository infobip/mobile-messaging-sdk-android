package org.infobip.mobile.messaging.chat.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.hardware.camera2.CameraManager
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
import androidx.activity.result.ActivityResult
import androidx.annotation.ColorInt
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
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
import org.infobip.mobile.messaging.chat.view.styles.factory.StyleFactory
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.mobileapi.InternalSdkError
import java.util.*

class InAppChatFragment : Fragment(), InAppChatFragmentActivityResultDelegate.ResultListener {

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
    private var _lifecycleRegistry: InAppChatFragmentLifecycleRegistry? = null
    private val lifecycleRegistry
        get() = _lifecycleRegistry!!
    private lateinit var activityResultDelegate: InAppChatFragmentActivityResultDelegate

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

    /**
     * Allows to disconnect chat when fragment is hidden and receive push notifications.
     */
    var disconnectChatWhenHidden = false

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityResultDelegate = InAppChatActivityResultDelegateImpl(requireActivity().activityResultRegistry, this)
        lifecycle.addObserver(activityResultDelegate)
    }

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
        localizationUtils = LocalizationUtils.getInstance(requireContext())
        _lifecycleRegistry = InAppChatFragmentLifecycleRegistryImpl(
            viewLifecycleOwner,
            ignoreLifecycleOwnerEventsWhen = { this.isHidden })
        initViews()
        initBackPressHandler()
    }

    /**
     * Logic needed to support InAppChat.showInAppChatFragment()/InAppChat.hideInAppChatFragment()
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (disconnectChatWhenHidden) {
            lifecycleRegistry.setState(if (hidden) Lifecycle.State.CREATED else Lifecycle.State.RESUMED)
        }
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
        _lifecycleRegistry = null
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
        val style = StyleFactory.create(requireContext(), widgetInfo = widgetInfo).chatToolbarStyle()
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
        eventsListener = object : InAppChatView.EventsListener {
            override fun onChatLoaded(controlsEnabled: Boolean) {
                binding.ibLcChatInput.isEnabled = controlsEnabled
            }

            override fun onChatDisconnected() {
                binding.ibLcChatInput.isEnabled = false
            }

            override fun onChatReconnected() {
                binding.ibLcChatInput.isEnabled = true
            }

            override fun onChatControlsVisibilityChanged(isVisible: Boolean) {
                setChatInputVisibility(isVisible)
            }

            override fun onAttachmentPreviewOpened(url: String?, type: String?, caption: String?) {
                val intent: Intent
                if (type == "DOCUMENT") {
                    intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }
                } else {
                    intent = InAppChatAttachmentPreviewActivity.startIntent(
                        requireContext(),
                        url,
                        type,
                        caption
                    )
                }
                startActivity(intent)
            }

            override fun onChatViewChanged(widgetView: InAppChatWidgetView) {
                this@InAppChatFragment.widgetView = widgetView
                updateViewsVisibilityByMultiThreadView(widgetView)
            }

            override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {
                this@InAppChatFragment.widgetInfo = widgetInfo
                updateViews(widgetInfo)
            }
        }
        init(lifecycleRegistry.lifecycle)
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
                msg.chunkedSequence(InAppChatView.MESSAGE_MAX_LENGTH)
                    .forEach { message ->
                        if (message.isNotBlank()) {
                            binding.ibLcChat.sendChatMessage(message)
                        }
                    }
                clearInputText()
            }
        }
    }

    private fun initAttachmentButton() {
        binding.ibLcChatInput.setAttachmentButtonClickListener {
            val isCameraPermissionGranted = isCameraPermissionGranted()
            if (isCameraPermissionGranted) {
                chooseFile(isCameraPermissionGranted = true)
            } else {
                requestCameraPermissionIfNeeded()
            }
        }
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

    private val attachmentHelperListener =
        object : InAppChatAttachmentHelper.InAppChatAttachmentHelperListener {

            override fun onAttachmentCreated(attachment: InAppChatMobileAttachment?) {
                if (attachment != null) {
                    MobileMessagingLogger.w(
                        "InAppChatFragment",
                        "Attachment created, will send Attachment"
                    )
                    binding.ibLcChat.sendChatMessage(null, attachment)
                } else {
                    MobileMessagingLogger.e("InAppChatFragment", "Can't create attachment")
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
                    MobileMessagingLogger.e("InAppChatFragment", "Attachment content is not valid.")
                    Toast.makeText(
                        context,
                        localizationUtils.getString(R.string.ib_chat_cant_create_attachment),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                deleteEmptyMediaFiles()
            }

        }

    private fun deleteEmptyMediaFiles() {
        InAppChatAttachmentHelper.deleteEmptyFileByUri(context, capturedImageUri)
        InAppChatAttachmentHelper.deleteEmptyFileByUri(context, capturedVideoUri)
    }

    private fun chooseFile(isCameraPermissionGranted: Boolean) {
        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, prepareIntentForChooser())
        if (isCameraPermissionGranted) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, prepareInitialIntentsForChooser())
        }
        lifecycleRegistry.isEnabled = false
        activityResultDelegate.openAttachmentChooser(chooserIntent)
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

    override fun onAttachmentChooserResult(result: ActivityResult) {
        lifecycleRegistry.isEnabled = true
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            InAppChatAttachmentHelper.makeAttachment(
                requireActivity(),
                data,
                getCapturedMediaUrl(data),
                attachmentHelperListener
            )
        } else {
            deleteEmptyMediaFiles()
        }
    }
    //endregion
    //endregion

    //region Permissions
    @SuppressLint("UnsupportedChromeOsCameraSystemFeature")
    fun requestCameraPermissionIfNeeded() {
        val hasCameraFeature = requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (hasCameraFeature && cameraManager.cameraIdList.isNotEmpty()) {
            val permission = Manifest.permission.CAMERA
            when {
                isCameraPermissionGranted() -> {
                    //all good no need to request
                }
                shouldShowRequestPermissionRationale(permission) -> showCameraPermissionRationale()
                else -> activityResultDelegate.requestCameraPermission()
            }
        }
    }

    override fun onCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            chooseFile(isCameraPermissionGranted = true)
        } else {
            showCameraPermissionRationale()
        }
    }

    private fun isCameraPermissionGranted(): Boolean = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PERMISSION_GRANTED

    private fun showCameraPermissionRationale() {
        AlertDialog.Builder(requireContext(), R.style.IB_Chat_AlertDialog)
            .setTitle(R.string.ib_chat_permissions_not_granted_title)
            .setMessage(R.string.ib_chat_permissions_not_granted_message)
            .setCancelable(false)
            .setNegativeButton(R.string.mm_button_cancel) { dialog, _ ->
                dialog.dismiss()
                chooseFile(false)
            }
            .setPositiveButton(R.string.mm_button_settings) { dialog, _ ->
                dialog.dismiss()
                activityResultDelegate.openAppSettings(context.packageName)
            }.show()
    }

    override fun onSettingsResult(result: ActivityResult) {
        MobileMessagingLogger.d("Settings intent result ${result.resultCode}")
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