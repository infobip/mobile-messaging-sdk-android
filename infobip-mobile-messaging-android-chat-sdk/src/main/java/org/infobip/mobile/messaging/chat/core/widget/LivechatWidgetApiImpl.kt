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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.infobip.mobile.messaging.MobileMessagingCore
import org.infobip.mobile.messaging.chat.InAppChat
import org.infobip.mobile.messaging.chat.core.InAppChatException
import org.infobip.mobile.messaging.chat.core.JwtProvider
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApi.ExecutionListener
import org.infobip.mobile.messaging.chat.models.MessagePayload
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import kotlin.coroutines.resume

internal class LivechatWidgetApiImpl(
    instanceId: InstanceId,
    private val webView: LivechatWidgetWebView,
    private val mmCore: MobileMessagingCore,
    private val inAppChat: InAppChat,
    private val propertyHelper: PropertyHelper,
    private val coroutineScope: CoroutineScope,
) : LivechatWidgetApi, LivechatWidgetWebViewManager {

    companion object {
        private const val DEFAULT_LOADING_TIMEOUT_MS = 60 * 1000L
        private const val MIN_LOADING_TIMEOUT_MS = 5 * 1000L
        private const val MAX_LOADING_TIMEOUT_MS = 5 * 60 * 1000L
        private const val LOADING_CHECK_INTERVAL_MS = 100L
    }

    init {
        webView.instanceId = instanceId
        webView.setup(this, coroutineScope)
    }

    /**
     *  Widget loading coroutine continuation. It is used to control - resume/cancel widget loading coroutine.
     *  @Volatile ensures visibility across threads. All writes must happen on [webViewDispatcher] (Main thread).
     */
    @Volatile
    private var widgetLoadingContinuation: CancellableContinuation<Boolean>? = null
    private val isWidgetLoadingInProgress: Boolean
        get() = widgetLoadingContinuation?.isActive == true

    /**
     * Listener for the result of [openNewThread] method. [LivechatWidgetEventsListener] is not used as [openNewThread] is a special internal only function.
     */
    private var openNewThreadResultListener: ((LivechatWidgetResult<Unit>) -> Unit)? = null

    private val loadingMutex = Mutex()

    /**
     * All [webView] interactions must be executed on the same thread.
     */
    private val webViewDispatcher = Dispatchers.Main

    //region LivechatWidgetApi
    /**
     * @Volatile ensures visibility across threads. All writes must happen on [webViewDispatcher] (Main thread).
     */
    @Volatile
    override var isWidgetLoaded: Boolean = false
        private set

    override var eventsListener: LivechatWidgetEventsListener? = null

    override var jwtProvider: JwtProvider? by inAppChat::widgetJwtProvider

    override var domain: String? by inAppChat::domain

    @set:Throws(InAppChatException.LivechatWidgetInvalidLoadingTimeoutValue::class)
    override var loadingTimeoutMillis: Long = DEFAULT_LOADING_TIMEOUT_MS
        set(value) {
            if (value in MIN_LOADING_TIMEOUT_MS..MAX_LOADING_TIMEOUT_MS) {
                field = value
            } else {
                throw InAppChatException.LivechatWidgetInvalidLoadingTimeoutValue()
            }
        }

    override fun loadWidget(
        widgetId: String?,
        jwt: String?,
        domain: String?,
        theme: String?,
        language: LivechatWidgetLanguage?,
    ) {
        coroutineScope.launch(webViewDispatcher) {
            runCatching {
                loadWidgetInternal(
                    providedWidgetId = widgetId,
                    providedJwt = jwt,
                    providedDomain = domain,
                    providedTheme = theme,
                    providedLanguage = language,
                )
            }.onFailure { throwable ->
                onWidgetLoadingFinished(LivechatWidgetResult.Error(throwable.mapToLivechatException()))
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
     * Sets widget loading continuation. All writes are dispatched to [webViewDispatcher] (Main thread).
     * Since Main dispatcher is single-threaded, this ensures thread-safe writes.
     */
    private fun setContinuation(continuation: CancellableContinuation<Boolean>?) {
        coroutineScope.launch(webViewDispatcher) {
            widgetLoadingContinuation = continuation
        }
    }

    /**
     * Updates [isWidgetLoaded] flag. All writes are dispatched to [webViewDispatcher] (Main thread).
     * Since Main dispatcher is single-threaded, this ensures thread-safe writes.
     */
    private fun updateWidgetLoaded(loaded: Boolean) {
        coroutineScope.launch(webViewDispatcher) {
            isWidgetLoaded = loaded
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
        providedWidgetId: String? = null,
        providedJwt: String? = null,
        providedDomain: String? = null,
        providedTheme: String? = null,
        providedLanguage: LivechatWidgetLanguage? = null,
    ) {
        loadingMutex.withLock {
            val pushRegId: String? = mmCore.pushRegistrationId?.takeIf { it.isNotBlank() }
            val widgetId: String? = providedWidgetId?.takeIf { it.isNotBlank() } ?: propertyHelper.findString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID)

            when {
                pushRegId.isNullOrBlank() -> throw InAppChatException.MissingPushRegistrationId()
                widgetId.isNullOrBlank() -> throw InAppChatException.MissingLivechatWidgetId()
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
                        val jwt: String? = providedJwt?.takeIf { it.isNotBlank() } ?: requestNewJwt()
                        val domain: String? = providedDomain?.takeIf { it.isNotBlank() } ?: inAppChat.domain
                        val theme: String? = providedTheme?.takeIf { it.isNotBlank() } ?: inAppChat.widgetTheme
                        val language: String = (providedLanguage ?: inAppChat.language).widgetCode
                        runCatching { //must stay in place to propagate possible suspendCancellableCoroutine() exception immediately and do not wait for timeout
                            val result: Boolean = suspendCancellableCoroutine { continuation ->
                                setContinuation(continuation)
                                webView.loadWidgetPage(
                                    pushRegistrationId = pushRegId,
                                    widgetId = widgetId,
                                    jwt = jwt,
                                    domain = domain,
                                    widgetTheme = theme,
                                    language = language
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
     * Requests new JWT from [inAppChat.widgetJwtProvider].
     * If provider is set, it will call [JwtProvider.provideJwt] and wait for the result.
     * Returns null if provider is not set.
     */
    private suspend fun requestNewJwt(): String? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            inAppChat.widgetJwtProvider?.provideJwt(
                object : JwtProvider.JwtCallback {
                    override fun onJwtReady(jwt: String) {
                        continuation.resume(jwt)
                    }

                    override fun onJwtError(error: Throwable) {
                        continuation.cancel(InAppChatException.JwtProviderError(error))
                    }
                }
            ) ?: continuation.resume(null)
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
            is LivechatWidgetResult.Error -> MobileMessagingLogger.e(webView.instanceId.tag(LivechatWidgetApi.TAG), "Widget loading error:", result.throwable)
            is LivechatWidgetResult.Success<Boolean> -> MobileMessagingLogger.d(webView.instanceId.tag(LivechatWidgetApi.TAG), if (result.payload) "Widget successfully loaded." else "Widget has been reset and is no longer loaded.")
        }
        propagateEvent { onLoadingFinished(result) }
    }

    /**
     * Executes Widget API call if widget is loaded. If widget is not loaded, loads it first.
     */
    private fun executeApiCall(
        method: LivechatWidgetMethod,
        apiCall: LivechatWidgetClient.(ExecutionListener<String>) -> Unit,
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
                ) ?: throw InAppChatException.LivechatWidgetWebViewNotInitialized()
            }.onFailure { throwable ->
                handleWidgetApiError(method, LivechatWidgetResult.Error(throwable.mapToLivechatException()))
            }
        }
    }

    private fun Throwable.mapToLivechatException(): InAppChatException {
        return when (this) {
            is LivechatWidgetException -> this
            is TimeoutCancellationException -> InAppChatException.LivechatWidgetLoadingTimeout()
            else -> InAppChatException.LivechatWidgetApiError(this)
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
        val exception = LivechatWidgetException.parse(errorPayload, method)
        handleWidgetApiError(method, LivechatWidgetResult.Error(exception))
    }

    private fun handleWidgetApiError(method: LivechatWidgetMethod, result: LivechatWidgetResult.Error) {
        when (method) {
            /**
             * widgetLoadingContinuation.cancel(Exception) will resume suspendCancellableCoroutine() what will propagate public event
             */
            LivechatWidgetMethod.config,
            LivechatWidgetMethod.identify,
            LivechatWidgetMethod.initWidget,
            LivechatWidgetMethod.show -> resumeWidgetLoadingContinuation(result)

            LivechatWidgetMethod.setTheme -> propagateEvent { onThemeChanged(result) }
            LivechatWidgetMethod.resumeConnection -> propagateEvent { onConnectionResumed(result) }
            LivechatWidgetMethod.pauseConnection -> propagateEvent { onConnectionPaused(result) }
            LivechatWidgetMethod.sendContextualData -> propagateEvent { onContextualDataSent(result) }
            LivechatWidgetMethod.setLanguage -> propagateEvent { onLanguageChanged(result) }
            LivechatWidgetMethod.showThreadList -> propagateEvent { onThreadListShown(result) }
            LivechatWidgetMethod.sendMessage -> propagateEvent { onSent(result) }

            LivechatWidgetMethod.getThreads -> propagateEvent { onThreadsReceived(result) }
            LivechatWidgetMethod.showThread -> propagateEvent { onThreadShown(result) }
            LivechatWidgetMethod.getActiveThread -> propagateEvent { onActiveThreadReceived(result) }
            LivechatWidgetMethod.createThread -> propagateEvent { onThreadCreated(result) }
            LivechatWidgetMethod.openNewThread -> {
                openNewThreadResultListener?.invoke(result)
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
            LivechatWidgetMethod.show, -> {} //nothing, handled by onWidgetViewChanged()

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