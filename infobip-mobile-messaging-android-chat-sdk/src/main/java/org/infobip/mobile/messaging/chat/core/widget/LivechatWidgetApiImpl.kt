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
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import kotlin.coroutines.resume

internal class LivechatWidgetApiImpl(
    private val webView: LivechatWidgetWebView,
    private val mmCore: MobileMessagingCore,
    private val inAppChat: InAppChat,
    private val propertyHelper: PropertyHelper,
    private val coroutineScope: CoroutineScope,
) : LivechatWidgetApi, LivechatWidgetWebViewManager {

    companion object {
        private const val LOADING_TIMEOUT_MS = 10_000L
        private const val LOADING_CHECK_INTERVAL_MS = 100L

        private const val LOADING_FAIL_MSG = "Widget loading failed. Reason:"
        private const val LOADING_SUCCESS_MSG = "Widget successfully loaded."
        private const val API_CALL_FAIL_MSG = "Livechat Widget API call failed. Reason:"
        private const val UNKNOWN_ERROR = "Unknown error."
    }

    init {
        webView.setup(this, coroutineScope)
    }

    /**
     *  Widget loading coroutine continuation. It is used to control - resume/cancel widget loading coroutine.
     */
    private var widgetLoadingContinuation: CancellableContinuation<Unit>? = null
    private val isWidgetLoadingInProgress: Boolean
        get() = widgetLoadingContinuation?.isActive == true

    private val loadingMutex = Mutex()
    private val continuationMutex = Mutex()
    private val loadedFlagMutex = Mutex()

    //region LivechatWidgetApi
    override var isWidgetLoaded: Boolean = false
        private set

    override var eventsListener: LivechatWidgetEventsListener? = null

    override var jwtProvider: JwtProvider?
        get() = inAppChat.widgetJwtProvider
        set(value) {
            inAppChat.widgetJwtProvider = value
        }

    override var domain: String?
        get() = inAppChat.domain
        set(value) {
            inAppChat.domain = value
        }

    override fun loadWidget(
        widgetId: String?,
        jwt: String?,
        domain: String?,
        theme: String?
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                loadWidgetInternal(
                    widgetId = widgetId,
                    jwt = jwt,
                    domain = domain,
                    theme = theme,
                )
            }.onFailure {
                onWidgetLoadingFinished(LivechatWidgetResult.Error(it.getMappedMessage()))
            }
        }
    }

    override fun pauseConnection(listener: ExecutionListener<String>?) {
        executeApiCall(LivechatWidgetMethod.pauseConnection) {
            pauseConnection(listener)
        }
    }

    override fun resumeConnection(listener: ExecutionListener<String>?) {
        executeApiCall(LivechatWidgetMethod.resumeConnection) {
            resumeConnection(listener)
        }
    }

    override fun sendMessage(message: String?, attachment: InAppChatMobileAttachment?, listener: ExecutionListener<String>?) {
        executeApiCall(LivechatWidgetMethod.sendMessage) {
            sendMessage(message, attachment, listener)
        }
    }

    override fun sendDraft(draft: String, listener: ExecutionListener<String>?) {
        executeApiCall(LivechatWidgetMethod.sendDraft) {
            sendDraft(draft, listener)
        }
    }

    override fun sendContextualData(data: String, multiThreadFlag: MultithreadStrategy, listener: ExecutionListener<String>?) {
        executeApiCall(LivechatWidgetMethod.sendContextualData) {
            sendContextualData(data, multiThreadFlag, listener)
        }
    }

    override fun showThreadList(listener: ExecutionListener<String>?) {
        executeApiCall(LivechatWidgetMethod.showThreadList) {
            showThreadList(listener)
        }
    }

    override fun setLanguage(language: LivechatWidgetLanguage, listener: ExecutionListener<String>?) {
        executeApiCall(LivechatWidgetMethod.setLanguage) {
            setLanguage(language, listener)
        }
    }

    override fun setTheme(themeName: String, listener: ExecutionListener<String>?) {
        executeApiCall(LivechatWidgetMethod.setTheme) {
            setTheme(themeName, listener)
        }
    }

    override fun reset() {
        coroutineScope.launch(Dispatchers.Main) {
            val result = LivechatWidgetResult.Error("Widget loading cancelled due to cleanup.")
            resumeWidgetLoadingContinuation(result)
            onWidgetLoadingFinished(result)
            webView.run {
                clearHistory()
                clearCache(true)
                MobileMessagingLogger.d(LivechatWidgetApi.TAG, "Livechat widget history deleted.")
                loadUrl("about:blank")
            }
        }
    }
    //endregion

    //region internal helpers
    /**
     * Sets widget loading continuation in safe manner - only one coroutine can access/set in a time.
     */
    private fun setContinuation(continuation: CancellableContinuation<Unit>?) {
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
     * Loads widget if not loaded yet. Widget loading timeout is 10 seconds.
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
    ) {
        loadingMutex.withLock {
            val fallbackPushRegistrationId: String? = pushRegistrationId?.takeIf { it.isNotBlank() } ?: mmCore.pushRegistrationId?.takeIf { it.isNotBlank() }
            val fallbackWidgetId: String? = widgetId?.takeIf { it.isNotBlank() } ?: propertyHelper.findString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID)
            val fallbackJwt: String? = jwt?.takeIf { it.isNotBlank() } ?: inAppChat.widgetJwtProvider?.provideJwt()
            val fallbackDomain: String? = domain?.takeIf { it.isNotBlank() } ?: inAppChat.domain
            val fallbackTheme: String? = theme?.takeIf { it.isNotBlank() } ?: inAppChat.widgetTheme

            when {
                fallbackPushRegistrationId.isNullOrBlank() -> throw IllegalStateException("Push registration id is null or blank.")
                fallbackWidgetId.isNullOrBlank() -> throw IllegalStateException("Widget id is null or blank.")
                isWidgetLoadingInProgress -> {
                    MobileMessagingLogger.d(LivechatWidgetApi.TAG, "Another widget loading is in progress.")
                    withTimeout(LOADING_TIMEOUT_MS) {
                        while (isActive && isWidgetLoadingInProgress) {
                            delay(LOADING_CHECK_INTERVAL_MS)
                        }
                    }
                }

                !isWidgetLoaded -> {
                    withTimeout(LOADING_TIMEOUT_MS) {
                        runCatching { //must stay in place to propagate possible suspendCancellableCoroutine() exception immediately and do not wait for timeout
                            suspendCancellableCoroutine<Unit> { continuation ->
                                setContinuation(continuation)
                                webView.loadWidgetPage(
                                    pushRegistrationId = fallbackPushRegistrationId,
                                    widgetId = fallbackWidgetId,
                                    jwt = fallbackJwt,
                                    domain = fallbackDomain,
                                    widgetTheme = fallbackTheme
                                )
                                continuation.invokeOnCancellation {
                                    setContinuation(null)
                                }
                            }
                            onWidgetLoadingFinished(LivechatWidgetResult.Success.unit)
                        }.getOrThrow()
                    }
                }
            }
        }
    }

    /**
     * Resumes widget loading continuation with [result].
     */
    private fun resumeWidgetLoadingContinuation(result: LivechatWidgetResult<Unit>) {
        when (result) {
            is LivechatWidgetResult.Success<*> -> {
                widgetLoadingContinuation?.takeIf { it.isActive }?.resume(Unit)
            }

            is LivechatWidgetResult.Error -> {
                widgetLoadingContinuation?.takeIf { it.isActive }?.cancel(Exception(result.throwable.message))
            }
        }
        setContinuation(null)
    }

    /**
     * Propagates widget loading result to public [eventsListener] and updates [isWidgetLoaded] flag.
     */
    private fun onWidgetLoadingFinished(result: LivechatWidgetResult<Unit>, withReport: Boolean = true) {
        updateWidgetLoaded(result is LivechatWidgetResult.Success<*>)
        if (withReport) {
            when (result) {
                is LivechatWidgetResult.Error -> MobileMessagingLogger.e(LivechatWidgetApi.TAG, "$LOADING_FAIL_MSG ${result.throwable.message}")
                is LivechatWidgetResult.Success<*> -> MobileMessagingLogger.d(LivechatWidgetApi.TAG, LOADING_SUCCESS_MSG)
            }
            eventsListener?.onLoadingFinished(result.addErrorMessagePrefix(LOADING_FAIL_MSG))
        }
    }

    /**
     * Executes Widget API call if widget is loaded. If widget is not loaded, loads it first.
     */
    private fun executeApiCall(
        method: LivechatWidgetMethod,
        apiCall: LivechatWidgetClient.() -> Unit
    ) {
        coroutineScope.launch(Dispatchers.Main) {
            runCatching {
                runCatching {
                    loadWidgetInternal()
                }.onFailure {
                    onWidgetLoadingFinished(LivechatWidgetResult.Error(it.getMappedMessage()), withReport = false)
                }.getOrThrow()
                webView.livechatWidgetClient?.apiCall() ?: throw IllegalStateException("LivechatWidgetWebView is not initialized. Call LivechatWidgetWebView.setup() first.")
            }.onFailure {
                onWidgetApiError(method, it.getMappedMessage())
            }
        }
    }

    private fun Throwable.getMappedMessage(): String = if (this is TimeoutCancellationException) "Widget loading timeout." else this.message ?: UNKNOWN_ERROR

    //endregion

    //region InAppChatWebViewManager
    override fun onPageStarted(url: String?) {
        eventsListener?.onPageStarted(url)
    }

    override fun onPageFinished(url: String?) {
        eventsListener?.onPageFinished(url)
    }

    override fun setControlsVisibility(isVisible: Boolean) {
        eventsListener?.onControlsVisibilityChanged(isVisible)
    }

    override fun openAttachmentPreview(url: String?, type: String?, caption: String?) {
        eventsListener?.onAttachmentPreviewOpened(url, type, caption)
    }

    override fun onWidgetViewChanged(widgetView: LivechatWidgetView) {
        if (!isWidgetLoaded && widgetView != LivechatWidgetView.LOADING) {
            coroutineScope.launch {
                delay(300) //we must wait a bit to let JS widget fully load, already reported to LC team
                resumeWidgetLoadingContinuation(LivechatWidgetResult.Success.unit)
            }
        }
        eventsListener?.onWidgetViewChanged(widgetView)
    }

    override fun onWidgetRawMessageReceived(message: String?) {
        eventsListener?.onRawMessageReceived(message)
    }

    override fun onWidgetApiError(method: LivechatWidgetMethod, errorPayload: String?) {
        val payload = errorPayload?.takeIf { it.isNotBlank() } ?: UNKNOWN_ERROR
        val message = "$method() ${"=>".takeIf { payload.isNotBlank() }} $payload"
        val error = LivechatWidgetResult.Error("$API_CALL_FAIL_MSG $message")
        when (method) {
            /**
             * widgetLoadingContinuation.cancel(Exception) will resume suspendCancellableCoroutine() what will propagate public event
             */
            LivechatWidgetMethod.config,
            LivechatWidgetMethod.identify,
            LivechatWidgetMethod.initWidget,
            LivechatWidgetMethod.show -> resumeWidgetLoadingContinuation(LivechatWidgetResult.Error(message))

            LivechatWidgetMethod.setTheme -> eventsListener?.onThemeChanged(error)
            LivechatWidgetMethod.resumeConnection -> eventsListener?.onConnectionResumed(error)
            LivechatWidgetMethod.pauseConnection -> eventsListener?.onConnectionPaused(error)
            LivechatWidgetMethod.sendContextualData -> eventsListener?.onContextualDataSent(error)
            LivechatWidgetMethod.setLanguage -> eventsListener?.onLanguageChanged(error)
            LivechatWidgetMethod.showThreadList -> eventsListener?.onThreadListShown(error)
            LivechatWidgetMethod.sendDraft -> eventsListener?.onDraftSent(error)
            LivechatWidgetMethod.sendMessageWithAttachment,
            LivechatWidgetMethod.sendMessage -> eventsListener?.onMessageSent(error)
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

            LivechatWidgetMethod.setTheme -> eventsListener?.onThemeChanged(LivechatWidgetResult.Success(payload))
            LivechatWidgetMethod.resumeConnection -> eventsListener?.onConnectionResumed(LivechatWidgetResult.Success.unit)
            LivechatWidgetMethod.pauseConnection -> eventsListener?.onConnectionPaused(LivechatWidgetResult.Success.unit)
            LivechatWidgetMethod.sendContextualData -> eventsListener?.onContextualDataSent(LivechatWidgetResult.Success(payload))
            LivechatWidgetMethod.setLanguage -> eventsListener?.onLanguageChanged(LivechatWidgetResult.Success(payload))
            LivechatWidgetMethod.showThreadList -> eventsListener?.onThreadListShown(LivechatWidgetResult.Success.unit)
            LivechatWidgetMethod.sendDraft -> eventsListener?.onDraftSent(LivechatWidgetResult.Success(payload))
            LivechatWidgetMethod.sendMessageWithAttachment,
            LivechatWidgetMethod.sendMessage -> eventsListener?.onMessageSent(LivechatWidgetResult.Success(payload))
        }
    }
    //endregion

}