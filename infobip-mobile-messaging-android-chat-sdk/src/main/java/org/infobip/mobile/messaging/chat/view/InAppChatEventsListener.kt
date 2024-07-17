package org.infobip.mobile.messaging.chat.view

import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.core.InAppChatWidgetView

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
    fun onChatViewChanged(widgetView: InAppChatWidgetView)

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
}