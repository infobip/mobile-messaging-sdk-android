package com.infobip.webrtc.ui

import android.content.res.Configuration
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infobip.webrtc.Injector
import com.infobip.webrtc.TAG
import com.infobip.webrtc.sdk.api.call.CallStatus
import com.infobip.webrtc.sdk.api.conference.RemoteVideo
import com.infobip.webrtc.sdk.api.video.RTCVideoTrack
import com.infobip.webrtc.sdk.api.video.ScreenCapturer
import com.infobip.webrtc.sdk.impl.event.DefaultApplicationCallEventListener
import com.infobip.webrtc.ui.model.CallState
import kotlinx.coroutines.flow.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CallViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        CallState(
            isIncoming = true,
            isMuted = false,
            isPeerMuted = false,
            elapsedTimeSeconds = 0,
            isSpeakerOn = false,
            isScreenShare = false,
            isWeakConnection = false,
            isPip = false,
            isFinished = false,
            showControls = true,
            localVideoTrack = null,
            remoteVideoTrack = null,
            screenShareTrack = null
        )
    )
    val state = _state.asStateFlow()

    var peerName: String = ""
    var orientation: Int = Configuration.ORIENTATION_UNDEFINED

    private val callsDelegate by lazy { Injector.callsDelegate }

    val remoteVideoTrack: StateFlow<RTCVideoTrack?> = state
        .map { it.screenShareTrack ?: it.remoteVideoTrack }
        .stateIn(viewModelScope, SharingStarted.Eagerly, callsDelegate.screenShareTrack() ?: callsDelegate.remoteVideoTrack())

    val localVideoTrack: StateFlow<RTCVideoTrack?> = state
        .map { it.localVideoTrack }
        .stateIn(viewModelScope, SharingStarted.Eagerly, callsDelegate.localVideoTrack())

    private val timeFormatter = DateTimeFormatter.ofPattern("mm:ss")

    fun init() {
        callsDelegate.getCallState()?.let { _state.value = it }
    }

    fun updateState(update: CallState.() -> CallState) {
        _state.update { update(_state.value) }
    }

    fun accept() {
        callsDelegate.accept()
        updateState { copy(isIncoming = false) }
    }

    fun decline() {
        callsDelegate.decline()
        updateState { copy(isFinished = true) }
    }

    fun endCall() {
        if (state.value.isIncoming) {
            decline()
        } else {
            hangup()
        }
    }

    fun onError(message: String) {
        updateState {
            copy(
                error = message,
                isFinished = callsDelegate.getCallStatus() in arrayOf(
                    CallStatus.FINISHED,
                    CallStatus.FINISHING
                )
            )
        }
    }

    fun formatTime(durationSeconds: Int): String {
        return timeFormatter.format(LocalTime.ofSecondOfDay(durationSeconds.toLong()))
    }

    fun shareScreen(screenCapturer: ScreenCapturer) {
        runCatching {
            callsDelegate.shareScreen(screenCapturer)
            updateState { copy(isScreenShare = true) }
        }.onFailure {
            Log.e(TAG, "Action start screen share failed.", it)
        }
    }

    fun stopScreenShare() {
        runCatching {
            callsDelegate.stopScreenShare()
            updateState { copy(isScreenShare = false) }
        }.onFailure {
            Log.e(TAG, "Action stop screen share failed.", it)
        }
    }

    fun hangup() {
        callsDelegate.hangup()
        updateState { copy(isFinished = true) }
    }

    fun toggleVideo() {
        runCatching {
            val newValue = !state.value.isLocalVideo
            callsDelegate.cameraVideo(newValue)
        }.onFailure {
            Log.e(TAG, "Action camera failed.", it)
        }
    }

    fun toggleMute() {
        runCatching {
            val newValue = !state.value.isMuted
            callsDelegate.mute(newValue)
            updateState { copy(isMuted = newValue) }
        }.onFailure {
            Log.e(TAG, "Action mute failed.", it)
        }
    }

    fun toggleSpeaker() {
        val newValue = !state.value.isSpeakerOn
        callsDelegate.speaker(newValue)
        updateState { copy(isSpeakerOn = newValue) }
    }

    fun toggleControlsVisibility() {
        if (state.value.isRemoteVideo)
            updateState { copy(showControls = !showControls) }
    }

    fun flipCamera() {
        callsDelegate.flipCamera()
    }

    fun setEventListener(applicationCallEventListener: DefaultApplicationCallEventListener) {
        callsDelegate.setEventListener(applicationCallEventListener)
    }

    fun callDuration(): Int {
        return callsDelegate.duration()
    }

    fun isIncomingCall(): Boolean {
        return state.value.isIncoming
    }

    fun getRemoteVideos(): List<RemoteVideo>? = callsDelegate.remoteVideos()?.map { it.value }

    fun getLocalVideo(): RTCVideoTrack? = callsDelegate.localVideoTrack()
}