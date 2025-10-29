/*
 * LivechatWidgetJsInterface.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core.widget

/**
 * Low level interface for communication from JS Livechat Widget to native code.
 */
internal interface LivechatWidgetJsInterface {

    companion object {
        /**
         * Name of JS interface. Must be used in livechat-widget.html when triggering native method from JS.
         */
        const val name = "InAppChatMobile"
    }

    /**
     * Provides controls visibility state.
     * It is triggered by Livechat widget when visibility of input field should be changed.
     * One of use-cases is when [Livechat pre-chat form](https://www.infobip.com/docs/live-chat/getting-started#pre-chat-forms-web-widget) is present, input field should be hidden.
     *
     * @param isVisible true if input field should be visible, false otherwise
     */
    fun setControlsVisibility(isVisible: Boolean)

    /**
     * Provides attachment data when attachment preview interacted in Livechat widget.
     * @param url link to cdn
     * @param type IMAGE, VIDEO, DOCUMENT (pdf file)
     * @param caption name of preview file
     */
    fun openAttachmentPreview(url: String?, type: String?, caption: String?)

    /**
     * Provides Livechat widget current view name.
     * @param view name of current widget destination
     */
    fun onViewChanged(view: String?)

    /**
     * Provides Livechat widget raw message.
     * @param message raw message
     */
    fun onRawMessageReceived(message: String?)

    /**
     * Provides Livechat widget api call error result.
     * @param method name of widget api function that caused error
     * @param errorPayload error payload
     */
    fun onWidgetApiError(method: String?, errorPayload: String?)

    /**
     * Provides Livechat widget api call success result.
     * @param method name of widget api function that returned result
     * @param successPayload result payload
     */
    fun onWidgetApiSuccess(method: String?, successPayload: String?)

}