package com.infobip.webrtc.ui.delegate

import android.content.Context
import android.content.Intent
import android.util.Log
import com.infobip.webrtc.sdk.api.InfobipRTC
import com.infobip.webrtc.sdk.api.application.ApplicationCall
import com.infobip.webrtc.sdk.api.application.IncomingApplicationCall
import com.infobip.webrtc.sdk.api.call.CallStatus
import com.infobip.webrtc.sdk.api.call.options.VideoOptions
import com.infobip.webrtc.sdk.api.conference.RemoteVideo
import com.infobip.webrtc.sdk.api.event.ApplicationCallEventListener
import com.infobip.webrtc.sdk.api.event.call.CallEstablishedEvent
import com.infobip.webrtc.sdk.api.event.call.CallHangupEvent
import com.infobip.webrtc.sdk.api.event.conference.ErrorEvent
import com.infobip.webrtc.sdk.api.event.rtc.EnablePushNotificationResult
import com.infobip.webrtc.sdk.api.event.rtc.IncomingApplicationCallEvent
import com.infobip.webrtc.sdk.api.video.RTCVideoTrack
import com.infobip.webrtc.sdk.api.video.ScreenCapturer
import com.infobip.webrtc.sdk.impl.event.DefaultApplicationCallEventListener
import com.infobip.webrtc.sdk.impl.event.DefaultIncomingCallEventListener
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.model.CallState
import com.infobip.webrtc.ui.service.OngoingCallService
import com.infobip.webrtc.ui.service.OngoingCallService.Companion.CALL_STATUS_EXTRA
import com.infobip.webrtc.ui.service.OngoingCallService.Companion.INCOMING_CALL_ACTION
import com.infobip.webrtc.ui.service.OngoingCallService.Companion.NAME_EXTRA
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal interface CallsDelegate {
    fun accept()
    fun decline()
    fun hangup()
    fun remoteVideos(): Map<String, RemoteVideo>?
    fun screenShareTrack(): RTCVideoTrack?
    fun remoteVideoTrack(): RTCVideoTrack?
    fun localVideoTrack(): RTCVideoTrack?
    fun setEventListener(listener: ApplicationCallEventListener)
    fun duration(): Int
    fun flipCamera()
    fun frontCamera()
    fun backCamera()
    fun cameraVideo(enabled: Boolean)
    fun mute(isMuted: Boolean)
    fun speaker(isSpeaker: Boolean)
    fun shareScreen(screenCapturer: ScreenCapturer)
    fun stopScreenShare()
    fun hasScreenShare(): Boolean
    fun hasRemoteVideo(): Boolean
    fun getCallState(): CallState?
    fun getCallStatus(): CallStatus
    fun isIncomingCall(data: Map<String, String>): Boolean
    fun handlePushMessage(data: Map<String, String>)
    fun registerActiveConnection(token: String)
    fun enablePush(token: String, onResult: (EnablePushNotificationResult) -> Unit)
}

