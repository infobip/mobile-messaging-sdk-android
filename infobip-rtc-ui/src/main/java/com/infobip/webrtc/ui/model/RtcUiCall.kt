package com.infobip.webrtc.ui.model

import android.content.Context
import com.infobip.webrtc.sdk.api.event.listener.NetworkQualityEventListener
import com.infobip.webrtc.sdk.api.exception.ActionFailedException
import com.infobip.webrtc.sdk.api.model.CallStatus
import com.infobip.webrtc.sdk.api.model.RemoteVideo
import com.infobip.webrtc.sdk.api.model.video.RTCVideoTrack
import com.infobip.webrtc.sdk.api.model.video.ScreenCapturer
import com.infobip.webrtc.sdk.api.options.VideoOptions
import com.infobip.webrtc.ui.listeners.RtcUiCallEventListener
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

    fun hasLocalVideo(): Boolean

    @Throws(ActionFailedException::class)
    fun localVideo(enabled: Boolean)
    fun localVideoTrack(): RTCVideoTrack?

    fun hasScreenShare(): Boolean

    @Throws(ActionFailedException::class)
    fun startScreenShare(screenCapturer: ScreenCapturer)

    @Throws(ActionFailedException::class)
    fun stopScreenShare()

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
}

internal interface RtcUiIncomingCall : RtcUiCall {
    fun peer(context: Context): String
    fun accept(callOptions: RtcUiCallOptions? = null)
    fun decline()
}