/*
 * LivechatWidgetClient.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.chat.core.MultithreadStrategy
import org.infobip.mobile.messaging.chat.models.MessagePayload

/**
 * Low level interface for communication with JS Livechat Widget from native code.
 */
internal interface LivechatWidgetClient {

    /**
     * Send message defined by [payload] data.
     * The message is sent to the thread defined by [threadId] parameter, otherwise it is sent to the currently active thread.
     */
    fun send(payload: MessagePayload, threadId: String? = null, executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

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
     * Get array of widget threads. Return empty array if there are no threads or widget is not multithread.
     *
     * @param executionListener action listener
     */
    fun getThreads(executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Get active thread.
     *
     * @param executionListener action listener
     */
    fun getActiveThread(executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Create new thread with initial message defined by [payload] data.
     */
    fun createThread(payload: MessagePayload, executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Change widget destination to thread defined by [threadId].
     *
     * @param threadId threadId
     * @param executionListener action listener
     */
    fun showThread(threadId: String, executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Prepares the widget to start a new conversation by setting its destination to [LivechatWidgetView.THREAD].
     *
     * Note: This does not create the actual thread until the initial message is sent by the user.
     * @param executionListener action listener
     */
    fun openNewThread(executionListener: LivechatWidgetApi.ExecutionListener<String>? = null)

    /**
     * Change widget destination from thread to list in multiThread widget. For non multiThread widget it does nothing.
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