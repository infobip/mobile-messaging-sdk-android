/*
 * InAppChatScreen.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat

import org.infobip.mobile.messaging.chat.view.DefaultInAppChatViewEventsListener
import org.infobip.mobile.messaging.chat.view.InAppChatErrorsHandler
import org.infobip.mobile.messaging.chat.view.InAppChatEventsListener

interface InAppChatScreen {

    /**
     * [InAppChatEventsListener] event listener allows you to listen for various in-app chat events.
     * It applies only when you show fullscreen in-app chat using `InAppChat.inAppChatScreen().show()`.
     * You can use [DefaultInAppChatViewEventsListener] to override only necessary methods.
     */
    var eventsListener: InAppChatEventsListener?

    /**
     * [InAppChatErrorsHandler] error handler allows you to define custom way to process in-app chat related errors.
     * It applies only when you show fullscreen in-app chat using `InAppChat.inAppChatScreen().show()`.
     * You can use [DefaultInAppChatErrorHandler] to override only necessary methods.
     */
    var errorHandler: InAppChatErrorsHandler?

    /**
     * Call this method to show the fullscreen in-app chat.
     */
    fun show()

}