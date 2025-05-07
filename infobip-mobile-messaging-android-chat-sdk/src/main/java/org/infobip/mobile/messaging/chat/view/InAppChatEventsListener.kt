package org.infobip.mobile.messaging.chat.view

import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.core.InAppChatWidgetView
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetMessage
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetResult
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThread
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThreads
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView

/**
 * Events listener propagates InAppChat related events.
 *
 * You can use [DefaultInAppChatEventsListener] to override only necessary methods.
 */
interface InAppChatEventsListener {

    /**
     * Called once chat has been loaded and connection established.
     *
     * @param controlsEnabled true if chat is loaded and connected without errors, false otherwise
     */
    @Deprecated("Use onChatLoadingFinished(result: LivechatWidgetResult<Unit>) instead")
    fun onChatLoaded(controlsEnabled: Boolean)

    /**
     * Called when chat loading finished.
     */
    fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>)

    /**
     * Chat connection has been stopped by [InAppChatView.stopConnection].
     * Chat loaded and connected state is not the same. Chat can be loaded but connection can be stopped.
     */
    @Deprecated("Use onChatConnectionPaused(result: LivechatWidgetResult<Unit>) instead")
    fun onChatDisconnected()

    /**
     * Called when chat connection has been paused by [InAppChatView.pauseChatConnection].
     * Chat loaded and connected state is not the same. Chat can be loaded but connection can be stopped.
     */
    fun onChatConnectionPaused(result: LivechatWidgetResult<Unit>)

    /**
     * Chat connection has been re-established by [InAppChatView.restartConnection].
     */
    @Deprecated("Use onConnectionResumed(result: LivechatWidgetResult<Unit>) instead")
    fun onChatReconnected()

    /**
     * Called when chat connection has been resumed by [InAppChatView.resumeChatConnection].
     */
    fun onChatConnectionResumed(result: LivechatWidgetResult<Unit>)

    /**
     * Called when chat message is sent.
     */
    @Deprecated("Use onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) instead")
    fun onChatMessageSent(result: LivechatWidgetResult<String?>)

    /**
     * Called when chat draft message is sent.
     */
    @Deprecated("Use onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) instead")
    fun onChatDraftSent(result: LivechatWidgetResult<String?>)

    /**
     * Called when any chat message payload is sent.
     */
    fun onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>)

    /**
     * Called when chat contextual data are sent.
     */
    fun onChatContextualDataSent(result: LivechatWidgetResult<String?>)

    /**
     * Called when livechat widget thread is created.
     */
    fun onChatThreadCreated(result: LivechatWidgetResult<LivechatWidgetMessage?>)

    /**
     * Called when chat threads were requested.
     */
    fun onChatThreadsReceived(result: LivechatWidgetResult<LivechatWidgetThreads>)

    /**
     * Called when chat active thread was requested.
     * Success result can contains null if there is no existing thread for current user session or current widget destination is not [LivechatWidgetView.THREAD].
     */
    fun onChatActiveThreadReceived(result: LivechatWidgetResult<LivechatWidgetThread?>)

    /**
     * Called when chat thread is shown.
     */
    fun onChatThreadShown(result: LivechatWidgetResult<LivechatWidgetThread>)

    /**
     * Called when chat thread list is shown.
     */
    fun onChatThreadListShown(result: LivechatWidgetResult<Unit>)

    /**
     * Called when chat language has been changed.
     */
    fun onChatLanguageChanged(result: LivechatWidgetResult<String?>)

    /**
     * Called when chat theme has been changed.
     */
    fun onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>)

    /**
     * Chat theme has changed.
     *
     * @param widgetThemeName name of the applied theme
     */
    @Deprecated("Use onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) instead")
    fun onChatWidgetThemeChanged(widgetThemeName: String)

    /**
     * Chat controls visibility has changed.
     *
     * @param isVisible true if controls are visible, false otherwise
     */
    fun onChatControlsVisibilityChanged(isVisible: Boolean)

    /**
     * Chat view has changed.
     *
     * @param widgetView current chat view
     */
    @Deprecated("Use onChatViewChanged(widgetView: LivechatWidgetView) instead")
    fun onChatViewChanged(widgetView: InAppChatWidgetView)

    /**
     * Chat view has changed.
     *
     * @param widgetView current chat view
     */
    fun onChatViewChanged(widgetView: LivechatWidgetView)

    /**
     * Chat [WidgetInfo] has been updated.
     *
     * @param widgetInfo updated widget info
     */
    fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo)

    /**
     * Chat message has been received.
     *
     * @param rawMessage raw message received by the chat
     */
    fun onChatRawMessageReceived(rawMessage: String)

}

