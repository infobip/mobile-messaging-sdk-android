/*
 * RtcUiCall.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.model

import android.content.Context
import com.infobip.webrtc.sdk.api.call.DataChannel
import com.infobip.webrtc.sdk.api.device.AudioDeviceManager
import com.infobip.webrtc.sdk.api.event.listener.NetworkQualityEventListener
import com.infobip.webrtc.sdk.api.event.listener.ParticipantNetworkQualityEventListener
import com.infobip.webrtc.sdk.api.exception.ActionFailedException
import com.infobip.webrtc.sdk.api.model.CallStatus
import com.infobip.webrtc.sdk.api.model.RemoteVideo
import com.infobip.webrtc.sdk.api.model.video.RTCVideoTrack
import com.infobip.webrtc.sdk.api.model.video.ScreenCapturer
import com.infobip.webrtc.sdk.api.options.VideoOptions
import com.infobip.webrtc.ui.internal.listener.RtcUiCallEventListener
import java.util.*

internal interface RtcUiCall {
    fun id(): String?
    fun callOptions(): RtcUiCallOptions?
    fun updateCustomData(customData: Map<String, String>)
    fun status(): CallStatus?
    fun duration(): Int
    fun startTime(): Date?
    fun establishTime(): Date?
    fun endTime(): Date?

    fun isVideoCall(): Boolean
    fun hasRemoteVideo(): Boolean
    fun remoteVideos(): Map<String, RemoteVideo>
    fun firstRemoteVideoTrack(type: RtcUiCallVideoTrackType): RTCVideoTrack?

    @Throws(ActionFailedException::class)
    fun pauseIncomingVideo()
    @Throws(ActionFailedException::class)
    fun resumeIncomingVideo()

    fun hasLocalVideo(): Boolean
    @Throws(ActionFailedException::class)
    fun localVideo(enabled: Boolean)
    fun localVideoTrack(): RTCVideoTrack?

    fun hasScreenShare(): Boolean
    @Throws(ActionFailedException::class)
    fun startScreenShare(screenCapturer: ScreenCapturer)
    @Throws(ActionFailedException::class)
    fun stopScreenShare()
    fun resetScreenShare()
    fun localScreenShareTrack(): RTCVideoTrack?
    fun remoteScreenShareTrack(): RTCVideoTrack?

    fun hangup()

    @Throws(ActionFailedException::class)
    fun mute(shouldMute: Boolean)
    fun muted(): Boolean

    fun speakerphone(enabled: Boolean)
    fun speakerphone(): Boolean

    fun sendDTMF(dtmf: String)

    fun cameraOrientation(cameraOrientation: VideoOptions.CameraOrientation)
    fun cameraOrientation(): VideoOptions.CameraOrientation?

    fun setEventListener(eventListener: RtcUiCallEventListener?)
    fun setNetworkQualityListener(networkQualityListener: NetworkQualityEventListener?)
    fun setParticipantNetworkQualityEventListener(participantNetworkQualityEventListener: ParticipantNetworkQualityEventListener?)

    fun audioDeviceManager(): AudioDeviceManager?
    fun dataChannel(): DataChannel?
}

internal interface RtcUiIncomingCall : RtcUiCall {
    fun peer(context: Context): String
    fun accept(callOptions: RtcUiCallOptions? = null)
    fun decline()
}