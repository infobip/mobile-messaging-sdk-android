package org.infobip.mobile.messaging.chat.core.widget

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.infobip.mobile.messaging.MobileMessagingCore
import org.infobip.mobile.messaging.chat.InAppChat
import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment
import org.infobip.mobile.messaging.chat.core.JwtProvider
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApi.ExecutionListener
import org.infobip.mobile.messaging.chat.models.MessagePayload
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import kotlin.coroutines.resume
import kotlin.properties.Delegates

internal class LivechatWidgetApiImpl(
    instanceId: InstanceId,
    private val webView: LivechatWidgetWebView,
    private val mmCore: MobileMessagingCore,
    private val inAppChat: InAppChat,
    private val propertyHelper: PropertyHelper,
    private val coroutineScope: CoroutineScope
) : LivechatWidgetApi, LivechatWidgetWebViewManager {

    companion object {
        private const val DEFAULT_LOADING_TIMEOUT_MS = 10_000L
        private const val MIN_LOADING_TIMEOUT_MS = 5 * 1000
        private const val MAX_LOADING_TIMEOUT_MS = 5 * 60 * 1000
        private const val LOADING_CHECK_INTERVAL_MS = 100L

        private const val LOADING_FAIL_MSG = "Widget loading failed. Reason:"
        private const val LOADING_RESET_MSG = "Widget has been reset and is no longer loaded."
        private const val LOADING_SUCCESS_MSG = "Widget successfully loaded."
        private const val UNKNOWN_ERROR = "Unknown error."
    }

    init {
        webView.instanceId = instanceId
        webView.setup(this, coroutineScope)
    }

    /**
     *  Widget loading coroutine continuation. It is used to control - resume/cancel widget loading coroutine.
     */
    private var widgetLoadingContinuation: CancellableContinuation<Boolean>? = null
    private val isWidgetLoadingInProgress: Boolean
        get() = widgetLoadingContinuation?.isActive == true

    /**
     * Listener for the result of [openNewThread] method. [LivechatWidgetEventsListener] is not used as [openNewThread] is a special internal only function.
     */
    private var openNewThreadResultListener: ((LivechatWidgetResult<Unit>) -> Unit)? = null

    private val loadingMutex = Mutex()
    private val continuationMutex = Mutex()
    private val loadedFlagMutex = Mutex()

    /**
     * All [webView] interactions must be executed on the same thread.
     */
    private val webViewDispatcher = Dispatchers.Main

    //region LivechatWidgetApi
    override var isWidgetLoaded: Boolean = false
        private set

    override var eventsListener: LivechatWidgetEventsListener? = null

    override var jwtProvider: JwtProvider? by inAppChat::widgetJwtProvider

    override var domain: String? by inAppChat::domain

    @set:Throws(IllegalArgumentException::class)
    override var loadingTimeoutMillis: Long by Delegates.vetoable(DEFAULT_LOADING_TIMEOUT_MS) { _, _, newValue ->
        if (newValue in MIN_LOADING_TIMEOUT_MS..MAX_LOADING_TIMEOUT_MS) {
            true
        } else {
            throw IllegalArgumentException("Loading timeout must be between $MIN_LOADING_TIMEOUT_MS and $MAX_LOADING_TIMEOUT_MS milliseconds.")
        }
    }

    override fun loadWidget(
        widgetId: String?,
        jwt: String?,
        domain: String?,
        theme: String?,
        language: LivechatWidgetLanguage?
    ) {
        coroutineScope.launch(webViewDispatcher) {
            runCatching {
                loadWidgetInternal(
                    widgetId = widgetId,
                    jwt = jwt,
                    domain = domain,
                    theme = theme,
                    language = language,
                )
            }.onFailure {
                onWidgetLoadingFinished(LivechatWidgetResult.Error(it.mapToLivechatException()))
            }
        }
    }

    override fun pauseConnection() {
        executeApiCall(LivechatWidgetMethod.pauseConnection) { listener ->
            pauseConnection(listener)
        }
    }

    override fun resumeConnection() {
        executeApiCall(LivechatWidgetMethod.resumeConnection) { listener ->
            resumeConnection(listener)
        }
    }

    @Deprecated(
        message = "Use send(message: MessagePayload) with MessagePayload.Basic() instead",
        replaceWith = ReplaceWith("send(MessagePayload.Basic(message, attachment))"),
    )
    override fun sendMessage(message: String?, attachment: InAppChatMobileAttachment?) {
        if (message?.isNotBlank() == true) {
            send(MessagePayload.Basic(message, attachment))
        } else if (attachment != null) {
            send(MessagePayload.Basic(null, attachment))
        } else {
            propagateEvent { onMessageSent(LivechatWidgetResult.Error("Could not send message. Both message and attachment are null or empty.")) }
        }
    }

    @Deprecated(
        message = "Use send(message: MessagePayload) with MessagePayload.Draft() instead",
        replaceWith = ReplaceWith("send(MessagePayload.Draft(draft))")
    )
    override fun sendDraft(draft: String) {
        if (draft.isNotBlank()) {
            send(MessagePayload.Draft(draft))
        } else {
            propagateEvent { onDraftSent(LivechatWidgetResult.Error("Could not send draft. Draft is null or empty.")) }
        }
    }

    override fun send(payload: MessagePayload, threadId: String?) {
        executeApiCall(LivechatWidgetMethod.sendMessage) { listener ->
            send(payload, threadId, listener)
        }
    }

    override fun createThread(payload: MessagePayload) {
        executeApiCall(LivechatWidgetMethod.createThread) { listener ->
            createThread(payload, listener)
        }
    }

    override fun sendContextualData(data: String, multiThreadFlag: MultithreadStrategy) {
        executeApiCall(LivechatWidgetMethod.sendContextualData) { listener ->
            sendContextualData(data, multiThreadFlag, listener)
        }
    }

    override fun getThreads() {
        executeApiCall(LivechatWidgetMethod.getThreads) { listener ->
            getThreads(listener)
        }
    }

    override fun getActiveThread() {
        executeApiCall(LivechatWidgetMethod.getActiveThread) { listener ->
            getActiveThread(listener)
        }
    }

    override fun showThread(threadId: String) {
        executeApiCall(LivechatWidgetMethod.showThread) { listener ->
            showThread(threadId, listener)
        }
    }

    override fun showThreadList() {
        executeApiCall(LivechatWidgetMethod.showThreadList) { listener ->
            showThreadList(listener)
        }
    }

    override fun setLanguage(language: LivechatWidgetLanguage) {
        executeApiCall(LivechatWidgetMethod.setLanguage) { listener ->
            setLanguage(language, listener)
        }
    }

    override fun setTheme(themeName: String) {
        executeApiCall(LivechatWidgetMethod.setTheme) { listener ->
            setTheme(themeName, listener)
        }
    }

    override fun reset() {
        coroutineScope.launch(webViewDispatcher) {
            val result = LivechatWidgetResult.Success(false)
            val handled = resumeWidgetLoadingContinuation(result)
            if (!handled) {
                onWidgetLoadingFinished(result)
            }
            webView.run {
                clearHistory()
                clearCache(true)
                MobileMessagingLogger.d(webView.instanceId.tag(LivechatWidgetApi.TAG), "Livechat widget history deleted.")
                loadUrl("about:blank")
            }
        }
    }

    /**
     * Prepares the widget to start a new conversation by setting its destination to [LivechatWidgetView.THREAD].
     *
     * Note: This does not create the actual thread until the initial message is sent by the user.
     * Internal method to be used by [InAppChat] only.
     * @param resultListener Optional listener to receive the result of the operation.
     */
    internal fun openNewThread(resultListener: ((LivechatWidgetResult<Unit>) -> Unit)? = null) {
        openNewThreadResultListener = resultListener
        executeApiCall(LivechatWidgetMethod.openNewThread) { listener ->
            openNewThread(listener)
        }
    }
    //endregion

    //region internal helpers
    /**
     * Sets widget loading continuation in safe manner - only one coroutine can access/set in a time.
     */
    private fun setContinuation(continuation: CancellableContinuation<Boolean>?) {
        coroutineScope.launch {
            continuationMutex.withLock {
                widgetLoadingContinuation = continuation
            }
        }
    }

    /**
     * Sets [isWidgetLoaded] flag in safe manner - only one coroutine can access/set in a time.
     */
    private fun updateWidgetLoaded(loaded: Boolean) {
        coroutineScope.launch {
            loadedFlagMutex.withLock {
                isWidgetLoaded = loaded
            }
        }
    }

    /**
     * Loads widget if not loaded yet. Widget loading timeout is [loadingTimeoutMillis] seconds.
     * If widget is already loaded, it does nothing.
     * It waits for widget loading to finish if it's in progress.
     * It validates input parameters and fallbacks to [InAppChat] default configuration values.
     * Throws exception if any of mandatory parameters is null or blank.
     */
    @Throws(Exception::class)
    private suspend fun loadWidgetInternal(
        pushRegistrationId: String? = null,
        widgetId: String? = null,
        jwt: String? = null,
        domain: String? = null,
        theme: String? = null,
        language: LivechatWidgetLanguage? = null
    ) {
        loadingMutex.withLock {
            val fallbackPushRegistrationId: String? = pushRegistrationId?.takeIf { it.isNotBlank() } ?: mmCore.pushRegistrationId?.takeIf { it.isNotBlank() }
            val fallbackWidgetId: String? = widgetId?.takeIf { it.isNotBlank() } ?: propertyHelper.findString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID)
            val fallbackJwt: String? = jwt?.takeIf { it.isNotBlank() } ?: inAppChat.widgetJwtProvider?.provideJwt()
            val fallbackDomain: String? = domain?.takeIf { it.isNotBlank() } ?: inAppChat.domain
            val fallbackTheme: String? = theme?.takeIf { it.isNotBlank() } ?: inAppChat.widgetTheme
            val fallbackLanguage: String = (language ?: inAppChat.language).widgetCode

            when {
                fallbackPushRegistrationId.isNullOrBlank() -> throw LivechatWidgetException.fromAndroid("$LOADING_FAIL_MSG Push registration id is null or blank.")
                fallbackWidgetId.isNullOrBlank() -> throw LivechatWidgetException.fromAndroid("$LOADING_FAIL_MSG Widget id is null or blank.")
                isWidgetLoadingInProgress -> {
                    MobileMessagingLogger.d(webView.instanceId.tag(LivechatWidgetApi.TAG), "Another widget loading is in progress.")
                    withTimeout(loadingTimeoutMillis) {
                        while (isActive && isWidgetLoadingInProgress) {
                            delay(LOADING_CHECK_INTERVAL_MS)
                        }
                    }
                }

                !isWidgetLoaded -> {
                    withTimeout(loadingTimeoutMillis) {
                        runCatching { //must stay in place to propagate possible suspendCancellableCoroutine() exception immediately and do not wait for timeout
                            val result: Boolean = suspendCancellableCoroutine { continuation ->
                                setContinuation(continuation)
                                webView.loadWidgetPage(
                                    pushRegistrationId = fallbackPushRegistrationId,
                                    widgetId = fallbackWidgetId,
                                    jwt = fallbackJwt,
                                    domain = fallbackDomain,
                                    widgetTheme = fallbackTheme,
                                    language = fallbackLanguage
                                )
                                continuation.invokeOnCancellation {
                                    setContinuation(null)
                                }
                            }
                            onWidgetLoadingFinished(LivechatWidgetResult.Success(result))
                        }.getOrThrow()
                    }
                }
            }
        }
    }

    /**
     * Resumes widget loading continuation with [result] if loading is in progress.
     * Returns true if widget loading was resumed, false otherwise.
     */
    private fun resumeWidgetLoadingContinuation(result: LivechatWidgetResult<Boolean>): Boolean {
        widgetLoadingContinuation?.takeIf { it.isActive }?.let { continuation ->
            when (result) {
                is LivechatWidgetResult.Success<Boolean> -> continuation.resume(result.payload)
                is LivechatWidgetResult.Error -> continuation.cancel(result.throwable)
            }
            setContinuation(null)
            return true
        }
        return false
    }

    /**
     * Propagates widget loading result to public [eventsListener] and updates [isWidgetLoaded] flag.
     */
    private fun onWidgetLoadingFinished(result: LivechatWidgetResult<Boolean>) {
        updateWidgetLoaded(result.getOrNull() == true)
        when (result) {
            is LivechatWidgetResult.Error -> MobileMessagingLogger.e(webView.instanceId.tag(LivechatWidgetApi.TAG), "${result.throwable.message}", result.throwable)
            is LivechatWidgetResult.Success<Boolean> -> MobileMessagingLogger.d(webView.instanceId.tag(LivechatWidgetApi.TAG), if (result.payload) LOADING_SUCCESS_MSG else LOADING_RESET_MSG)
        }
        propagateEvent { onLoadingFinished(result) }
    }

    /**
     * Executes Widget API call if widget is loaded. If widget is not loaded, loads it first.
     */
    private fun executeApiCall(
        method: LivechatWidgetMethod,
        apiCall: LivechatWidgetClient.(ExecutionListener<String>) -> Unit
    ) {
        coroutineScope.launch(webViewDispatcher) {
            runCatching {
                runCatching {
                    loadWidgetInternal()
                }.onFailure {
                    updateWidgetLoaded(false)
                }.getOrThrow()
                webView.livechatWidgetClient?.apiCall(
                    ExecutionListener { executionResult: LivechatWidgetResult<String> ->
                        if (executionResult is LivechatWidgetResult.Error) {
                            handleWidgetApiError(method, executionResult)
                        }
                    }
                ) ?: throw LivechatWidgetException.fromAndroid("LivechatWidgetWebView is not initialized. Call LivechatWidgetWebView.setup() first.")
            }.onFailure {
                handleWidgetApiError(method, LivechatWidgetResult.Error(it.mapToLivechatException()))
            }
        }
    }

    private fun Throwable.mapToLivechatException(): LivechatWidgetException {
        return when (this) {
            is LivechatWidgetException -> this
            is TimeoutCancellationException -> LivechatWidgetException.fromAndroid("$LOADING_FAIL_MSG Widget loading timeout.")
            else -> LivechatWidgetException.fromAndroid("${this.javaClass.simpleName}: $message")
        }
    }

    //endregion

    //region InAppChatWebViewManager
    override fun onPageStarted(url: String?) {
        propagateEvent { onPageStarted(url) }
    }

    override fun onPageFinished(url: String?) {
        propagateEvent { onPageFinished(url) }
    }

    override fun setControlsVisibility(isVisible: Boolean) {
        propagateEvent { onControlsVisibilityChanged(isVisible) }
    }

    override fun openAttachmentPreview(url: String?, type: String?, caption: String?) {
        propagateEvent { onAttachmentPreviewOpened(url, type, caption) }
    }

    override fun onWidgetViewChanged(widgetView: LivechatWidgetView) {
        if (!isWidgetLoaded && widgetView != LivechatWidgetView.LOADING) {
            coroutineScope.launch {
                resumeWidgetLoadingContinuation(LivechatWidgetResult.Success(true))
            }
        }
        propagateEvent { onWidgetViewChanged(widgetView) }
    }

    override fun onWidgetRawMessageReceived(message: String?) {
        propagateEvent { onRawMessageReceived(message) }
    }

    override fun onWidgetApiError(method: LivechatWidgetMethod, errorPayload: String?) {
        val error = LivechatWidgetResult.Error(LivechatWidgetException.parse(errorPayload ?: UNKNOWN_ERROR, method))
        handleWidgetApiError(method, error)
    }

    private fun handleWidgetApiError(method: LivechatWidgetMethod, error: LivechatWidgetResult.Error) {
        when (method) {
            /**
             * widgetLoadingContinuation.cancel(Exception) will resume suspendCancellableCoroutine() what will propagate public event
             */
            LivechatWidgetMethod.config,
            LivechatWidgetMethod.identify,
            LivechatWidgetMethod.initWidget,
            LivechatWidgetMethod.show -> resumeWidgetLoadingContinuation(error)

            LivechatWidgetMethod.setTheme -> propagateEvent { onThemeChanged(error) }
            LivechatWidgetMethod.resumeConnection -> propagateEvent { onConnectionResumed(error) }
            LivechatWidgetMethod.pauseConnection -> propagateEvent { onConnectionPaused(error) }
            LivechatWidgetMethod.sendContextualData -> propagateEvent { onContextualDataSent(error) }
            LivechatWidgetMethod.setLanguage -> propagateEvent { onLanguageChanged(error) }
            LivechatWidgetMethod.showThreadList -> propagateEvent { onThreadListShown(error) }
            LivechatWidgetMethod.sendMessage -> propagateEvent { onSent(error) }

            LivechatWidgetMethod.getThreads -> propagateEvent { onThreadsReceived(error) }
            LivechatWidgetMethod.showThread -> propagateEvent { onThreadShown(error) }
            LivechatWidgetMethod.getActiveThread -> propagateEvent { onActiveThreadReceived(error) }
            LivechatWidgetMethod.createThread -> propagateEvent { onThreadCreated(error) }
            LivechatWidgetMethod.openNewThread -> {
                openNewThreadResultListener?.invoke(error)
                openNewThreadResultListener = null
            }
        }
    }

    override fun onWidgetApiSuccess(method: LivechatWidgetMethod, successPayload: String?) {
        val payload = successPayload?.takeIf { it.isNotBlank() }
        when (method) {
            LivechatWidgetMethod.config,
            LivechatWidgetMethod.identify,
            LivechatWidgetMethod.initWidget,
            LivechatWidgetMethod.show -> {
                //nothing, handled by onWidgetViewChanged()
            }

            LivechatWidgetMethod.setTheme -> propagateEvent { onThemeChanged(LivechatWidgetResult.Success(payload)) }
            LivechatWidgetMethod.resumeConnection -> propagateEvent { onConnectionResumed(LivechatWidgetResult.Success.unit) }
            LivechatWidgetMethod.pauseConnection -> propagateEvent { onConnectionPaused(LivechatWidgetResult.Success.unit) }
            LivechatWidgetMethod.sendContextualData -> propagateEvent { onContextualDataSent(LivechatWidgetResult.Success(payload)) }
            LivechatWidgetMethod.setLanguage -> propagateEvent { onLanguageChanged(LivechatWidgetResult.Success(payload)) }
            LivechatWidgetMethod.showThreadList -> propagateEvent { onThreadListShown(LivechatWidgetResult.Success.unit) }

            LivechatWidgetMethod.getThreads -> propagateEvent { onThreadsReceived(LivechatWidgetResult.Success(LivechatWidgetThreads.parse(payload))) }
            LivechatWidgetMethod.showThread -> propagateEvent { onThreadShown(LivechatWidgetResult.Success(LivechatWidgetThread.parse(payload))) }
            LivechatWidgetMethod.getActiveThread -> propagateEvent { onActiveThreadReceived(LivechatWidgetResult.Success(LivechatWidgetThread.parseOrNull(payload))) }
            LivechatWidgetMethod.sendMessage -> propagateEvent { onSent(LivechatWidgetResult.Success(LivechatWidgetMessage.parseOrNull(payload))) }
            LivechatWidgetMethod.createThread -> propagateEvent { onThreadCreated(LivechatWidgetResult.Success(LivechatWidgetMessage.parseOrNull(payload))) }
            LivechatWidgetMethod.openNewThread -> {
                openNewThreadResultListener?.invoke(LivechatWidgetResult.Success.unit)
                openNewThreadResultListener = null
            }
        }
    }

    /**
     * Propagates event to [eventsListener] in a safe manner on UI thread.
     */
    private fun propagateEvent(action: LivechatWidgetEventsListener.() -> Unit) {
        eventsListener?.let { listener ->
            coroutineScope.launch(Dispatchers.Main) {
                listener.action()
            }
        }
    }

    //endregion

}