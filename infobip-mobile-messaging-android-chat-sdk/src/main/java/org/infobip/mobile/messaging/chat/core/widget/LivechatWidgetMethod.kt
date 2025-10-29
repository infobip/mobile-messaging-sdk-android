/*
 * LivechatWidgetMethod.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core.widget

/**
 * Represents Livechat widget JS methods.
 * It must match with JS methods in livechat-widget.html.
 */
internal enum class LivechatWidgetMethod {
    config,
    identify,
    initWidget, //init is Kotlin reserved keyword so there is Widget postfix used
    show,
    pauseConnection,
    resumeConnection,
    sendMessage,
    sendContextualData,
    setTheme,
    setLanguage,
    getThreads,
    getActiveThread,
    createThread,
    showThread,
    showThreadList,
    openNewThread,
}