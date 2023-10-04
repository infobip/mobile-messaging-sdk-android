package com.infobip.webrtc.ui.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.infobip.webrtc.CallRegistrationWorker
import com.infobip.webrtc.Injector
import com.infobip.webrtc.ui.model.RtcUiMode
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