/*
 * CallsDelegate.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.delegate

import android.content.Context
import android.util.Log
import com.infobip.webrtc.sdk.api.InfobipRTC
import com.infobip.webrtc.sdk.api.call.ApplicationCall
import com.infobip.webrtc.sdk.api.call.IncomingApplicationCall
import com.infobip.webrtc.sdk.api.call.IncomingWebrtcCall
import com.infobip.webrtc.sdk.api.call.WebrtcCall
import com.infobip.webrtc.sdk.api.event.listener.IncomingApplicationCallEventListener
import com.infobip.webrtc.sdk.api.event.listener.IncomingCallEventListener
import com.infobip.webrtc.sdk.api.event.listener.NetworkQualityEventListener
import com.infobip.webrtc.sdk.api.model.CallStatus
import com.infobip.webrtc.sdk.api.model.RemoteVideo
import com.infobip.webrtc.sdk.api.model.push.EnablePushNotificationResult
import com.infobip.webrtc.sdk.api.model.video.RTCVideoTrack
import com.infobip.webrtc.sdk.api.model.video.ScreenCapturer
import com.infobip.webrtc.sdk.api.options.VideoOptions
import com.infobip.webrtc.ui.internal.core.TAG
import com.infobip.webrtc.ui.internal.listener.IncomingCallEventListenerImpl
import com.infobip.webrtc.ui.internal.listener.RtcUiCallEventListener
import com.infobip.webrtc.ui.internal.model.CallState
import com.infobip.webrtc.ui.internal.model.RtcUiAppCall
import com.infobip.webrtc.ui.internal.model.RtcUiCall
import com.infobip.webrtc.ui.internal.model.RtcUiIncomingAppCallImpl
import com.infobip.webrtc.ui.internal.model.RtcUiIncomingCall
import com.infobip.webrtc.ui.internal.model.RtcUiIncomingWebrtcCallImpl

internal interface CallsDelegate {
    fun accept()
    fun decline()
    fun hangup()
    fun remoteVideos(): Map<String, RemoteVideo>?
    fun screenShareTrack(): RTCVideoTrack?
    fun remoteVideoTrack(): RTCVideoTrack?
    fun localVideoTrack(): RTCVideoTrack?
    fun setEventListener(listener: RtcUiCallEventListener)
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
    fun handleIncomingCall(data: Map<String, String>): Boolean
    fun registerActiveConnection(token: String)
    fun enablePush(token: String, configId: String, onResult: (EnablePushNotificationResult) -> Unit)
    fun setNetworkQualityListener(networkQualityEventListener: NetworkQualityEventListener)
}

internal class CallsDelegateImpl(
    private val context: Context,
    private val infobipRtc: InfobipRTC
) : CallsDelegate {
    private val call: RtcUiCall?
        get() = getActiveCall(
            { if (this is IncomingWebrtcCall) RtcUiIncomingWebrtcCallImpl(this) else null },
            { if (this is IncomingApplicationCall) RtcUiIncomingAppCallImpl(this) else null }
        )

    private inline fun <OUT : RtcUiCall?> getActiveCall(
        callBlock: WebrtcCall.() -> OUT,
        appCallBlock: ApplicationCall.() -> OUT,
    ): OUT? {
        return (infobipRtc.activeCall as? WebrtcCall)?.callBlock()
            ?: infobipRtc.activeApplicationCall?.appCallBlock()
    }

    override fun accept() {
        (call as? RtcUiIncomingCall)?.accept()
    }

    override fun decline() {
        (call as? RtcUiIncomingCall)?.decline()
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
        return call?.localVideoTrack()
    }

    override fun setEventListener(listener: RtcUiCallEventListener) {
        call?.setEventListener(listener)
    }

    override fun duration(): Int = call?.duration() ?: 0

    override fun flipCamera() {
        if (call?.hasLocalVideo() == true) {
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
        call?.localVideo(enabled)
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
                CallState(
                    isEstablished = call.status()?.let { it == CallStatus.ESTABLISHED } == true || call.establishTime() != null || call.duration() > 0,
                    isMuted = call.muted(),
                    isPeerMuted = (call as? RtcUiAppCall)?.participants()?.firstOrNull()?.media?.audio?.muted,
                    elapsedTimeSeconds = call.duration(),
                    isSpeakerOn = call.speakerphone(),
                    isLocalScreenShare = call.hasScreenShare(),
                    callAlert = null,
                    isPip = false,
                    isFinished = call.status()?.let { it == CallStatus.FINISHED || it == CallStatus.FINISHING } == true || call.endTime() != null,
                    showControls = true,
                    error = null,
                    localVideoTrack = localVideoTrack(),
                    remoteVideoTrack = remoteVideoTrack(),
                    screenShareTrack = screenShareTrack()
                )
            }.getOrNull()
        }
    }

    override fun getCallStatus(): CallStatus {
        return call?.status() ?: CallStatus.FINISHED
    }

    override fun handleIncomingCall(data: Map<String, String>): Boolean {
        Log.d(TAG, "Incoming call push message received $data")
        var handled = false
        if (infobipRtc.isIncomingCall(data)) {
            infobipRtc.handleIncomingCall(data, context, IncomingCallEventListenerImpl(context, data))
            handled = true
        } else if (infobipRtc.isIncomingApplicationCall(data)) {
            infobipRtc.handleIncomingApplicationCall(data, context, IncomingCallEventListenerImpl(context, data))
            handled = true
        }
        return handled
    }

    override fun registerActiveConnection(token: String) {
        infobipRtc.registerForActiveConnection(
            token,
            context,
            IncomingCallEventListenerImpl(context, mapOf()) as IncomingApplicationCallEventListener
        )
        infobipRtc.registerForActiveConnection(
            token,
            context,
            IncomingCallEventListenerImpl(context, mapOf()) as IncomingCallEventListener
        )
    }

    override fun enablePush(token: String, configId: String, onResult: (EnablePushNotificationResult) -> Unit) {
        infobipRtc.enablePushNotification(token, context, configId) {
            onResult(it)
        }
    }

    override fun setNetworkQualityListener(networkQualityEventListener: NetworkQualityEventListener) {
        call?.setNetworkQualityListener(networkQualityEventListener)
    }
}