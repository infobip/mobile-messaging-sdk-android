package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment
import org.infobip.mobile.messaging.chat.core.JwtProvider
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy
import org.infobip.mobile.messaging.chat.models.MessagePayload

/**
 * The `LivechatWidgetApi` interface provides methods to interact with the Livechat widget.
 * It allows you to perform various actions such as sending messages, setting themes, and managing the widget's state.
 * It creates queue for widget functions execution using Kotlin Mutex, single function is executed at a time.
 *
 * ### Using `LivechatWidgetEventsListener`
 * To get the real return value from the Livechat widget, you should use the [LivechatWidgetEventsListener].
 * This listener provides events that you can handle to get the actual data from the widget.
 *
 */
interface LivechatWidgetApi {

    /**
     * Execution listener interface for asynchronous operations.
     *
     * @param <T> type of successful result
     */
    fun interface ExecutionListener<T> {
        fun onResult(result: LivechatWidgetResult<T>)
    }

    companion object {
        const val TAG = "LivechatWidgetApi"
        const val MESSAGE_MAX_LENGTH = 4096
    }

    /**
     * Indicates if livechat widget is loaded.
     */
    val isWidgetLoaded: Boolean

    /**
     * Listener for livechat widget events. It allows to observe widget loading state, widget events and widget functions results.
     */
    var eventsListener: LivechatWidgetEventsListener?

    /**
     * [JwtProvider] gives livechat widget ability to authenticate.
     * Must be set before calling [loadWidget] method. If not set, it will be taken from current `InAppChat` configuration.
     */
    var jwtProvider: JwtProvider?

    /**
     * Livechat widget domain.
     * Must be set before calling [loadWidget] method. If not set, it will be taken from current `InAppChat` configuration.
     */
    var domain: String?

    /**
     * Timeout duration for loading the LiveChat widget, in milliseconds.
     *
     * This value must be set **before** calling [loadWidget].
     *
     * - **Minimum allowed:** 5,000 ms (5 seconds)
     * - **Maximum allowed:** 300,000 ms (5 minutes)
     * - **Default value:** 10,000 ms (10 seconds)
     *
     * @throws IllegalStateException if the value is set outside the allowed range.
     */
    @set:Throws(IllegalStateException::class)
    var loadingTimeoutMillis: Long

    /**
     * Triggers livechat widget loading. Does nothing, if widget is already loaded.
     * You can observe loading's result by [LivechatWidgetEventsListener.onLoadingFinished] event.
     * If you want to force load widget again, you need to call [reset] first.
     * If you want to load widget with current `InAppChat` configuration, you can call [loadWidget] without parameters.
     * You can control widget loading timeout by setting [loadingTimeoutMillis] property.
     *
     * @param widgetId livechat widget id to be loaded, if not provided, it will be taken from current configuration
     * @param jwt jwt token for authentication, if not provided, it will be taken from current `InAppChat` configuration
     * @param domain domain for livechat widget, if not provided, it will be taken from current `InAppChat` configuration
     * @param theme theme for livechat widget, if not provided, it will be taken from current `InAppChat` configuration
     * @param language language for livechat widget, if not provided, it will be taken from current `InAppChat` configuration
     */
    fun loadWidget(
        widgetId: String? = null,
        jwt: String? = null,
        domain: String? = null,
        theme: String? = null,
        language: LivechatWidgetLanguage? = null,
    )

    /**
     * Triggers livechat widget loading with current `InAppChat` configuration. Does nothing, if widget is already loaded.
     * You can observe loading's result by [LivechatWidgetEventsListener.onLoadingFinished] event.
     * If you want to force load widget again, you need to call [reset] first.
     * You can control widget loading timeout by setting [loadingTimeoutMillis] property.
     *
     * @param widgetId livechat widget id to be loaded, if not provided, it will be taken from current configuration
     */
    fun loadWidget(
        widgetId: String? = null,
    ) = loadWidget(widgetId, jwt = null, domain = null, theme = null, language = null)

    /**
     * Triggers livechat widget loading with current `InAppChat` configuration. Does nothing, if widget is already loaded.
     * You can observe loading's result by [LivechatWidgetEventsListener.onLoadingFinished] event.
     * If you want to force load widget again, you need to call [reset] first.
     * You can control widget loading timeout by setting [loadingTimeoutMillis] property.
     */
    fun loadWidget() = loadWidget(widgetId = null, jwt = null, domain = null, theme = null, language = null)

    /**
     * Pauses livechat widget connection, but widget stay loaded in WebView.
     * Widget connection and loaded state are independent.
     *
     * By the connection you can control push notifications.
     * Push notifications are active only when the connection is not active.
     *
     * Use [resumeConnection] to reestablish connection.
     *
     * To detect if the connection is paused use [LivechatWidgetEventsListener.onConnectionPaused] event.
     */
    fun pauseConnection()

