package com.infobip.webrtc.ui.model

import android.content.Context
import android.util.Log
import com.infobip.webrtc.sdk.api.call.IncomingWebrtcCall
import com.infobip.webrtc.sdk.api.call.WebrtcCall
import com.infobip.webrtc.sdk.api.event.listener.NetworkQualityEventListener
import com.infobip.webrtc.sdk.api.model.CallStatus
import com.infobip.webrtc.sdk.api.model.RemoteVideo
import com.infobip.webrtc.sdk.api.model.video.RTCVideoTrack
import com.infobip.webrtc.sdk.api.model.video.ScreenCapturer
import com.infobip.webrtc.sdk.api.options.VideoOptions
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.applyIf
import com.infobip.webrtc.ui.listeners.RtcUiCallEventListener
import com.infobip.webrtc.ui.listeners.toWebRtcCallEventListener
import java.util.*

interface RtcUiWebrtcCall : RtcUiCall

abstract class BaseRtcUiWebrtcCall(
    private val activeCall: WebrtcCall
) : RtcUiWebrtcCall {

    override fun id(): String? = activeCall.id()
    override fun callOptions(): RtcUiCallOptions? = activeCall.options()?.run { RtcUiCallOptions(isAudio, isVideo, null, videoOptions, customData) }
    override fun updateCustomData(customData: Map<String, String>) {
        activeCall.options()?.customData?.applyIf({ isEmpty() }, { putAll(customData) })
    }

    override fun status(): CallStatus? = activeCall.status()
    override fun duration(): Int = activeCall.duration()
    override fun startTime(): Date? = activeCall.startTime()
    override fun endTime(): Date? = activeCall.endTime()
    override fun establishTime(): Date? = activeCall.establishTime()

    override fun isVideoCall(): Boolean = activeCall.options()?.isVideo ?: false
    override fun hasRemoteVideo(): Boolean = activeCall.hasRemoteCameraVideo()
    override fun remoteVideos(): Map<String, RemoteVideo> {
        return mapOf(
            activeCall.id() to RemoteVideo(activeCall.remoteCameraTrack(), activeCall.remoteScreenShareTrack())
        )
    }

    override fun firstRemoteVideoTrack(type: RtcUiCallVideoTrackType): RTCVideoTrack? = remoteVideos()
        .values.firstNotNullOfOrNull {
            when (type) {
                RtcUiCallVideoTrackType.CAMERA -> it.camera
                RtcUiCallVideoTrackType.SCREEN_SHARE -> it.screenShare
                RtcUiCallVideoTrackType.ANY_AVAILABLE -> it.camera ?: it.screenShare
            }
        }

    override fun hasLocalVideo(): Boolean = activeCall.hasCameraVideo()
    override fun localVideo(enabled: Boolean) = activeCall.cameraVideo(enabled)
    override fun localVideoTrack(): RTCVideoTrack? = activeCall.localCameraTrack()

    override fun hasScreenShare(): Boolean = activeCall.hasScreenShare()
    override fun startScreenShare(screenCapturer: ScreenCapturer) = activeCall.startScreenShare(screenCapturer)
    override fun stopScreenShare() = activeCall.stopScreenShare()

    override fun hangup() = activeCall.hangup()

    override fun mute(shouldMute: Boolean) = activeCall.mute(shouldMute)
    override fun muted(): Boolean = activeCall.muted()

    override fun speakerphone(enabled: Boolean) = activeCall.speakerphone(enabled)
    override fun speakerphone(): Boolean = runCatching { activeCall.speakerphone() }.getOrElse { false }

    override fun sendDTMF(dtmf: String) {
        runCatching {
            activeCall.sendDTMF(dtmf)
        }.onFailure {
            Log.e("MMWebrtcCall", "sendDTMF($dtmf) failed", it)
        }
    }

    override fun cameraOrientation(): VideoOptions.CameraOrientation? = activeCall.cameraOrientation()
    override fun cameraOrientation(cameraOrientation: VideoOptions.CameraOrientation) = activeCall.cameraOrientation(cameraOrientation)

    override fun setEventListener(eventListener: RtcUiCallEventListener?) {
        activeCall.eventListener = eventListener?.toWebRtcCallEventListener()
    }

    override fun setNetworkQualityListener(networkQualityListener: NetworkQualityEventListener?) {
        activeCall.networkQualityEventListener = networkQualityListener
    }
}

interface RtcUiIncomingWebRtcCall : RtcUiIncomingCall

class RtcUiIncomingWebrtcCallImpl(private val activeCall: IncomingWebrtcCall) : BaseRtcUiWebrtcCall(activeCall), RtcUiIncomingWebRtcCall {

    override fun peer(context: Context): String {
        return activeCall.source()?.displayIdentifier()?.takeIf { it.isNotBlank() }
            ?: activeCall.source()?.identifier()?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.mm_unknown)
    }

    override fun accept(callOptions: RtcUiCallOptions?) {
        if (callOptions != null)
            activeCall.accept(callOptions.toWebRtcCallOptions())
        else
            activeCall.accept()
    }

    override fun decline() {
        activeCall.decline()
    }

}
