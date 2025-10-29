/*
 * PushRegistrationBroadcastReceiver.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.infobip.mobile.messaging.BroadcastParameter
import org.infobip.mobile.messaging.chat.mobileapi.InAppChatSynchronizer

class PushRegistrationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        InAppChatSynchronizer(context, "PushRegistrationBroadcastReceiver").sync(
            pushRegistrationId = intent?.getStringExtra(BroadcastParameter.EXTRA_INFOBIP_ID),
            delay = 200L
        )
    }

}