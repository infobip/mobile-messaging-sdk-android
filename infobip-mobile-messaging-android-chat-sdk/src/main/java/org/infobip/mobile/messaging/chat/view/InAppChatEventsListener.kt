/*
 * InAppChatEventsListener.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view

import org.infobip.mobile.messaging.api.chat.WidgetInfo
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
     * Called when chat loading finished.
     */
    fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>)

    /**
     * Called when chat connection has been paused by [InAppChatView.pauseChatConnection].
     * Chat loaded and connected state is not the same. Chat can be loaded but connection can be stopped.
     */
    fun onChatConnectionPaused(result: LivechatWidgetResult<Unit>)

    /**
     * Called when chat connection has been resumed by [InAppChatView.resumeChatConnection].
     */
    fun onChatConnectionResumed(result: LivechatWidgetResult<Unit>)

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
    override fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>) {}
    override fun onChatConnectionPaused(result: LivechatWidgetResult<Unit>) {}
    override fun onChatConnectionResumed(result: LivechatWidgetResult<Unit>) {}
    override fun onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatContextualDataSent(result: LivechatWidgetResult<String?>) {}
    override fun onChatThreadCreated(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatControlsVisibilityChanged(isVisible: Boolean) {}
    override fun onChatViewChanged(widgetView: LivechatWidgetView) {}
    override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {}
    override fun onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) {}
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
    override fun onChatAttachmentPreviewOpened(url: String?, type: String?, caption: String?): Boolean = false
    override fun onExitChatPressed() {}
    override fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>) {}
    override fun onChatConnectionPaused(result: LivechatWidgetResult<Unit>) {}
    override fun onChatConnectionResumed(result: LivechatWidgetResult<Unit>) {}
    override fun onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatContextualDataSent(result: LivechatWidgetResult<String?>) {}
    override fun onChatThreadCreated(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatThreadsReceived(result: LivechatWidgetResult<LivechatWidgetThreads>) {}
    override fun onChatActiveThreadReceived(result: LivechatWidgetResult<LivechatWidgetThread?>) {}
    override fun onChatThreadShown(result: LivechatWidgetResult<LivechatWidgetThread>) {}
    override fun onChatThreadListShown(result: LivechatWidgetResult<Unit>) {}
    override fun onChatLanguageChanged(result: LivechatWidgetResult<String?>) {}
    override fun onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) {}
    override fun onChatControlsVisibilityChanged(isVisible: Boolean) {}
    override fun onChatViewChanged(widgetView: LivechatWidgetView) {}
    override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {}
    override fun onChatRawMessageReceived(rawMessage: String) {}
}

/**
 * Default implementation of [InAppChatView.EventsListener] with empty methods.
 * It allows you to override only necessary methods.
 */
open class DefaultInAppChatViewEventsListener : InAppChatView.EventsListener {
    override fun onChatAttachmentPreviewOpened(url: String?, type: String?, caption: String?) {}
    override fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>) {}
    override fun onChatConnectionPaused(result: LivechatWidgetResult<Unit>) {}
    override fun onChatConnectionResumed(result: LivechatWidgetResult<Unit>) {}
    override fun onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatContextualDataSent(result: LivechatWidgetResult<String?>) {}
    override fun onChatThreadCreated(result: LivechatWidgetResult<LivechatWidgetMessage?>) {}
    override fun onChatThreadsReceived(result: LivechatWidgetResult<LivechatWidgetThreads>) {}
    override fun onChatActiveThreadReceived(result: LivechatWidgetResult<LivechatWidgetThread?>) {}
    override fun onChatThreadShown(result: LivechatWidgetResult<LivechatWidgetThread>) {}
    override fun onChatThreadListShown(result: LivechatWidgetResult<Unit>) {}
    override fun onChatLanguageChanged(result: LivechatWidgetResult<String?>) {}
    override fun onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) {}
    override fun onChatControlsVisibilityChanged(isVisible: Boolean) {}
    override fun onChatViewChanged(widgetView: LivechatWidgetView) {}
    override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {}
    override fun onChatRawMessageReceived(rawMessage: String) {}
}