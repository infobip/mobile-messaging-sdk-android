package org.infobip.mobile.messaging.chat.core

/**
 * Interface for Views with lifecycle (Activity, Fragment) that manage WebView
 */
internal interface InAppChatWebViewManager {
    fun onPageStarted()
    fun onPageFinished()
    fun setControlsEnabled(enabled: Boolean)
    fun onJSError(message: String?)
    fun setControlsVisibility(isVisible: Boolean)
    fun openAttachmentPreview(url: String?, type: String?, caption: String?)
    fun onWidgetViewChanged(widgetView: InAppChatWidgetView)
}