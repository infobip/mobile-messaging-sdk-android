/*
 * InAppChatTheme.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view.styles

data class InAppChatTheme @JvmOverloads constructor(
    val chatToolbarStyle: InAppChatToolbarStyle = InAppChatToolbarStyle(),
    val attachmentToolbarStyle: InAppChatToolbarStyle = InAppChatToolbarStyle(),
    val chatStyle: InAppChatStyle = InAppChatStyle(),
    val chatInputViewStyle: InAppChatInputViewStyle = InAppChatInputViewStyle(),
)