/**
 * Default implementation of [InAppChatEventsListener] with empty methods.
 * It allows you to override only necessary methods.
 */
open class DefaultInAppChatEventsListener : InAppChatEventsListener {
    @Deprecated("Use onChatLoadingFinished(result: LivechatWidgetResult<Unit>) instead")
    override fun onChatLoaded(controlsEnabled: Boolean) {}
    override fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>) {}
    @Deprecated("Use onChatConnectionPaused(result: LivechatWidgetResult<Unit>) instead")
    override fun onChatDisconnected() {}
    override fun onChatConnectionPaused(result: LivechatWidgetResult<Unit>) {}
    @Deprecated("Use onChatConnectionResumed(result: LivechatWidgetResult<Unit>) instead")
    override fun onChatReconnected() {}
    override fun onChatConnectionResumed(result: LivechatWidgetResult<Unit>) {}
    @Deprecated("Use onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) instead")
    override fun onChatMessageSent(result: LivechatWidgetResult<String?>) {}
    @Deprecated("Use onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) instead")
    override fun onChatDraftSent(result: LivechatWidgetResult<String?>) {}
    override fun onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatContextualDataSent(result: LivechatWidgetResult<String?>) {}
    override fun onChatThreadCreated(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatControlsVisibilityChanged(isVisible: Boolean) {}
    override fun onChatViewChanged(widgetView: LivechatWidgetView) {}
    @Deprecated("Use onChatViewChanged(widgetView: LivechatWidgetView) instead")
    override fun onChatViewChanged(widgetView: InAppChatWidgetView) {}
    override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {}
    override fun onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) {}
    @Deprecated("Use onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) instead")
    override fun onChatWidgetThemeChanged(widgetThemeName: String) {}
    override fun onChatRawMessageReceived(rawMessage: String) {}
    override fun onChatThreadsReceived(result: LivechatWidgetResult<LivechatWidgetThreads>) {}
    override fun onChatActiveThreadReceived(result: LivechatWidgetResult<LivechatWidgetThread?>) {}
    override fun onChatThreadShown(result: LivechatWidgetResult<LivechatWidgetThread>) {}
    override fun onChatThreadListShown(result: LivechatWidgetResult<Unit>) {}
    override fun onChatLanguageChanged(result: LivechatWidgetResult<String?>) {}
}

/**
 * Default implementation of [InAppChatFragment.EventsListener] with empty methods.
 * It allows you to override only necessary methods.
 */
