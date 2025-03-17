package org.infobip.mobile.messaging.chat.view

import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.core.InAppChatWidgetView
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView

/**
 * Events listener propagates InAppChat related events.
 */
interface InAppChatEventsListener {
    /**
     * Called once chat has been loaded and connection established.
     *
     * @param controlsEnabled true if chat is loaded and connected without errors, false otherwise
     */
    fun onChatLoaded(controlsEnabled: Boolean)

    /**
     * Chat connection has been stopped by [InAppChatView.stopConnection].
     * Chat loaded and connected state is not the same. Chat can be loaded but connection can be stopped.
     */
    fun onChatDisconnected()

    /**
     * Chat connection has been re-established by [InAppChatView.restartConnection].
     */
    fun onChatReconnected()

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
     * Chat theme has changed.
     *
     * @param widgetThemeName name of the applied theme
     */
    fun onChatWidgetThemeChanged(widgetThemeName: String)

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
    override fun onChatLoaded(controlsEnabled: Boolean) {}
    override fun onChatDisconnected() {}
    override fun onChatReconnected() {}
    override fun onChatControlsVisibilityChanged(isVisible: Boolean) {}
    override fun onChatViewChanged(widgetView: InAppChatWidgetView) {}
    override fun onChatViewChanged(widgetView: LivechatWidgetView) {}
    override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {}
    override fun onChatWidgetThemeChanged(widgetThemeName: String) {}
    override fun onChatRawMessageReceived(rawMessage: String) {}
}