/*
 * PushIdBroadcastReceiver.kt
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
import org.infobip.mobile.messaging.BroadcastParameter.EXTRA_INFOBIP_ID

class PushIdBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (Injector.cache.rtcUiMode == RtcUiMode.DEFAULT) {
            intent?.getStringExtra(EXTRA_INFOBIP_ID)?.let { pushRegId ->
                CallRegistrationWorker.launch(context, pushRegId)
            }
        }
    }
}