open class DefaultInAppChatFragmentEventsListener : InAppChatFragment.EventsListener {
    @Deprecated("Use onChatAttachmentPreviewOpened(url: String?, type: String?, caption: String?) instead")
    override fun onAttachmentPreviewOpened(url: String?, type: String?, caption: String?): Boolean = false
    override fun onChatAttachmentPreviewOpened(url: String?, type: String?, caption: String?): Boolean = false
    override fun onExitChatPressed() {}
    @Deprecated("Use onChatLoadingFinished(result: LivechatWidgetResult<Unit>) instead")
    override fun onChatLoaded(controlsEnabled: Boolean) {}
    override fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>) {}
    @Deprecated("Use onChatConnectionPaused(result: LivechatWidgetResult<Unit>) instead")
    override fun onChatDisconnected() {}
    override fun onChatConnectionPaused(result: LivechatWidgetResult<Unit>) {}
    @Deprecated("Use onChatConnectionResumed(result: LivechatWidgetResult<Unit>) instead")
    override fun onChatReconnected() {}
    override fun onChatConnectionResumed(result: LivechatWidgetResult<Unit>) {}
    @Deprecated("Use onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) instead")
    override fun onChatMessageSent(result: LivechatWidgetResult<String?>) {}
    @Deprecated("Use onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) instead")
    override fun onChatDraftSent(result: LivechatWidgetResult<String?>) {}
    override fun onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatContextualDataSent(result: LivechatWidgetResult<String?>) {}
    override fun onChatThreadCreated(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatThreadsReceived(result: LivechatWidgetResult<LivechatWidgetThreads>) {}
    override fun onChatActiveThreadReceived(result: LivechatWidgetResult<LivechatWidgetThread?>) {}
    override fun onChatThreadShown(result: LivechatWidgetResult<LivechatWidgetThread>) {}
    override fun onChatThreadListShown(result: LivechatWidgetResult<Unit>) {}
    override fun onChatLanguageChanged(result: LivechatWidgetResult<String?>) {}
    override fun onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) {}
    @Deprecated("Use onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) instead")
    override fun onChatWidgetThemeChanged(widgetThemeName: String) {}
    override fun onChatControlsVisibilityChanged(isVisible: Boolean) {}
    @Deprecated("Use onChatViewChanged(widgetView: LivechatWidgetView) instead")
    override fun onChatViewChanged(widgetView: InAppChatWidgetView) {}
    override fun onChatViewChanged(widgetView: LivechatWidgetView) {}
    override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {}
    override fun onChatRawMessageReceived(rawMessage: String) {}
}

/**
 * Default implementation of [InAppChatView.EventsListener] with empty methods.
 * It allows you to override only necessary methods.
 */
open class DefaultInAppChatViewEventsListener : InAppChatView.EventsListener {
    @Deprecated("Use onChatAttachmentPreviewOpened(url: String?, type: String?, caption: String?) instead")
    override fun onAttachmentPreviewOpened(url: String?, type: String?, caption: String?) {}
    override fun onChatAttachmentPreviewOpened(url: String?, type: String?, caption: String?) {}
    @Deprecated("Use onChatLoadingFinished(result: LivechatWidgetResult<Unit>) instead")
    override fun onChatLoaded(controlsEnabled: Boolean) {}
    override fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>) {}
    @Deprecated("Use onChatConnectionPaused(result: LivechatWidgetResult<Unit>) instead")
    override fun onChatDisconnected() {}
    override fun onChatConnectionPaused(result: LivechatWidgetResult<Unit>) {}
    @Deprecated("Use onChatConnectionResumed(result: LivechatWidgetResult<Unit>) instead")
    override fun onChatReconnected() {}
    override fun onChatConnectionResumed(result: LivechatWidgetResult<Unit>) {}
    @Deprecated("Use onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) instead")
    override fun onChatMessageSent(result: LivechatWidgetResult<String?>) {}
    @Deprecated("Use onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) instead")
    override fun onChatDraftSent(result: LivechatWidgetResult<String?>) {}
    override fun onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatContextualDataSent(result: LivechatWidgetResult<String?>) {}
    override fun onChatThreadCreated(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatThreadsReceived(result: LivechatWidgetResult<LivechatWidgetThreads>) {}
    override fun onChatActiveThreadReceived(result: LivechatWidgetResult<LivechatWidgetThread?>) {}
    override fun onChatThreadShown(result: LivechatWidgetResult<LivechatWidgetThread>) {}
    override fun onChatThreadListShown(result: LivechatWidgetResult<Unit>) {}
    override fun onChatLanguageChanged(result: LivechatWidgetResult<String?>) {}
    override fun onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) {}
    @Deprecated("Use onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) instead")
    override fun onChatWidgetThemeChanged(widgetThemeName: String) {}
    override fun onChatControlsVisibilityChanged(isVisible: Boolean) {}
    @Deprecated("Use onChatViewChanged(widgetView: LivechatWidgetView) instead")
    override fun onChatViewChanged(widgetView: InAppChatWidgetView) {}
    override fun onChatViewChanged(widgetView: LivechatWidgetView) {}
    override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {}
    override fun onChatRawMessageReceived(rawMessage: String) {}
}