package com.infobip.webrtc.ui.model

import android.content.Context
import android.util.Log
import com.infobip.webrtc.sdk.api.call.ApplicationCall
import com.infobip.webrtc.sdk.api.call.IncomingApplicationCall
import com.infobip.webrtc.sdk.api.event.listener.NetworkQualityEventListener
import com.infobip.webrtc.sdk.api.exception.ActionFailedException
import com.infobip.webrtc.sdk.api.model.CallStatus
import com.infobip.webrtc.sdk.api.model.RemoteVideo
import com.infobip.webrtc.sdk.api.model.participant.Participant
import com.infobip.webrtc.sdk.api.model.video.RTCVideoTrack
import com.infobip.webrtc.sdk.api.model.video.ScreenCapturer
import com.infobip.webrtc.sdk.api.options.VideoOptions
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.applyIf
import com.infobip.webrtc.ui.listeners.RtcUiCallEventListener
import com.infobip.webrtc.ui.listeners.toAppCallEventListener
import java.util.*

/**
 * Represents WebRTC 2.0 model - Application Call
 */
interface RtcUiAppCall : RtcUiCall {
    fun applicationId(): String?
    fun participants(): List<Participant>?
}

abstract class BaseRtcUiAppCall(
    private val activeCall: ApplicationCall
) : RtcUiAppCall {

    override fun id(): String? = activeCall.id()
    override fun applicationId(): String? = activeCall.applicationId()
    override fun callOptions(): RtcUiCallOptions? = activeCall.options().run { RtcUiCallOptions(isAudio, isVideo, audioOptions, videoOptions, customData) }
    override fun updateCustomData(customData: Map<String, String>) {
        activeCall.options().customData.applyIf({ isEmpty() }, { putAll(customData) })
    }

    override fun status(): CallStatus? = activeCall.status()
    override fun duration(): Int = activeCall.duration()
    override fun startTime(): Date? = activeCall.startTime()
    override fun endTime(): Date? = activeCall.endTime()
    override fun establishTime(): Date? = activeCall.establishTime()

    override fun isVideoCall(): Boolean = activeCall.options().customData["isVideo"]?.toBoolean() ?: false
    override fun hasRemoteVideo(): Boolean = remoteVideos().isNotEmpty()
    override fun remoteVideos(): Map<String, RemoteVideo> = activeCall.remoteVideos()
    override fun firstRemoteVideoTrack(type: RtcUiCallVideoTrackType): RTCVideoTrack? {
        return remoteVideos()
            .values
            .reversed()
            .firstNotNullOfOrNull {
                when (type) {
                    RtcUiCallVideoTrackType.CAMERA -> it.camera
                    RtcUiCallVideoTrackType.SCREEN_SHARE -> it.screenShare
                    RtcUiCallVideoTrackType.ANY_AVAILABLE -> it.camera ?: it.screenShare
                }
            }
    }

    override fun hasLocalVideo(): Boolean = activeCall.hasCameraVideo()
    override fun localVideo(enabled: Boolean) = activeCall.cameraVideo(enabled)
    override fun localVideoTrack(): RTCVideoTrack? = activeCall.localCameraTrack()

    override fun hasScreenShare(): Boolean = activeCall.hasScreenShare()
    override fun startScreenShare(screenCapturer: ScreenCapturer) = activeCall.startScreenShare(screenCapturer)
    override fun stopScreenShare() = activeCall.stopScreenShare()

    override fun hangup() = activeCall.hangup()

    @Throws(ActionFailedException::class)
    override fun mute(shouldMute: Boolean) = activeCall.mute(shouldMute)
    override fun muted(): Boolean = runCatching { activeCall.muted() }.getOrElse { false }

    override fun speakerphone(enabled: Boolean) = activeCall.speakerphone(enabled)
    override fun speakerphone(): Boolean = runCatching { activeCall.speakerphone() }.getOrElse { false }

    override fun sendDTMF(dtmf: String) {
        runCatching {
            activeCall.sendDTMF(dtmf)
        }.onFailure {
            Log.e("MMAppCall", "sendDTMF($dtmf) failed", it)
        }
    }

    override fun cameraOrientation(): VideoOptions.CameraOrientation? = activeCall.cameraOrientation()
    override fun cameraOrientation(cameraOrientation: VideoOptions.CameraOrientation) = activeCall.cameraOrientation(cameraOrientation)

    override fun setEventListener(eventListener: RtcUiCallEventListener?) {
        activeCall.eventListener = eventListener?.toAppCallEventListener()
    }

    override fun setNetworkQualityListener(networkQualityListener: NetworkQualityEventListener?) {
        activeCall.networkQualityEventListener = networkQualityListener
    }

    override fun participants(): List<Participant>? = activeCall.participants()
}

interface RtcUiIncomingAppCall : RtcUiIncomingCall

class RtcUiIncomingAppCallImpl(private val activeCall: IncomingApplicationCall) : BaseRtcUiAppCall(activeCall), RtcUiIncomingAppCall {

    override fun peer(context: Context): String {
        return activeCall.fromDisplayName()?.takeIf { it.isNotBlank() }
            ?: activeCall.from()?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.mm_unknown)
    }

    override fun accept(callOptions: RtcUiCallOptions?) {
        if (callOptions != null)
            activeCall.accept(callOptions.toApplicationCallOptions())
        else
            activeCall.accept()
    }

    override fun decline() {
        activeCall.decline()
    }
}
