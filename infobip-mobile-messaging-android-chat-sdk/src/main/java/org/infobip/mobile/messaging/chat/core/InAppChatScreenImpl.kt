/*
 * InAppChatScreenImpl.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core

import android.content.Context
import org.infobip.mobile.messaging.chat.InAppChatScreen
import org.infobip.mobile.messaging.chat.view.InAppChatActivity
import org.infobip.mobile.messaging.chat.view.InAppChatErrorsHandler
import org.infobip.mobile.messaging.chat.view.InAppChatEventsListener

internal class InAppChatScreenImpl(
    private val context: Context,
    private val sessionStorage: SessionStorage,
) : InAppChatScreen {

    override var eventsListener: InAppChatEventsListener? by sessionStorage::inAppChatEventsListener

    override var errorHandler: InAppChatErrorsHandler? by sessionStorage::inAppChatErrorsHandler

    override fun show() {
        context.startActivity(InAppChatActivity.startIntent(context))
    }

}