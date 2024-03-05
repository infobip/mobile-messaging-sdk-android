package com.infobip.webrtc.ui.internal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.infobip.webrtc.ui.internal.core.CallRegistrationWorker
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.model.RtcUiMode
import org.infobip.mobile.messaging.BroadcastParameter.EXTRA_LIVECHAT_REGISTRATION_ID

class LcRegIdBroadcastReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (Injector.cache.rtcUiMode == RtcUiMode.IN_APP_CHAT) {
            intent?.getStringExtra(EXTRA_LIVECHAT_REGISTRATION_ID)?.let { lcRegId ->
                CallRegistrationWorker.launch(context, lcRegId, true)
            }
        }
    }

}