/*
 * LcRegIdBroadcastReceiver.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.infobip.webrtc.ui.internal.core.CallRegistrationWorker
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.model.RtcUiMode
import org.infobip.mobile.messaging.BroadcastParameter.EXTRA_LIVECHAT_REGISTRATION_ID

class LcRegIdBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        intent?.getStringExtra(EXTRA_LIVECHAT_REGISTRATION_ID)?.let { lcRegId ->
            if (Injector.cache.rtcUiMode == RtcUiMode.IN_APP_CHAT) {
                CallRegistrationWorker.launch(context, lcRegId, true)
            }
            if (lcRegId.isNotBlank())
                Injector.cache.livechatRegistrationId = lcRegId
        }
    }

}