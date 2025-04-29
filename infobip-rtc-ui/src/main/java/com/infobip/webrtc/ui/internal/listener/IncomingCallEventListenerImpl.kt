package com.infobip.webrtc.ui.internal.listener

import android.content.Context
import android.util.Log
import com.infobip.webrtc.sdk.api.event.call.CallHangupEvent
import com.infobip.webrtc.sdk.api.event.listener.IncomingApplicationCallEventListener
import com.infobip.webrtc.sdk.api.event.listener.IncomingCallEventListener
import com.infobip.webrtc.sdk.api.event.rtc.IncomingApplicationCallEvent
import com.infobip.webrtc.sdk.api.event.rtc.IncomingWebrtcCallEvent
import com.infobip.webrtc.sdk.api.model.CallStatus
import com.infobip.webrtc.sdk.api.model.ErrorCode
import com.infobip.webrtc.ui.internal.core.TAG
import com.infobip.webrtc.ui.internal.model.CallAction
import com.infobip.webrtc.ui.internal.model.RtcUiIncomingAppCallImpl
import com.infobip.webrtc.ui.internal.model.RtcUiIncomingCall
import com.infobip.webrtc.ui.internal.model.RtcUiIncomingWebrtcCallImpl
import com.infobip.webrtc.ui.internal.service.ActiveCallService

internal class IncomingCallEventListenerImpl(
    private val context: Context,
    private val pushPayload: Map<String, String>,
) : IncomingCallEventListener, IncomingApplicationCallEventListener {

    /**
     * Listener handles case when only ringing notification is present, without ActiveCallActivity. Once ActiveCallActivity is started
     * it replaces this listener with own one.
     */
    private val eventListener = object : DefaultRtcUiCallEventListener() {

        override fun onHangup(callHangupEvent: CallHangupEvent?) {
            Log.d(TAG, "onHangup ${callHangupEvent?.errorCode?.name}")
            ActiveCallService.start(context, CallAction.CALL_FINISHED)
        }

        override fun onError(errorCode: ErrorCode?) {
            Log.d(TAG, "onError ${errorCode?.name}")
            ActiveCallService.start(context, CallAction.CALL_FINISHED)
        }
    }

    private fun onIncomingCMCall(call: RtcUiIncomingCall) {
        if (call.status() == CallStatus.FINISHING || call.status() == CallStatus.FINISHED)
            return

        call.updateCustomData(pushPayload)
        call.setEventListener(eventListener)
        ActiveCallService.start(
            context = context,
            action = CallAction.INCOMING_CALL_START,
            peer = call.peer(context),
            callStatus = call.status(),
            foreground = true
        )
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