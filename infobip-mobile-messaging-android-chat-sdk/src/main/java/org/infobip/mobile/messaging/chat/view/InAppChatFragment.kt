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
import org.infobip.mobile.messaging.chat.core.SessionStorage
import org.infobip.mobile.messaging.chat.databinding.IbFragmentChatBinding
import org.infobip.mobile.messaging.chat.models.ContextualData
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils
import org.infobip.mobile.messaging.chat.utils.getStatusBarColor
import org.infobip.mobile.messaging.chat.utils.isLightStatusBarMode
import org.infobip.mobile.messaging.chat.utils.setLightStatusBarMode
import org.infobip.mobile.messaging.chat.utils.setStatusBarColor
import org.infobip.mobile.messaging.chat.utils.show
import org.infobip.mobile.messaging.chat.view.styles.InAppChatToolbarStyle
import org.infobip.mobile.messaging.chat.view.styles.apply
import org.infobip.mobile.messaging.chat.view.styles.factory.StyleFactory
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.mobileapi.InternalSdkError
import java.util.Locale

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

    /**
     * [InAppChatFragment] events listener propagates chat related events.
     *
     * It is intended to be used for more advanced integrations of [InAppChatFragment].
     * Together with another public properties and functions it offers you more control over chat.
     */
    interface EventsListener : InAppChatEventsListener {

        /**
         * Called when attachment from chat has been interacted.
         *
         * It allows you to handle attachment preview on your own. Return true if you handled attachment preview.
         * Return false to let [InAppChatFragment] handle attachment preview.
         *
         * @param url attachment url
         * @param type attachment type
         * @param caption attachment caption
         * @return true if attachment preview has been handled, false otherwise
         */
        fun onAttachmentPreviewOpened(url: String?, type: String?, caption: String?): Boolean

        /**
         * Called by default InAppChat's Toolbar back navigation logic to exit chat. You are supposed to hide/remove [InAppChatFragment].
         *
         * If you showed [InAppChatFragment] using [InAppChat.showInAppChatFragment] use [InAppChat.hideInAppChatFragment] to hide InAppChatFragment.
         * If you added [InAppChatFragment] using custom logic, it is up to you to get rid of [InAppChatFragment].
         */
        fun onExitChatPressed()
    }

    /**
     * [InAppChatFragment] errors handler allows you to define custom way to process chat errors.
     */
    interface ErrorsHandler : InAppChatErrorHandler

    companion object {
        private const val USER_INPUT_CHECKER_DELAY_MS = 250
        private const val TAG = "InAppChatFragment"
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
    private var appliedWidgetTheme: String? = null
    private val backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (handleBackPress)
                navigateBack()
        }
    }
    private val inputFinishChecker: InAppChatInputFinishChecker =
        InAppChatInputFinishChecker { draft ->
            withBinding { it.ibLcChat.sendChatMessageDraft(draft) }
        }
    private val inputCheckerHandler = Handler(Looper.getMainLooper())
    private var lifecycleRegistry: InAppChatFragmentLifecycleRegistry? = null
    private lateinit var activityResultDelegate: InAppChatFragmentActivityResultDelegate

    /**
     * [InAppChatFragment] events listener allows you to listen to chat related events.
     */
    var eventsListener: EventsListener? = null

    val defaultErrorsHandler = object : ErrorsHandler {
        override fun handlerError(error: String) {
            withBinding {
                it.ibLcChat.defaultErrorsHandler.handlerError(error)
            }
        }

        override fun handlerWidgetError(error: String) {
            withBinding {
                it.ibLcChat.defaultErrorsHandler.handlerWidgetError(error)
            }
        }

        override fun handlerNoInternetConnectionError(hasConnection: Boolean) {
            withBinding {
                it.ibLcChat.defaultErrorsHandler.handlerNoInternetConnectionError(hasConnection)
            }
        }
    }

    /**
     * Allows you to set custom [InAppChatFragment.ErrorsHandler] handler to process chat errors on your own.
     */
    var errorsHandler: ErrorsHandler = defaultErrorsHandler

    /**
     * Returns true if chat is loaded and multithread feature is enabled, otherwise returns false.
     */
    val isMultiThread
        get() = _binding?.ibLcChat?.isMultiThread ?: false

    /**
     * Allows you to control presence of InAppChatFragment's Toolbar.
     * If you want to use your own Toolbar, set it to false. Default value is true.
     *
     * When you use own Toolbar it is up to you to handle back navigation logic.
     * You can use [InAppChatFragment.navigateBackOrCloseChat] for default back navigation logic,
     * what handles internal multithread widget navigation together with android native navigation.
     * In case you want to handle back navigation on your own, there is [InAppChatFragment.showThreadList]
     * to navigate from [InAppChatWidgetView.THREAD] back to [InAppChatWidgetView.THREAD_LIST] in multithread widget.
     */
    var withToolbar = true
        set(value) {
            field = value
            //it can be called before fragment is attached/view is created
            if (_binding != null)
                initToolbar(value)
        }

    /**
     * Allows you to control presence of InAppChatFragment's message input.
     * If you want to use your own message input, set it to false. Default value is true.
     *
     * When you use own message input it is up to you to handle message and attachment sending logic
     * including request Android permissions for attachment picker.
     * You can reuse provided [InAppChatInputView] or create custom UI.
     * Use [InAppChatFragment.sendChatMessage] to send message.
     * Use [InAppChatFragment.sendChatMessageDraft] to send draft message.
     */
    var withInput = true
        set(value) {
            field = value
            //it can be called before fragment is attached/view is created
            if (_binding != null)
                initChatInput(value)
        }

    /**
     * Allows another way how to inject InAppChatActionBarProvider to InAppChatFragment.
     * It is used in React Native plugin to handle multithread navigation.
     */
    var inAppChatActionBarProvider: InAppChatActionBarProvider? = null
        get() {
            return field ?: (requireActivity() as? InAppChatActionBarProvider)
        }

    /**
     * Allows to stop chat connection when fragment is hidden.
     * Chat is reconnected automatically once fragment is shown again.
     * Default value is false.
     *
     * By chat connection you can control push notifications.
     * Push notifications are active only when chat connection is not active.
     *
     * It calls [InAppChatView.stopConnection] when fragment is hidden and [InAppChatView.restartConnection] once fragment is visible again.
     */
    var disconnectChatWhenHidden = false

    /**
     * Allows to enable/disable back press handling logic.
     * If true, it triggers default back navigation logic [navigateBackOrCloseChat], useful especially for multithread widgets.
     * If false, back press events are not handled.
     * It does not affect ActionBar back button.
     *
     * Default value is true.
     */
    var handleBackPress = true

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
        getLifecycleRegistry()
        initViews()
        initBackPressHandler()
    }

    /**
     * Logic needed to support InAppChat.showInAppChatFragment()/InAppChat.hideInAppChatFragment()
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (disconnectChatWhenHidden) {
            getLifecycleRegistry().setState(if (hidden) Lifecycle.State.CREATED else Lifecycle.State.RESUMED)
        }
        if (!hidden) {
            initToolbar(withToolbar)
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
        lifecycleRegistry = null
    }

    private fun initViews() {
        binding.ibLcChatInput.isEnabled = false
        initChat()
        initToolbar(withToolbar)
        initChatInput(withInput)
    }

    /**
     * Every time [WidgetInfo] is updated, views are updated to apply widget colors.
     */
    private fun updateViews(widgetInfo: WidgetInfo) {
        applyToolbarStyle(widgetInfo)
        applyChatInputStyle(widgetInfo)
        if (!isMultiThread) {
            setChatInputVisibility(true)
        }
    }
    //endregion

    //region Public
    /**
     * Set the language of the Livechat Widget.
     *
     * It does nothing if [InAppChatFragment] is not attached.
     *
     * @param locale locale's language is used by Livechat Widget and native parts
     */
    fun setLanguage(locale: Locale) {
        MobileMessagingLogger.d(TAG, "setLanguage($locale)")
        withBinding { it.ibLcChat.setLanguage(locale) }
    }


    /**
     * Set contextual data of the Livechat Widget.
     *
     * If the function is called when [InAppChatFragment] is attached and the chat is loaded,
     * data will be sent immediately, otherwise they will be sent to the chat once it is loaded.
     *
     * Every function invocation will overwrite the previous contextual data.
     *
     *
     * @param data                   contextual data in the form of JSON string
     * @param allMultiThreadStrategy multithread strategy flag, true -> ALL, false -> ACTIVE
     * @see [InAppChatFragment.EventsListener.onChatLoaded] to detect if chat is loaded
     */
    fun sendContextualData(data: String, allMultiThreadStrategy: Boolean) {
        withBinding(
            action = { it.ibLcChat.sendContextualData(data, allMultiThreadStrategy) },
            fallback = {
                SessionStorage.contextualData = ContextualData(data, allMultiThreadStrategy)
                MobileMessagingLogger.d(TAG, "Contextual data is stored, will be sent once chat is loaded.")
            }
        )
    }

    /**
     * Navigates Livechat widget from [InAppChatWidgetView.THREAD] back to [InAppChatWidgetView.THREAD_LIST]
     * destination in multithread widget. It does nothing if widget is not multithread.
     *
     * It does nothing if [InAppChatFragment] is not attached.
     */
    fun showThreadList() {
        withBinding { it.ibLcChat.showThreadList() }
    }

    /**
     * Sends draft message to be show in chat to peer's chat.
     *
     * It does nothing if [InAppChatFragment] is not attached.
     *
     * @param draft message
     */
    fun sendChatMessageDraft(draft: String) {
        withBinding { it.ibLcChat.sendChatMessageDraft(draft) }
    }

    /**
     * Sends message to the chat with optional [InAppChatMobileAttachment].
     *
     * It does nothing if [InAppChatFragment] is not attached.
     *
     * @param message message to be send, max length allowed is 4096 characters
     * @param attachment to create attachment use [InAppChatMobileAttachment]'s constructor where you provide attachment's mimeType, base64 and filename
     */
    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun sendChatMessage(message: String?, attachment: InAppChatMobileAttachment? = null) {
        withBinding { it.ibLcChat.sendChatMessage(message, attachment) }
    }

    /**
     * Set the theme of the Livechat Widget.
     * You can define widget themes in <a href="https://portal.infobip.com/apps/livechat/widgets">Live chat widget setup page</a> in Infobip Portal, section `Advanced customization`.
     * Please check widget <a href="https://www.infobip.com/docs/live-chat/widget-customization">documentation</a> for more details.
     *
     * Function allows to change widget theme while chat is shown - in runtime.
     * If you set widget theme before [InAppChatFragment] is shown the theme will be used once chat is loaded.
     *
     * It does nothing if [InAppChatFragment] is not attached.
     *
     * @param widgetThemeName unique theme name, empty or blank value is ignored
     */
    fun setWidgetTheme(widgetThemeName: String) {
        withBinding { it.ibLcChat.setWidgetTheme(widgetThemeName) }
    }

    /**
     * Executes back navigation. In multithread widget it handles internal navigation
     * from [InAppChatWidgetView.THREAD] back to [InAppChatWidgetView.THREAD_LIST] using
     * [InAppChatFragment.showThreadList], otherwise it triggers [InAppChatFragment.EventsListener.onExitChatPressed] event
     * and [InAppChatFragment.InAppChatActionBarProvider.onInAppChatBackPressed].
     *
     * It is default InAppChatFragment back navigation logic.
     */
    fun navigateBackOrCloseChat() {
        navigateBack()
    }
    //endregion

    //region Toolbar
    private fun initToolbar(withToolbar: Boolean) {
        withBinding { binding ->
            binding.ibLcAppbar.show(withToolbar)
            binding.ibLcAppbar.invalidate()
            if (withToolbar) {
                if (inAppChatActionBarProvider?.originalSupportActionBar?.isShowing == true)
                    inAppChatActionBarProvider?.originalSupportActionBar?.hide()
                binding.ibLcChatToolbar.setNavigationOnClickListener { navigateBack() }
            } else {
                binding.ibLcChatToolbar.setNavigationOnClickListener(null)
            }
        }
    }

    private fun navigateBack() {
        withBinding { it.ibLcChatInput.hideKeyboard() }
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
        if (withToolbar)
            inAppChatActionBarProvider?.originalSupportActionBar?.show()
        backPressedCallback.isEnabled = false //when InAppChat is used as Activity need to disable callback before onBackPressed() is called to avoid endless loop
        inAppChatActionBarProvider?.onInAppChatBackPressed()
        eventsListener?.onExitChatPressed()
    }

    private fun applyToolbarStyle(widgetInfo: WidgetInfo) {
        val style =
            StyleFactory.create(requireContext(), widgetInfo = widgetInfo).chatToolbarStyle()
        withBinding { style.apply(it.ibLcChatToolbar) }
        applyStatusBarStyle(style)
    }

    private fun applyStatusBarStyle(style: InAppChatToolbarStyle) {
        if (!withToolbar)
            return
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
        if (!withToolbar)
            return
        if (originalStatusBarColor != 0)
            requireActivity().setStatusBarColor(originalStatusBarColor)
        originalLightStatusBar?.let {
            requireActivity().setLightStatusBarMode(it)
        }
    }
    //endregion

    //region Chat
    private fun initChat() {
        binding.ibLcChat.eventsListener = object : InAppChatView.EventsListener {
            override fun onChatLoaded(controlsEnabled: Boolean) {
                binding.ibLcChatInput.isEnabled = controlsEnabled
                eventsListener?.onChatLoaded(controlsEnabled)
            }

            override fun onChatDisconnected() {
                binding.ibLcChatInput.isEnabled = false
                eventsListener?.onChatDisconnected()
            }

            override fun onChatReconnected() {
                binding.ibLcChatInput.isEnabled = true
                eventsListener?.onChatReconnected()
            }

            override fun onChatControlsVisibilityChanged(isVisible: Boolean) {
                setChatInputVisibility(isVisible)
                eventsListener?.onChatControlsVisibilityChanged(isVisible)
            }

            override fun onAttachmentPreviewOpened(url: String?, type: String?, caption: String?) {
                val handled = eventsListener?.onAttachmentPreviewOpened(url, type, caption) ?: false
                if (!handled) {
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
                    runCatching {
                        startActivity(intent)
                    }.onFailure {
                        MobileMessagingLogger.e(TAG, "Could not open attachment preview.", it)
                    }
                }
            }

            override fun onChatViewChanged(widgetView: InAppChatWidgetView) {
                this@InAppChatFragment.widgetView = widgetView
                updateInputVisibilityByMultiThreadView(widgetView)
                eventsListener?.onChatViewChanged(widgetView)
            }

            override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {
                this@InAppChatFragment.widgetInfo = widgetInfo
                updateViews(widgetInfo)
                MobileMessagingLogger.w(TAG, "WidgetInfo updated $widgetInfo")
                eventsListener?.onChatWidgetInfoUpdated(widgetInfo)
            }

            override fun onChatWidgetThemeChanged(widgetThemeName: String) {
                appliedWidgetTheme = widgetThemeName
                eventsListener?.onChatWidgetThemeChanged(widgetThemeName)
            }
        }
        binding.ibLcChat.errorsHandler = object : InAppChatView.ErrorsHandler {
            override fun handlerError(error: String) {
                errorsHandler.handlerError(error)
            }

            override fun handlerWidgetError(error: String) {
                errorsHandler.handlerWidgetError(error)
            }

            override fun handlerNoInternetConnectionError(hasConnection: Boolean) {
                errorsHandler.handlerNoInternetConnectionError(hasConnection)
            }

        }
        binding.ibLcChat.init(getLifecycleRegistry().lifecycle)
    }
    //endregion

    //region ChatInput
    private fun initChatInput(withInput: Boolean) {
        withBinding {
            if (withInput) {
                initTextBar()
                initSendButton()
                initAttachmentButton()
                setChatInputVisibility(true)
            } else {
                it.ibLcChatInput.visibility = View.GONE
            }
        }
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
        withBinding { it.ibLcChatInput.applyWidgetInfoStyle(widgetInfo) }
    }

    private fun updateInputVisibilityByMultiThreadView(widgetView: InAppChatWidgetView) {
        if (!withInput)
            return
        if (isMultiThread) {
            when (widgetView) {
                InAppChatWidgetView.THREAD,
                InAppChatWidgetView.SINGLE_MODE_THREAD -> setChatInputVisibility(true)
                InAppChatWidgetView.LOADING,
                InAppChatWidgetView.THREAD_LIST,
                InAppChatWidgetView.CLOSED_THREAD,
                InAppChatWidgetView.LOADING_THREAD -> setChatInputVisibility(false)
            }
        } else {
            setChatInputVisibility(true)
        }
    }

    private fun setChatInputVisibility(isVisible: Boolean) {
        withBinding { binding ->
            if (!withInput)
                return@withBinding
            val canShowInMultiThread =
                isMultiThread && (widgetView == InAppChatWidgetView.THREAD || widgetView == InAppChatWidgetView.SINGLE_MODE_THREAD)
            val isNotMultiThread: Boolean = !isMultiThread
            val isVisibleMultiThreadSafe = isVisible && (canShowInMultiThread || isNotMultiThread)
            if (binding.ibLcChatInput.isVisible == isVisibleMultiThreadSafe) {
                return@withBinding
            } else {
                binding.ibLcChatInput.show(isVisibleMultiThreadSafe)
            }
        }
    }

    private fun sendInputDraftImmediately() {
        if (!withInput)
            return
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
                    MobileMessagingLogger.w(TAG, "Attachment created, will send Attachment")
                    binding.ibLcChat.sendChatMessage(null, attachment)
                } else {
                    MobileMessagingLogger.e(TAG, "Can't create attachment")
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
                        TAG,
                        "Maximum allowed attachment size exceeded" + widgetInfo?.getMaxUploadContentSize()
                    )
                    Toast.makeText(
                        context,
                        localizationUtils.getString(R.string.ib_chat_allowed_attachment_size_exceeded),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    MobileMessagingLogger.e(TAG, "Attachment content is not valid.")
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
        getLifecycleRegistry().isEnabled = false
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
        getLifecycleRegistry().isEnabled = true
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
        MobileMessagingLogger.d(TAG, "Settings intent result ${result.resultCode}")
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

    private fun withBinding(
        fallback: (() -> Unit)? = null,
        action: (IbFragmentChatBinding) -> Unit,
    ) {
        _binding?.let(action) ?: fallback?.invoke() ?: MobileMessagingLogger.e(TAG, "Could not execute action, fragment is not attached yet")
    }

    private fun getLifecycleRegistry(): InAppChatFragmentLifecycleRegistry {
        return lifecycleRegistry ?: InAppChatFragmentLifecycleRegistryImpl(
            viewLifecycleOwner,
            ignoreLifecycleOwnerEventsWhen = { this.isHidden }
        ).also {
            lifecycleRegistry = it
        }
    }

}