    /**
     * Resumes livechat widget connection when you previously called [pauseConnection].
     *
     * By the connection you can control push notifications.
     * Push notifications are active only when the connection is not active.
     *
     * Use [pauseConnection] to pause connection.
     *
     * To detect if the connection is resumed use [LivechatWidgetEventsListener.onConnectionResumed] event.
     */
    fun resumeConnection()

    /**
     * Sends a message with optional [InAppChatMobileAttachment].
     *
     * You can observe result by [LivechatWidgetEventsListener.onMessageSent] event.
     *
     * @param message message to be send, max length allowed is 4096 characters
     * @param attachment to create attachment use [InAppChatMobileAttachment]'s constructor where you provide attachment's mimeType, base64 and filename
     */
    @Deprecated(
        message = "Use send(payload: MessagePayload) with MessagePayload.Basic() instead",
        replaceWith = ReplaceWith("send(MessagePayload.Basic(message, attachment))"),
    )
    fun sendMessage(message: String?, attachment: InAppChatMobileAttachment? = null)

    /**
     * Sends a message
     *
     * You can observe result by [LivechatWidgetEventsListener.onMessageSent] event.
     *
     * @param message message to be send, max length allowed is 4096 characters
     */
    @Deprecated(
        message = "Use send(payload: MessagePayload) with MessagePayload.Basic() instead",
        replaceWith = ReplaceWith("send(MessagePayload.Basic(message))"),
    )
    fun sendMessage(message: String) = sendMessage(message = message, attachment = null)

    /**
     * Sends a draft message.
     *
     * You can observe result by [LivechatWidgetEventsListener.onDraftSent] event.
     *
     * @param draft draft message to be send
     */
    @Deprecated(
        message = "Use send(payload: MessagePayload) with MessagePayload.Draft() instead",
        replaceWith = ReplaceWith("send(MessagePayload.Draft(draft))")
    )
    fun sendDraft(draft: String)

    /**
     * Sends a message defined by the given [payload] to the specified [threadId], if provided.
     * Otherwise, the message will be sent to the currently active thread.
     *
     * You can observe the result via the [LivechatWidgetEventsListener.onSent] event.
     *
     * @param payload The message payload to send.
     * @param threadId The ID of the existing thread to send the message to. If `null`, the active thread will be used.
     */
    fun send(payload: MessagePayload, threadId: String? = null)

    /**
     * Sends a message defined by the given [payload] to the currently active thread.
     *
     * You can observe the result via the [LivechatWidgetEventsListener.onSent] event.
     *
     * @param payload The message payload to send.
     */
    fun send(payload: MessagePayload) = send(payload = payload, threadId = null)

    /**
     * Creates a new thread with an initial message defined by the given [payload].
     *
     * You can observe the result via the [LivechatWidgetEventsListener.onThreadCreated] event.
     *
     * @param payload The message payload used to start the new thread.
     */
    fun createThread(payload: MessagePayload)

    /**
     * Sends contextual data.
     *
     * You can observe result by [LivechatWidgetEventsListener.onContextualDataSent] event.
     *
     * @param data contextual data in JSON format
     * @param multiThreadFlag multithread strategy flag
     */
    fun sendContextualData(data: String, multiThreadFlag: MultithreadStrategy)

    /**
     * Requests current threads from livechat widget.
     *
     * You can observe result by [LivechatWidgetEventsListener.onThreadsReceived] event.
     */
    fun getThreads()

    /**
     * Requests shown thread - active from livechat widget.
     *
     * You can observe result by [LivechatWidgetEventsListener.onActiveThreadReceived] event.
     */
    fun getActiveThread()

    /**
     * Navigates livechat widget to thread specified by provided [threadId].
     *
     * You can observe result by [LivechatWidgetEventsListener.onThreadShown] event.
     *
     * @param threadId thread to be shown
     */
    fun showThread(threadId: String)

    /**
     * Navigates livechat widget from [LivechatWidgetView.THREAD] back to [LivechatWidgetView.THREAD_LIST] destination in multithread widget. It does nothing if widget is not multithread.
     *
     * You can observe result by [LivechatWidgetEventsListener.onThreadListShown] or [LivechatWidgetEventsListener.onWidgetViewChanged] event.
     */
    fun showThreadList()

    /**
     * Sets a livechat widget's language.
     *
     * You can observe result by [LivechatWidgetEventsListener.onLanguageChanged] event.
     *
     * @param language language to be set
     */
    fun setLanguage(language: LivechatWidgetLanguage)

    /**
     * Sets a livechat widget's theme.
     *
     * You can observe result by [LivechatWidgetEventsListener.onThemeChanged] event.
     *
     * You can define widget themes in <a href="https://portal.infobip.com/apps/livechat/widgets">Live chat widget setup page</a> in Infobip Portal, section `Advanced customization`.
     * Please check widget <a href="https://www.infobip.com/docs/live-chat/widget-customization">documentation</a> for more details.
     *
     * @param themeName theme name to be set
     */
    fun setTheme(themeName: String)

    /**
     * Resets livechat widget state and loads blank page.
     */
    fun reset()

}