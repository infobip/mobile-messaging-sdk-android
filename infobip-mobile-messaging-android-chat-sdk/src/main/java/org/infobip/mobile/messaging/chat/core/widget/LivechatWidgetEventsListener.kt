package org.infobip.mobile.messaging.chat.core.widget

/**
 * Interface for listening to events from livechat widget.
 *
 * You can use [DefaultLivechatWidgetEventsListener] to override only necessary methods.
 */
interface LivechatWidgetEventsListener {

    //Loading events
    /**
     * Called when livechat widget page starts loading.
     */
    fun onPageStarted(url: String?)

    /**
     * Called when livechat widget page loading finished.
     */
    fun onPageFinished(url: String?)

    /**
     * Called when livechat widget loading finished.
     */
    fun onLoadingFinished(result: LivechatWidgetResult<Unit>)

    //Widget API callbacks
    /**
     * Called when livechat widget connection is paused.
     */
    fun onConnectionPaused(result: LivechatWidgetResult<Unit>)

    /**
     * Called when livechat widget connection is resumed.
     */
    fun onConnectionResumed(result: LivechatWidgetResult<Unit>)

    /**
     * Called when message is sent.
     */
    fun onMessageSent(result: LivechatWidgetResult<String?>)

    /**
     * Called when draft message is sent.
     */
    fun onDraftSent(result: LivechatWidgetResult<String?>)

    /**
     * Called when contextual data is sent.
     */
    fun onContextualDataSent(result: LivechatWidgetResult<String?>)

    /**
     * Called when livechat widget threads were requested.
     */
    fun onThreadsReceived(result: LivechatWidgetResult<LivechatWidgetThreads>)

    /**
     * Called when livechat widget active thread was requested.
     * Success result can contains null if there is no existing thread for current user session or current widget destination is not [LivechatWidgetView.THREAD].
     */
    fun onActiveThreadReceived(result: LivechatWidgetResult<LivechatWidgetThread?>)

    /**
     * Called when livechat widget thread is shown.
     */
    fun onThreadShown(result: LivechatWidgetResult<LivechatWidgetThread>)

    /**
     * Called when livechat widget thread list is shown.
     */
    fun onThreadListShown(result: LivechatWidgetResult<Unit>)

    /**
     * Called when livechat widget language is changed.
     */
    fun onLanguageChanged(result: LivechatWidgetResult<String?>)

    /**
     * Called when livechat widget theme is changed.
     */
    fun onThemeChanged(result: LivechatWidgetResult<String?>)

    //Widget events
    /**
     * Called when livechat widget controls visibility changes.
     * It says when you can show or hide input field.
     */
    fun onControlsVisibilityChanged(visible: Boolean)

    /**
     * Called when livechat widget attachment preview is interacted.
     */
    fun onAttachmentPreviewOpened(url: String?, type: String?, caption: String?)

    /**
     * Called when livechat widget view changes.
     */
    fun onWidgetViewChanged(view: LivechatWidgetView)

    /**
     * Called when raw message is received from livechat widget.
     */
    fun onRawMessageReceived(message: String?)

}

/**
 * Default implementation of [LivechatWidgetEventsListener] with empty methods.
 * It allows you to override only necessary methods.
 */
open class DefaultLivechatWidgetEventsListener : LivechatWidgetEventsListener {
    override fun onPageStarted(url: String?) {}
    override fun onPageFinished(url: String?) {}
    override fun onLoadingFinished(result: LivechatWidgetResult<Unit>) {}
    override fun onControlsVisibilityChanged(visible: Boolean) {}
    override fun onAttachmentPreviewOpened(url: String?, type: String?, caption: String?) {}
    override fun onWidgetViewChanged(view: LivechatWidgetView) {}
    override fun onRawMessageReceived(message: String?) {}
    override fun onThreadsReceived(result: LivechatWidgetResult<LivechatWidgetThreads>) {}
    override fun onActiveThreadReceived(result: LivechatWidgetResult<LivechatWidgetThread?>) {}
    override fun onThreadShown(result: LivechatWidgetResult<LivechatWidgetThread>) {}
    override fun onConnectionPaused(result: LivechatWidgetResult<Unit>) {}
    override fun onConnectionResumed(result: LivechatWidgetResult<Unit>) {}
    override fun onDraftSent(result: LivechatWidgetResult<String?>) {}
    override fun onMessageSent(result: LivechatWidgetResult<String?>) {}
    override fun onContextualDataSent(result: LivechatWidgetResult<String?>) {}
    override fun onThemeChanged(result: LivechatWidgetResult<String?>) {}
    override fun onLanguageChanged(result: LivechatWidgetResult<String?>) {}
    override fun onThreadListShown(result: LivechatWidgetResult<Unit>) {}
}