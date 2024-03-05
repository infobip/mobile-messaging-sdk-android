package com.infobip.webrtc.ui.internal.model

import com.infobip.webrtc.sdk.api.model.video.RTCVideoTrack
import com.infobip.webrtc.ui.internal.ui.view.CallAlert

internal data class CallState(
    val isEstablished: Boolean,
    val isMuted: Boolean,
    val isPeerMuted: Boolean?,
    val elapsedTimeSeconds: Int,
    val isSpeakerOn: Boolean,
    val isLocalScreenShare: Boolean,
    val callAlert: CallAlert.Mode? = null,
    val isPip: Boolean,
    val isFinished: Boolean,
    val showControls: Boolean,
    val error: String? = null,
    val localVideoTrack: RTCVideoTrack? = null,
    val remoteVideoTrack: RTCVideoTrack? = null,
    val screenShareTrack: RTCVideoTrack? = null
) {
    val isRemoteVideo: Boolean = remoteVideoTrack != null
    val isLocalVideo: Boolean = localVideoTrack != null
    val isRemoteScreenShare: Boolean = screenShareTrack != null
}