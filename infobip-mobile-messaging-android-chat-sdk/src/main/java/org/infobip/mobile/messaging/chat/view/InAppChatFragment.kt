/*
 * InAppChatFragment.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.net.toUri
import androidx.core.text.layoutDirection
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.InAppChat
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachment
import org.infobip.mobile.messaging.chat.core.InAppChatException
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy
import org.infobip.mobile.messaging.chat.core.SessionStorage
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApi
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetLanguage
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetMessage
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetResult
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThread
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThreads
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView
import org.infobip.mobile.messaging.chat.databinding.IbFragmentChatBinding
import org.infobip.mobile.messaging.chat.models.AttachmentSource
import org.infobip.mobile.messaging.chat.models.AttachmentSourceSpecification
import org.infobip.mobile.messaging.chat.models.ContextualData
import org.infobip.mobile.messaging.chat.models.MessagePayload
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils
import org.infobip.mobile.messaging.chat.utils.copyFileToPublicDir
import org.infobip.mobile.messaging.chat.utils.deleteFile
import org.infobip.mobile.messaging.chat.utils.show
import org.infobip.mobile.messaging.chat.view.chooser.BottomSheetChooser
import org.infobip.mobile.messaging.chat.view.chooser.BottomSheetRow
import org.infobip.mobile.messaging.chat.view.styles.apply
import org.infobip.mobile.messaging.chat.view.styles.factory.StyleFactory
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

@OptIn(FlowPreview::class)
class InAppChatFragment : Fragment(), InAppChatFragmentActivityResultDelegate.ResultListener {

    /**
     * [InAppChatFragment] events listener propagates chat related events.
     *
     * It is intended to be used for more advanced integrations of [InAppChatFragment].
     * Together with another public properties and functions it offers you more control over chat.
     *
     * You can use [DefaultInAppChatFragmentEventsListener] to override only necessary methods.
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
        fun onChatAttachmentPreviewOpened(url: String?, type: String?, caption: String?): Boolean

        /**
         * Called by default InAppChat's Toolbar back navigation logic to exit chat. You are supposed to hide/remove [InAppChatFragment].
         */
        fun onExitChatPressed()
    }

    /**
     * [InAppChatFragment] errors handler allows you to define custom way to process chat errors.
     * You can use [DefaultInAppChatErrorsHandler] to override only necessary methods.
     */
    interface ErrorsHandler : InAppChatErrorsHandler

    companion object {
        private const val USER_INPUT_CHECKER_DELAY_MS = 250
        private const val TAG = "InAppChatFragment"
    }

    private var _binding: IbFragmentChatBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var localizationUtils: LocalizationUtils
    private var widgetInfo: WidgetInfo? = null
    private var widgetView: LivechatWidgetView? = null
    private val backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (handleBackPress)
                navigateBack()
        }
    }
    private val inputTextFlow = MutableStateFlow("")
    private var sendDraftJob: Job? = null
    private var lifecycleRegistry: InAppChatFragmentLifecycleRegistry? = null
    private lateinit var activityResultDelegate: InAppChatFragmentActivityResultDelegate

    /**
     * [InAppChatFragment] events listener allows you to listen to chat related events.
     */
    var eventsListener: EventsListener? = null

    val defaultErrorsHandler = object : ErrorsHandler {
        override fun handleError(exception: InAppChatException): Boolean {
            withBinding {
                return it.ibLcChat.defaultErrorsHandler.handleError(exception)
            }
            return false
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
     * to navigate from [LivechatWidgetView.THREAD] back to [LivechatWidgetView.THREAD_LIST] in multithread widget.
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
     * Use [InAppChatFragment.send] to send message.
     */
    var withInput = true
        set(value) {
            field = value
            //it can be called before fragment is attached/view is created
            if (_binding != null)
                initChatInput(value)
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

    /**
     * Timeout duration for loading chat in milliseconds.
     *
     * It has no effect if set to null or fragment is not attached yet.
     *
     * - **Minimum allowed:** 5,000 ms (5 seconds)
     * - **Maximum allowed:** 300,000 ms (5 minutes)
     * - **Default value:** 10,000 ms (10 seconds)
     *
     * @throws InAppChatException.LivechatWidgetInvalidLoadingTimeoutValue if the value is set outside the allowed range.
     */
    @set:Throws(InAppChatException.LivechatWidgetInvalidLoadingTimeoutValue::class)
    var loadingTimeoutMillis: Long?
        get() = _binding?.ibLcChat?.loadingTimeoutMillis
        set(value) {
            if (value != null)
                _binding?.ibLcChat?.loadingTimeoutMillis = value
        }

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityResultDelegate = InAppChatActivityResultDelegateImpl(requireActivity(), this)
        lifecycle.addObserver(activityResultDelegate)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
        val language = InAppChat.getInstance(requireContext()).language
        view.layoutDirection = language.locale.layoutDirection
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

    override fun onDestroyView() {
        super.onDestroyView()
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
     * Sends a message defined by [payload] data with optional [threadId].
     *
     * You can observe result by [InAppChatFragment.EventsListener.onChatSent] event.
     *
     * @param payload data defining message to be sent
     * @param threadId id of existing thread to send message into, if not provided, it will be sent to the active thread
     */
    @JvmOverloads
    fun send(payload: MessagePayload, threadId: String? = null) {
        withBinding { it.ibLcChat.send(payload, threadId) }
    }

    /**
     * Set contextual data of the livechat widget.
     *
     * If the function is called when [InAppChatFragment] is attached and the chat is loaded,
     * data will be sent immediately, otherwise they will be sent to the chat once it is loaded.
     *
     * Every function invocation will overwrite the previous contextual data.
     *
     * @param data contextual data in the form of JSON string
     * @param flag multithread strategy [MultithreadStrategy]
     * @see [InAppChatFragment.EventsListener.onChatLoaded] to detect if chat is loaded
     */
    fun sendContextualData(data: String, flag: MultithreadStrategy) {
        withBinding(
            action = { it.ibLcChat.sendContextualData(data, flag) },
            fallback = {
                SessionStorage.contextualData = ContextualData(data, flag)
                MobileMessagingLogger.d(TAG, "Contextual data is stored, will be sent once chat is loaded.")
            }
        )
    }

    /**
     * Creates a new thread with an initial message defined by the given [payload].
     *
     * You can observe the result via the [InAppChatFragment.EventsListener.onChatThreadCreated] event.
     *
     * @param payload The message payload used to start the new thread.
     */
    fun createThread(payload: MessagePayload) {
        withBinding { it.ibLcChat.createThread(payload) }
    }

    /**
     * Requests current threads from livechat widget.
     *
     * Does nothing if [InAppChatFragment] not attached.
     *
     * You can observe result by [InAppChatFragment.EventsListener.onChatThreadsReceived] event.
     */
    fun getThreads() {
        withBinding { it.ibLcChat.getThreads() }
    }

    /**
     * Requests shown thread - active from livechat widget.
     *
     * Does nothing if [InAppChatFragment] not attached.
     *
     * You can observe result by [InAppChatFragment.EventsListener.onChatActiveThreadReceived] event.
     */
    fun getActiveThread() {
        withBinding { it.ibLcChat.getActiveThread() }
    }

    /**
     * Navigates livechat widget to thread specified by provided [threadId].
     *
     * Does nothing if [InAppChatFragment] not attached.
     *
     * You can observe result by [InAppChatFragment.EventsListener.onChatThreadShown] event.
     *
     * @param threadId thread to be shown
     */
    fun showThread(threadId: String) {
        withBinding { it.ibLcChat.showThread(threadId) }
    }

    /**
     * Navigates livechat widget from [LivechatWidgetView.THREAD] back to [LivechatWidgetView.THREAD_LIST]
     * destination in multithread widget. Does nothing if livechat widget not multithread.
     *
     * Does nothing if [InAppChatFragment] not attached.
     */
    fun showThreadList() {
        withBinding { it.ibLcChat.showThreadList() }
    }

    /**
     * Executes back navigation. In multithread widget it handles internal navigation
     * from [LivechatWidgetView.THREAD] back to [LivechatWidgetView.THREAD_LIST] using
     * [InAppChatFragment.showThreadList], otherwise it triggers [InAppChatFragment.EventsListener.onExitChatPressed] event.
     *
     * It is default InAppChatFragment back navigation logic.
     */
    fun navigateBackOrCloseChat() {
        navigateBack()
    }

    /**
     * Set an in-app chat's language
     *
     * Does nothing if [InAppChatFragment] not attached.
     *
     * @param language language is used by livechat widget and InAppChat native parts
     */
    fun setLanguage(language: LivechatWidgetLanguage) {
        MobileMessagingLogger.d(TAG, "setLanguage($language)")
        withBinding { it.ibLcChat.setLanguage(language) }
    }

    /**
     * Returns current in-app chat language
     *
     * @return current in-app chat language or default [LivechatWidgetLanguage.ENGLISH]
     */
    fun getLanguage(): LivechatWidgetLanguage {
        return _binding?.ibLcChat?.getLanguage() ?: InAppChat.getInstance(requireContext()).language
    }

    /**
     * Sets a livechat widget's theme.
     * You can define widget themes in <a href="https://portal.infobip.com/apps/livechat/widgets">Live chat widget setup page</a> in Infobip Portal, section `Advanced customization`.
     * Please check widget <a href="https://www.infobip.com/docs/live-chat/widget-customization">documentation</a> for more details.
     *
     * Function allows to change widget theme while chat is shown - in runtime.
     * If you set widget theme before [InAppChatFragment] is shown the theme will be used once chat is loaded.
     *
     * Does nothing if [InAppChatFragment] not attached.
     *
     * @param widgetThemeName unique theme name, empty or blank value is ignored
     */
    fun setWidgetTheme(widgetThemeName: String) {
        withBinding { it.ibLcChat.setWidgetTheme(widgetThemeName) }
    }

    /**
     * Returns current livechat widget theme.
     *
     * @return applied theme name of livechat widget
     */
    fun getWidgetTheme(): String? {
        return _binding?.ibLcChat?.getWidgetTheme() ?: InAppChat.getInstance(requireContext()).widgetTheme
    }
    //endregion

    //region Internal
    /**
     * Prepares the widget to start a new conversation by setting its destination to [LivechatWidgetView.THREAD].
     *
     * Note: This does not create the actual thread until the initial message is sent by the user.
     * Internal method to be used by [InAppChat] only.
     * @param resultListener Optional listener to receive the result of the operation.
     */
    internal fun openNewThread(resultListener: ((LivechatWidgetResult<Unit>) -> Unit)? = null) {
        withBinding { it.ibLcChat.openNewThread(resultListener) }
    }
    //endregion

    //region Toolbar
    private fun initToolbar(withToolbar: Boolean) {
        withBinding { binding ->
            binding.ibLcAppbar.show(withToolbar)
            binding.ibLcAppbar.invalidate()
            if (withToolbar) {
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
                LivechatWidgetView.LOADING, LivechatWidgetView.THREAD_LIST, LivechatWidgetView.SINGLE_MODE_THREAD -> closeChatPage()
                LivechatWidgetView.THREAD, LivechatWidgetView.LOADING_THREAD, LivechatWidgetView.CLOSED_THREAD -> showThreadList()
            }
        } else {
            closeChatPage()
        }
    }

    private fun closeChatPage() {
        backPressedCallback.isEnabled = false //when InAppChat is used as Activity need to disable callback before onBackPressed() is called to avoid endless loop
        eventsListener?.onExitChatPressed()
    }

    private fun applyToolbarStyle(widgetInfo: WidgetInfo) {
        val style = StyleFactory.create(requireContext(), widgetInfo = widgetInfo).chatToolbarStyle()
        withBinding { style.apply(it.ibLcChatToolbar) }
    }
    //endregion

    //region Chat
    private fun initChat() {
        binding.ibLcChat.eventsListener = object : InAppChatView.EventsListener {

            override fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>) {
                binding.ibLcChatInput.isEnabled = result.isSuccess
                eventsListener?.onChatLoadingFinished(result)
            }

            override fun onChatConnectionPaused(result: LivechatWidgetResult<Unit>) {
                if (result.isSuccess) {
                    binding.ibLcChatInput.isEnabled = false
                }
                eventsListener?.onChatConnectionPaused(result)
            }

            override fun onChatConnectionResumed(result: LivechatWidgetResult<Unit>) {
                if (result.isSuccess) {
                    binding.ibLcChatInput.isEnabled = true
                }
                eventsListener?.onChatConnectionResumed(result)
            }

            override fun onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) {
                eventsListener?.onChatSent(result)
            }

            override fun onChatContextualDataSent(result: LivechatWidgetResult<String?>) {
                eventsListener?.onChatContextualDataSent(result)
            }

            override fun onChatThreadCreated(result: LivechatWidgetResult<LivechatWidgetMessage?>) {
                eventsListener?.onChatThreadCreated(result)
            }

            override fun onChatThreadsReceived(result: LivechatWidgetResult<LivechatWidgetThreads>) {
                eventsListener?.onChatThreadsReceived(result)
            }

            override fun onChatActiveThreadReceived(result: LivechatWidgetResult<LivechatWidgetThread?>) {
                eventsListener?.onChatActiveThreadReceived(result)
            }

            override fun onChatThreadShown(result: LivechatWidgetResult<LivechatWidgetThread>) {
                eventsListener?.onChatThreadShown(result)
            }

            override fun onChatThreadListShown(result: LivechatWidgetResult<Unit>) {
                eventsListener?.onChatThreadListShown(result)
            }

            override fun onChatLanguageChanged(result: LivechatWidgetResult<String?>) {
                eventsListener?.onChatLanguageChanged(result)
            }

            override fun onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) {
                eventsListener?.onChatWidgetThemeChanged(result)
            }

            override fun onChatControlsVisibilityChanged(isVisible: Boolean) {
                setChatInputVisibility(isVisible)
            }

            override fun onChatAttachmentPreviewOpened(url: String?, type: String?, caption: String?) {
                val handled = eventsListener?.onChatAttachmentPreviewOpened(url, type, caption) ?: false
                if (!handled) {
                    val intent: Intent
                    if (type == "DOCUMENT") {
                        intent = Intent(Intent.ACTION_VIEW).apply { data = url?.toUri() }
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

            override fun onChatViewChanged(widgetView: LivechatWidgetView) {
                this@InAppChatFragment.widgetView = widgetView
                updateInputVisibilityByMultiThreadView(widgetView)
                eventsListener?.onChatViewChanged(widgetView)
            }

            override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {
                this@InAppChatFragment.widgetInfo = widgetInfo
                updateViews(widgetInfo)
                MobileMessagingLogger.d(TAG, "WidgetInfo updated $widgetInfo")
                eventsListener?.onChatWidgetInfoUpdated(widgetInfo)
            }

            override fun onChatRawMessageReceived(rawMessage: String) {
                eventsListener?.onChatRawMessageReceived(rawMessage)
            }
        }
        binding.ibLcChat.errorsHandler = object : InAppChatView.ErrorsHandler {
            override fun handleError(exception: InAppChatException): Boolean {
                return errorsHandler.handleError(exception)
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
                updateAttachmentButton(widgetInfo)
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
                // nothing
            }

            override fun afterTextChanged(s: Editable) {
                inputTextFlow.value = s.toString()
            }
        })
        binding.ibLcChatInput.setInputFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (!hasFocus)
                binding.ibLcChatInput.hideKeyboard()
        }
        if (sendDraftJob?.isActive == true)
            sendDraftJob?.cancel()
        sendDraftJob = inputTextFlow
            .debounce(USER_INPUT_CHECKER_DELAY_MS.toLong())
            .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
            .flowOn(Dispatchers.Main)
            .filter { it.isNotEmpty() }
            .map { if (it.length > LivechatWidgetApi.MESSAGE_MAX_LENGTH) it.substring(0, LivechatWidgetApi.MESSAGE_MAX_LENGTH) else it }
            .onEach { draft -> withBinding { it.ibLcChat.send(MessagePayload.Draft(draft)) } }
            .catch { MobileMessagingLogger.e(TAG, "Failed to send draft message", it) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun initSendButton() = with(binding.ibLcChatInput) {
        setSendButtonClickListener {
            getInputText()?.let { msg ->
                if (msg.isNotBlank()) {
                    binding.ibLcChat.send(MessagePayload.Basic(msg))
                }
                clearInputText()
            }
        }
    }

    private fun updateAttachmentButton(widgetInfo: WidgetInfo?) {
        withBinding {
            val attachmentsEnabled = widgetInfo?.attachmentConfig?.isEnabled == true && widgetInfo.attachmentConfig.allowedExtensions.isNotEmpty()
            it.ibLcChatInput.setAttachmentButtonVisibility(if (attachmentsEnabled) View.VISIBLE else View.GONE)
            it.ibLcChatInput.setAttachmentButtonEnabled(attachmentsEnabled)
            if (attachmentsEnabled) {
                it.ibLcChatInput.setAttachmentButtonClickListener {
                    showAttachmentActionDialog(widgetInfo?.attachmentConfig?.allowedExtensions.orEmpty())
                }
            }
        }
    }

    private fun applyChatInputStyle(widgetInfo: WidgetInfo) {
        updateAttachmentButton(widgetInfo)
        withBinding {
            it.ibLcChatInput.applyWidgetInfoStyle(widgetInfo)
        }
    }

    private fun updateInputVisibilityByMultiThreadView(widgetView: LivechatWidgetView) {
        if (!withInput)
            return
        if (isMultiThread) {
            when (widgetView) {
                LivechatWidgetView.THREAD,
                LivechatWidgetView.SINGLE_MODE_THREAD,
                    -> setChatInputVisibility(true)

                LivechatWidgetView.LOADING,
                LivechatWidgetView.THREAD_LIST,
                LivechatWidgetView.CLOSED_THREAD,
                LivechatWidgetView.LOADING_THREAD,
                    -> setChatInputVisibility(false)
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
                isMultiThread && (widgetView == LivechatWidgetView.THREAD || widgetView == LivechatWidgetView.SINGLE_MODE_THREAD)
            val isNotMultiThread: Boolean = !isMultiThread
            val isVisibleMultiThreadSafe = isVisible && (canShowInMultiThread || isNotMultiThread)
            if (binding.ibLcChatInput.isVisible == isVisibleMultiThreadSafe) {
                return@withBinding
            } else {
                binding.ibLcChatInput.show(isVisibleMultiThreadSafe)
                eventsListener?.onChatControlsVisibilityChanged(isVisibleMultiThreadSafe)
            }
        }
    }

    //region Attachment picker
    private fun showAttachmentActionDialog(allowedExtensions: Set<String>) {
        if (allowedExtensions.isEmpty()) {
            MobileMessagingLogger.w(TAG, "Attachment picker skipped: no allowed attachment extensions are configured in the LiveChat Widget.")
            return
        }
        val specifications: Set<AttachmentSourceSpecification> = InAppChatAttachment.getAvailableSourcesSpecifications(requireContext(), allowedExtensions)
        if (specifications.isEmpty()) {
            MobileMessagingLogger.w(TAG, "Attachment picker skipped: no available attachment sources match the allowed extensions configured in the LiveChat Widget.")
            return
        }

        val bottomSheetRows = specifications.map { spec ->
            BottomSheetRow(
                text = localizationUtils.getString(spec.nameRes),
                identifier = spec.attachmentSource
            )
        }

        val chooser = BottomSheetChooser<AttachmentSource>(context).apply {
            setRows(bottomSheetRows)
        }

        chooser.setOnItemSelectedListener { attachmentSource, bottomSheetDialog ->
            bottomSheetDialog?.dismiss()
            getLifecycleRegistry().isEnabled = false
            when (attachmentSource) {
                AttachmentSource.Camera -> specifications.firstInstanceOrNull<AttachmentSourceSpecification.Camera>()?.let {
                    activityResultDelegate.capturePhoto(it.photoFileExtension)
                }

                AttachmentSource.VideoRecorder -> specifications.firstInstanceOrNull<AttachmentSourceSpecification.VideoRecorder>()?.let {
                    activityResultDelegate.recordVideo(it.videoFileExtension)
                }

                AttachmentSource.VisualMediaPicker -> specifications.firstInstanceOrNull<AttachmentSourceSpecification.VisualMediaPicker>()?.let {
                    activityResultDelegate.selectMedia(it.type)
                }

                AttachmentSource.FilePicker -> specifications.firstInstanceOrNull<AttachmentSourceSpecification.FilePicker>()?.let {
                    activityResultDelegate.selectFile(it.mimeTypes.toTypedArray())
                }
            }
        }
        chooser.createDialog().show()
    }

    private inline fun <reified T : AttachmentSourceSpecification> Set<AttachmentSourceSpecification>.firstInstanceOrNull(): T? {
        return firstOrNull { it is T } as? T
    }

    override fun onAttachmentLauncherResult(uri: Uri, source: AttachmentSource) {
        getLifecycleRegistry().isEnabled = true
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            runCatching {
                withContext(Dispatchers.Default) {
                    InAppChatAttachment.makeAttachment(context, uri, widgetInfo?.attachmentConfig?.maxSize?.toInt() ?: 0)
                }
            }.onFailure {
                MobileMessagingLogger.e(TAG, "Failed to create In-app chat attachment", it)
                val messageRes =
                    if ((it as? InAppChatException)?.technicalMessage?.contains("Attachment exceeds maximum allowed size") == true)
                        R.string.ib_chat_allowed_attachment_size_exceeded
                    else
                        R.string.ib_chat_cant_create_attachment
                Toast.makeText(
                    context,
                    localizationUtils.getString(messageRes),
                    Toast.LENGTH_SHORT
                ).show()
            }.onSuccess {
                binding.ibLcChat.send(MessagePayload.Basic(null, it))
                if (source == AttachmentSource.Camera || source == AttachmentSource.VideoRecorder) {
                    uri.copyFileToPublicDir(context)
                    uri.deleteFile(context)
                }
            }
        }
    }
    //endregion
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

    private inline fun withBinding(
        noinline fallback: (() -> Unit)? = null,
        action: (IbFragmentChatBinding) -> Unit,
    ) {
        _binding?.let(action) ?: fallback?.invoke() ?: MobileMessagingLogger.w(TAG, "Could not execute action, fragment is not attached yet")
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
