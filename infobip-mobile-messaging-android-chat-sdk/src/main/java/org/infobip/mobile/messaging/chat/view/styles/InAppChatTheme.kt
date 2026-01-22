/*
 * InAppChatTheme.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view.styles

/**
 * Complete theme configuration for InAppChat.
 *
 * This data class aggregates all style classes used to customize the appearance and behavior
 * of the entire in-app chat experience, including the toolbar, main chat view, input field,
 * and attachment preview toolbar.
 *
 * ### Structure
 * The theme combines four style classes:
 * - [chatToolbarStyle] - Main chat screen toolbar
 * - [attachmentToolbarStyle] - Attachment preview screen toolbar
 * - [chatStyle] - Main chat view (background, error screen, connection banner)
 * - [chatInputViewStyle] - Message input field and action buttons
 *
 * ### Property Resolution Priority
 * Each property in the aggregated style classes follows a 4-level resolution priority:
 * 1. Runtime values set via Builder (highest priority)
 * 2. XML theme attributes (prefixed with `ibChat*`)
 * 3. WidgetInfo server configuration
 * 4. Default values (lowest priority)
 *
 * ### Usage Example
 * ```kotlin
 * val theme = InAppChatTheme(
 *     chatToolbarStyle = InAppChatToolbarStyle.Builder()
 *         .setToolbarBackgroundColor(Color.BLUE)
 *         .setTitleText("Chat")
 *         .build(),
 *     attachmentToolbarStyle = InAppChatToolbarStyle.Builder()
 *         .setToolbarBackgroundColor(Color.BLUE)
 *         .setTitleText("Attachments")
 *         .build(),
 *     chatStyle = InAppChatStyle.Builder()
 *         .setBackgroundColor(Color.WHITE)
 *         .setChatFullScreenErrorBackgroundColor(Color.WHITE)
 *         .build(),
 *     chatInputViewStyle = InAppChatInputViewStyle.Builder()
 *         .setTextColor(Color.BLACK)
 *         .setBackgroundColor(Color.WHITE)
 *         .build()
 * )
 *
 * InAppChat.getInstance(context).setTheme(theme)
 * ```
 *
 * @see InAppChatToolbarStyle for toolbar customization options
 * @see InAppChatStyle for main chat view customization options
 * @see InAppChatInputViewStyle for input field customization options
 */
data class InAppChatTheme @JvmOverloads constructor(
    val chatToolbarStyle: InAppChatToolbarStyle = InAppChatToolbarStyle(),
    val attachmentToolbarStyle: InAppChatToolbarStyle = InAppChatToolbarStyle(),
    val chatStyle: InAppChatStyle = InAppChatStyle(),
    val chatInputViewStyle: InAppChatInputViewStyle = InAppChatInputViewStyle(),
)