internal class CallsDelegateImpl(
        private val context: Context,
        private val callsScope: CoroutineScope
) : CallsDelegate {
    private val call: ApplicationCall?
        get() = InfobipRTC.getActiveApplicationCall()

    override fun accept() {
        (call as? IncomingApplicationCall)?.accept()
    }

    override fun decline() {
        (call as? IncomingApplicationCall)?.decline()
    }

    override fun hangup() {
        call?.hangup()
    }

    override fun remoteVideos(): Map<String, RemoteVideo>? {
        return call?.remoteVideos()
    }

    override fun screenShareTrack(): RTCVideoTrack? {
        return call?.remoteVideos()?.values?.firstOrNull { it.screenShare != null }?.screenShare
    }

    override fun remoteVideoTrack(): RTCVideoTrack? {
        return call?.remoteVideos()?.values?.firstOrNull { it.camera != null }?.camera
    }

    override fun localVideoTrack(): RTCVideoTrack? {
        return call?.localCameraTrack()
    }

    override fun setEventListener(listener: ApplicationCallEventListener) {
        call?.eventListener = listener
    }

    override fun duration(): Int = call?.duration() ?: 0

    override fun flipCamera() {
        if (call?.hasCameraVideo() == true) {
            if (call?.cameraOrientation() == VideoOptions.CameraOrientation.FRONT) {
                backCamera()
            } else {
                frontCamera()
            }
        }
    }

    override fun frontCamera() {
        call?.cameraOrientation(VideoOptions.CameraOrientation.FRONT)
    }

    override fun backCamera() {
        call?.cameraOrientation(VideoOptions.CameraOrientation.BACK)
    }

    override fun cameraVideo(enabled: Boolean) {
        call?.cameraVideo(enabled)
    }

    override fun mute(isMuted: Boolean) {
        call?.mute(isMuted)
    }

    override fun speaker(isSpeaker: Boolean) {
        call?.speakerphone(isSpeaker)
    }

    override fun shareScreen(screenCapturer: ScreenCapturer) {
        call?.startScreenShare(screenCapturer)
    }

    override fun stopScreenShare() {
        call?.stopScreenShare()
    }

    override fun hasScreenShare(): Boolean {
        return call?.hasScreenShare() ?: false
    }

    override fun hasRemoteVideo(): Boolean {
        return call?.remoteVideos()?.isNotEmpty() == true
    }

    override fun getCallState(): CallState? {
        return call?.let { call ->
            runCatching {
                val remoteVideoTrack = remoteVideoTrack()
                val screenShareTrack = screenShareTrack()
                val localVideoTrack = localVideoTrack()
                CallState(
                    isIncoming = call.duration() == 0,
                    isLocalVideo = localVideoTrack != null,
                    isMuted = call.muted(),
                    isPeerMuted = call.participants().firstOrNull()?.media?.audio?.muted == true,
                    elapsedTimeSeconds = call.duration(),
                        isSpeakerOn = call.speakerphone(),
                        isScreenShare = call.hasScreenShare(),
                        isWeakConnection = false,
                        isPip = false,
                        isFinished = call.status()?.let { it == CallStatus.FINISHED || it == CallStatus.FINISHING } == true,
                        error = "",
                        localVideoTrack = localVideoTrack,
                        remoteVideoTrack = remoteVideoTrack,
                        screenShareTrack = screenShareTrack
                )
            }.getOrNull()
        }
    }

    override fun getCallStatus(): CallStatus {
        return call?.status() ?: CallStatus.FINISHED
    }

    override fun isIncomingCall(data: Map<String, String>): Boolean {
        return InfobipRTC.isIncomingApplicationCall(data)
    }

    override fun handlePushMessage(data: Map<String, String>) {
        InfobipRTC.handleIncomingApplicationCall(data, context, IncomingCallListener(context, callsScope, data))
    }

    override fun registerActiveConnection(token: String) {
        InfobipRTC.registerForActiveConnection(
                token,
                context,
                IncomingCallListener(context, callsScope, mapOf())
        )
    }

    override fun enablePush(token: String, onResult: (EnablePushNotificationResult) -> Unit) {
        InfobipRTC.enablePushNotification(token, context) {
            onResult(it)
        }
    }

    private class IncomingCallListener(
            private val context: Context,
            private val callsScope: CoroutineScope,
            private val data: Map<String, String>
    ) : DefaultIncomingCallEventListener() {

        override fun onIncomingApplicationCall(incomingApplicationCallEvent: IncomingApplicationCallEvent?) {
            incomingApplicationCallEvent?.incomingApplicationCall?.let { call ->
                if (call.callOptions().customData.isNullOrEmpty()) {
                    call.callOptions().customData = data
                }
                val name = call.fromDisplayName()?.takeIf { it.isNotEmpty() } ?: call.from()
                ?: context.getString(R.string.mm_unknown)
                context.startService(Intent(context, OngoingCallService::class.java).apply {
                    action = INCOMING_CALL_ACTION
                    putExtra(NAME_EXTRA, name)
                    putExtra(CALL_STATUS_EXTRA, call.status()?.name)
                })

                call.eventListener = object : DefaultApplicationCallEventListener() {
                    private fun stopCall() {
                        callsScope.launch(Dispatchers.Main) {
                            OngoingCallService.sendCallServiceIntent(context, OngoingCallService.CALL_ENDED_ACTION)
                        }
                    }

                    override fun onHangup(callHangupEvent: CallHangupEvent?) {
                        stopCall()
                    }

                    override fun onError(errorEvent: ErrorEvent?) {
                        stopCall()
                    }

                    override fun onEstablished(callEstablishedEvent: CallEstablishedEvent?) {
                        callsScope.launch(Dispatchers.Main) {
                            OngoingCallService.sendCallServiceIntent(context, OngoingCallService.CALL_ESTABLISHED_ACTION)
                        }
                    }
                }
            }
        }
    }
}