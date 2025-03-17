package org.infobip.mobile.messaging.chat.core.widget

/**
 * Interface provides WebView events together with events from Livechat Widget.
 */
internal interface LivechatWidgetWebViewManager {
    fun onPageStarted(url: String?)
    fun onPageFinished(url: String?)
    fun setControlsVisibility(isVisible: Boolean)
    fun openAttachmentPreview(url: String?, type: String?, caption: String?)
    fun onWidgetViewChanged(widgetView: LivechatWidgetView)
    fun onWidgetRawMessageReceived(message: String?)
    fun onWidgetApiError(method: LivechatWidgetMethod, errorPayload: String?)
    fun onWidgetApiSuccess(method: LivechatWidgetMethod, successPayload: String?)
}