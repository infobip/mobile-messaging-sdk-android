package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment
import org.infobip.mobile.messaging.chat.core.JwtProvider
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApi.ExecutionListener

/**
 * The `LivechatWidgetApi` interface provides methods to interact with the Livechat widget.
 * It allows you to perform various actions such as sending messages, setting themes, and managing the widget's state.
 * It creates queue for widget functions execution using Kotlin Mutex, single function is executed at a time.
 *
 * ### Important Note on `ExecutionListener`
 *
 * The [ExecutionListener] used in the `LivechatWidgetApi` methods does not return the actual return value from the Livechat widget.
 * Instead, it provides a way to listen for the completion of the execution of a method.
 * If you need to get the real return value from the Livechat widget, you should use the [LivechatWidgetEventsListener].
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
     * Triggers livechat widget loading. Does nothing, if widget is already loaded.
     * You can observe loading's result by [LivechatWidgetEventsListener.onLoadingFinished] event.
     * If you want to force load widget again, you need to call [reset] first.
     * If you want to load widget with current `InAppChat` configuration, you can call [loadWidget] without parameters.
     *
     * @param widgetId livechat widget id to be loaded, if not provided, it will be taken from current configuration
     * @param jwt jwt token for authentication, if not provided, it will be taken from current `InAppChat` configuration
     * @param domain domain for livechat widget, if not provided, it will be taken from current `InAppChat` configuration
     * @param theme theme for livechat widget, if not provided, it will be taken from current `InAppChat` configuration
     */
    fun loadWidget(
        widgetId: String? = null,
        jwt: String? = null,
        domain: String? = null,
        theme: String? = null,
    )

    /**
     * Triggers livechat widget loading with current `InAppChat` configuration. Does nothing, if widget is already loaded.
     * You can observe loading's result by [LivechatWidgetEventsListener.onLoadingFinished] event.
     * If you want to force load widget again, you need to call [reset] first.
     *
     * @param widgetId livechat widget id to be loaded, if not provided, it will be taken from current configuration
     */
    fun loadWidget(
        widgetId: String? = null,
    ) = loadWidget(widgetId, jwt = null, domain = null, theme = null)

    /**
     * Triggers livechat widget loading with current `InAppChat` configuration. Does nothing, if widget is already loaded.
     * You can observe loading's result by [LivechatWidgetEventsListener.onLoadingFinished] event.
     * If you want to force load widget again, you need to call [reset] first.
     */
    fun loadWidget() = loadWidget(widgetId = null, jwt = null, domain = null, theme = null)

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
     *
     * @param listener listen for the completion of the livechat widget method execution
     */
    fun pauseConnection(listener: ExecutionListener<String>? = null)

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
    fun pauseConnection() = pauseConnection(listener = null)

    /**
     * Resumes livechat widget connection when you previously called [pauseConnection].
     *
     * By the connection you can control push notifications.
     * Push notifications are active only when the connection is not active.
     *
     * Use [pauseConnection] to pause connection.
     *
     * To detect if the connection is resumed use [LivechatWidgetEventsListener.onConnectionResumed] event.
     *
     * @param listener listen for the completion of the livechat widget method execution
     */
    fun resumeConnection(listener: ExecutionListener<String>? = null)

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
    fun resumeConnection() = resumeConnection(listener = null)

    /**
     * Sends a message with optional [InAppChatMobileAttachment].
     *
     * You can observe result by [LivechatWidgetEventsListener.onMessageSent] event.
     *
     * @param message message to be send, max length allowed is 4096 characters
     * @param attachment to create attachment use [InAppChatMobileAttachment]'s constructor where you provide attachment's mimeType, base64 and filename
     * @param listener listen for the completion of the livechat widget method execution
     */
    fun sendMessage(message: String?, attachment: InAppChatMobileAttachment? = null, listener: ExecutionListener<String>? = null)

    /**
     * Sends a message.
     *
     * You can observe result by [LivechatWidgetEventsListener.onMessageSent] event.
     *
     * @param message message to be send, max length allowed is 4096 characters
     * @param listener listen for the completion of the livechat widget method execution
     */
    fun sendMessage(message: String?, listener: ExecutionListener<String>? = null) = sendMessage(message = message, attachment = null, listener = listener)

    /**
     * Sends a message
     *
     * You can observe result by [LivechatWidgetEventsListener.onMessageSent] event.
     *
     * @param message message to be send, max length allowed is 4096 characters
     */
    fun sendMessage(message: String?) = sendMessage(message = message, attachment = null, listener = null)

    /**
     * Sends a draft message.
     *
     * You can observe result by [LivechatWidgetEventsListener.onDraftSent] event.
     *
     * @param draft draft message to be send
     * @param listener listen for the completion of the livechat widget method execution
     */
    fun sendDraft(draft: String, listener: ExecutionListener<String>? = null)

    /**
     * Sends a draft message.
     *
     * You can observe result by [LivechatWidgetEventsListener.onDraftSent] event.
     *
     * @param draft draft message to be send
     */
    fun sendDraft(draft: String) = sendDraft(draft = draft, listener = null)

    /**
     * Sends contextual data.
     *
     * You can observe result by [LivechatWidgetEventsListener.onContextualDataSent] event.
     *
     * @param data contextual data in JSON format
     * @param multiThreadFlag multithread strategy flag
     * @param listener listen for the completion of the livechat widget method execution
     */
    fun sendContextualData(data: String, multiThreadFlag: MultithreadStrategy, listener: ExecutionListener<String>? = null)

    /**
     * Sends contextual data.
     *
     * You can observe result by [LivechatWidgetEventsListener.onContextualDataSent] event.
     *
     * @param data contextual data in JSON format
     * @param multiThreadFlag multithread strategy flag
     */
    fun sendContextualData(data: String, multiThreadFlag: MultithreadStrategy) = sendContextualData(data = data, multiThreadFlag = multiThreadFlag, listener = null)

    /**
     * Navigates livechat widget from [LivechatWidgetView.THREAD] back to [LivechatWidgetView.THREAD_LIST] destination in multithread widget. It does nothing if widget is not multithread.
     *
     * You can observe result by [LivechatWidgetEventsListener.onWidgetViewChanged] event.
     * @param listener listen for the completion of the livechat widget method execution
     */
    fun showThreadList(listener: ExecutionListener<String>? = null)

    /**
     * Navigates livechat widget from [LivechatWidgetView.THREAD] back to [LivechatWidgetView.THREAD_LIST] destination in multithread widget. It does nothing if widget is not multithread.
     *
     * You can observe result by [LivechatWidgetEventsListener.onWidgetViewChanged] event.
     */
    fun showThreadList() = showThreadList(listener = null)

    /**
     * Sets a livechat widget's language.
     *
     * You can observe result by [LivechatWidgetEventsListener.onLanguageChanged] event.
     *
     * @param language language to be set
     * @param listener listen for the completion of the livechat widget method execution
     */
    fun setLanguage(language: LivechatWidgetLanguage, listener: ExecutionListener<String>? = null)

    /**
     * Sets a livechat widget's language.
     *
     * You can observe result by [LivechatWidgetEventsListener.onLanguageChanged] event.
     *
     * @param language language to be set
     */
    fun setLanguage(language: LivechatWidgetLanguage) = setLanguage(language = language, listener = null)

    /**
     * Sets a livechat widget's theme.
     *
     * You can observe result by [LivechatWidgetEventsListener.onThemeChanged] event.
     *
     * You can define widget themes in <a href="https://portal.infobip.com/apps/livechat/widgets">Live chat widget setup page</a> in Infobip Portal, section `Advanced customization`.
     * Please check widget <a href="https://www.infobip.com/docs/live-chat/widget-customization">documentation</a> for more details.
     *
     * @param themeName theme name to be set
     * @param listener listen for the completion of the livechat widget method execution
     */
    fun setTheme(themeName: String, listener: ExecutionListener<String>? = null)

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
    fun setTheme(themeName: String) = setTheme(themeName = themeName, listener = null)

    /**
     * Resets livechat widget state and loads blank page.
     */
    fun reset()

}