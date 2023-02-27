package com.infobip.webrtc.ui.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.infobip.webrtc.Injector
import com.infobip.webrtc.TAG
import com.infobip.webrtc.ui.ErrorListener
import com.infobip.webrtc.ui.SuccessListener
import com.infobip.webrtc.ui.model.ListenType
import org.infobip.mobile.messaging.BroadcastParameter.EXTRA_INFOBIP_ID

class PushIdBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (Injector.cache.inAppCallsEnabled) {
            intent?.getStringExtra(EXTRA_INFOBIP_ID)?.let { pushRegId ->
                Injector.getWebrtcUi(context).enableCalls(
                    identity = pushRegId,
                    listenType = ListenType.PUSH,
                    successListener = Injector.enableInAppCallsSuccess ?: SuccessListener { Log.d(TAG, "InAppCalls enabled from broadcast.") },
                    errorListener = Injector.enableInAppCallsError ?: ErrorListener { Log.e(TAG, "Failed to enabled InAppCalls from broadcast.", it) }
                )
            }
        }
    }
}