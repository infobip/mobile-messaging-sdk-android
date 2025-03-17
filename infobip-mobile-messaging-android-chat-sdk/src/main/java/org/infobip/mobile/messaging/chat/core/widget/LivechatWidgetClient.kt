package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy

/**
 * Low level interface for communication with JS Livechat Widget from native code.
 */
internal interface LivechatWidgetClient {

    /**
     * Send typed message and attachment
     * @param message user message
     * @param attachment IMAGE, VIDEO, DOCUMENT
     * @param executionListener action listener
     */
    fun sendMessage(message: String?, attachment: InAppChatMobileAttachment? = null, executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Send intermediate state of message input component
     *
     * @param draft user message draft
     * @param executionListener action listener
     */
    fun sendDraft(draft: String?, executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Set language of widget
     *
     * @param language locale contains country and language
     * @param executionListener action listener
     */
    fun setLanguage(language: LivechatWidgetLanguage, executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Send contextual metadata of conversation and a MultithreadStrategy flag
     *
     * @param data            contextual data in the form of JSON string
     * @param multiThreadFlag multithread strategy flag
     * @param executionListener action listener
     */
    fun sendContextualData(data: String, multiThreadFlag: MultithreadStrategy, executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Change destination from thread to list in multiThread widget. For non multiThread widget it does nothing.
     *
     * @param executionListener action listener
     */
    fun showThreadList(executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Close webSocket connection and be able to receive push notifications
     *
     * @param executionListener action listener
     */
    fun pauseConnection(executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Resume webSocket connection
     *
     * @param executionListener action listener
     */
    fun resumeConnection(executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Set widget theme. Widget themes are defined in Livechat widget in Infobip Portal, section Advanced customization.
     *
     * @param themeName theme name to be set
     * @param executionListener action listener
     */
    fun setTheme(themeName: String?, executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)
}