package com.infobip.webrtc.ui.internal.listener

import android.content.Context
import android.content.Intent
import com.infobip.webrtc.sdk.api.event.call.CallHangupEvent
import com.infobip.webrtc.sdk.api.event.listener.IncomingApplicationCallEventListener
import com.infobip.webrtc.sdk.api.event.listener.IncomingCallEventListener
import com.infobip.webrtc.sdk.api.event.rtc.IncomingApplicationCallEvent
import com.infobip.webrtc.sdk.api.event.rtc.IncomingWebrtcCallEvent
import com.infobip.webrtc.sdk.api.model.CallStatus
import com.infobip.webrtc.sdk.api.model.ErrorCode
import com.infobip.webrtc.ui.internal.model.RtcUiIncomingAppCallImpl
import com.infobip.webrtc.ui.internal.model.RtcUiIncomingCall
import com.infobip.webrtc.ui.internal.model.RtcUiIncomingWebrtcCallImpl
import com.infobip.webrtc.ui.internal.service.OngoingCallService
import com.infobip.webrtc.ui.internal.service.OngoingCallService.Companion.CALL_STATUS_EXTRA
import com.infobip.webrtc.ui.internal.service.OngoingCallService.Companion.INCOMING_CALL_ACTION
import com.infobip.webrtc.ui.internal.service.OngoingCallService.Companion.NAME_EXTRA

internal class IncomingCallEventListenerImpl(
    private val context: Context,
    private val pushPayload: Map<String, String>,
) : IncomingCallEventListener, IncomingApplicationCallEventListener {

    /**
     * Listener handles case when only ringing notification is present, without ActiveCallActivity. Once ActiveCallActivity is started
     * it replaces this listener with own one.
     */
    private val eventListener = object : DefaultRtcUiCallEventListener() {
        private fun stopCall() {
            OngoingCallService.sendCallServiceIntent(context, OngoingCallService.CALL_ENDED_ACTION)
        }

        override fun onHangup(callHangupEvent: CallHangupEvent?) {
            stopCall()
        }

        override fun onError(errorCode: ErrorCode?) {
            stopCall()
        }
    }

    private fun onIncomingCMCall(call: RtcUiIncomingCall) {
        if (call.status() == CallStatus.FINISHING || call.status() == CallStatus.FINISHED)
            return

        call.updateCustomData(pushPayload)
        call.setEventListener(eventListener)
        context.startService(Intent(context, OngoingCallService::class.java).apply {
            action = INCOMING_CALL_ACTION
            putExtra(NAME_EXTRA, call.peer(context))
            putExtra(CALL_STATUS_EXTRA, call.status()?.name)
        })
    }

    override fun onIncomingApplicationCall(incomingApplicationCallEvent: IncomingApplicationCallEvent?) {
        incomingApplicationCallEvent?.incomingApplicationCall?.let { call ->
            onIncomingCMCall(RtcUiIncomingAppCallImpl(call))
        }
    }

    override fun onIncomingWebrtcCall(incomingWebrtcCallEvent: IncomingWebrtcCallEvent?) {
        incomingWebrtcCallEvent?.incomingWebrtcCall?.let { call ->
            onIncomingCMCall(RtcUiIncomingWebrtcCallImpl(call))
        }
    }

}