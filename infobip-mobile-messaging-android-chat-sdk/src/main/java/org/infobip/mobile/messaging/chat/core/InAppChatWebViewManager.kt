package org.infobip.mobile.messaging.chat.core

/**
 * Interface for Views with lifecycle (Activity, Fragment) that manage WebView
 */
internal interface InAppChatWebViewManager {
    fun onPageStarted(url: String)
    fun onPageFinished(url: String)
    fun onWidgetApiError(method: InAppChatWidgetApiMethod, errorPayload: String?)
    fun onWidgetApiSuccess(method: InAppChatWidgetApiMethod, successPayload: String?)
    fun setControlsVisibility(isVisible: Boolean)
    fun openAttachmentPreview(url: String?, type: String?, caption: String?)
    fun onWidgetViewChanged(widgetView: InAppChatWidgetView)
    fun onWidgetRawMessageReceived(message: String?)
}