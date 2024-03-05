package com.infobip.webrtc.ui.internal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.core.RtcUiCallErrorMapperFactory
import com.infobip.webrtc.ui.internal.core.TAG
import com.infobip.webrtc.ui.internal.service.OngoingCallService
import com.infobip.webrtc.ui.model.RtcUiError

class PhoneStateBroadcastReceiver : BroadcastReceiver() {

    private val callDelegate by lazy { Injector.callsDelegate }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (phoneState != null) {
                Log.d(TAG, "Phone state has changed: $phoneState")
                when (phoneState) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {} // Incoming call ringing

                    TelephonyManager.EXTRA_STATE_OFFHOOK -> { // Call ongoing
                        callDelegate.getCallState()?.let { rtcCall ->
                            if (!rtcCall.isFinished && Injector.cache.autoFinishWhenIncomingCellularCallAccepted){
                                if (rtcCall.isEstablished)
                                    callDelegate.hangup()
                                else
                                    callDelegate.decline()
                                OngoingCallService.sendCallServiceIntent(context, OngoingCallService.CALL_ENDED_ACTION)
                                val message = RtcUiCallErrorMapperFactory.create(context).getMessageForError(RtcUiError.CELLULAR_CALL_ACCEPTED_WHILE_WEBRTC_CALL)
                                if (message?.isNotBlank() == true) {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {}  // Call ended or idle
                }
            }
        }
    }
}