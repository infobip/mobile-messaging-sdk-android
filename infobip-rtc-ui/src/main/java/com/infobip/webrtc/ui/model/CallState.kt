package com.infobip.webrtc.ui.model

import com.infobip.webrtc.sdk.api.model.video.RTCVideoTrack

internal data class CallState(
    val isIncoming: Boolean,
    val isMuted: Boolean,
    val isPeerMuted: Boolean?,
    val elapsedTimeSeconds: Int,
    val isSpeakerOn: Boolean,
    val isLocalScreenShare: Boolean,
    val isWeakConnection: Boolean,
    val isPip: Boolean,
    val isFinished: Boolean,
    val showControls: Boolean,
    val error: String = "",
    val localVideoTrack: RTCVideoTrack? = null,
    val remoteVideoTrack: RTCVideoTrack? = null,
    val screenShareTrack: RTCVideoTrack? = null
) {
    val isRemoteVideo: Boolean = remoteVideoTrack != null
    val isLocalVideo: Boolean = localVideoTrack != null
    val isRemoteScreenShare: Boolean = screenShareTrack != null
}