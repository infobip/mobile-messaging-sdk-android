/*
 * InAppChatNotificationInteractionHandler.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core

import org.infobip.mobile.messaging.Message

/**
 * Interface for handling interactions with in-app chat related notifications.
 *
 * Implement this interface to define custom behavior when a user interacts with an in-app chat related notification.
 */
fun interface InAppChatNotificationInteractionHandler {

    /**
     * Called when a user interacts with an in-app chat related notification.
     * You can detect notification type by checking if the message is a chat message or an action exist.
     * ```
     * val action = OpenLivechatAction.parseFrom(message)
     * if (message.isChatMessage()) {
     *     // in-app chat message push notification tapped
     * } else if (action != null) {
     *     // notification action "Open chatbot in LiveChat" triggered
     * }
     * ```
     *
     * @param message The message associated with the notification.
     */
    fun onNotificationInteracted(message: Message)
}