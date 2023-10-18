package com.infobip.webrtc.ui

import android.content.res.Configuration
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infobip.webrtc.Injector
import com.infobip.webrtc.TAG
import com.infobip.webrtc.sdk.api.event.network.NetworkQualityChangedEvent
import com.infobip.webrtc.sdk.api.model.CallStatus
import com.infobip.webrtc.sdk.api.model.network.NetworkQuality
import com.infobip.webrtc.sdk.api.model.video.RTCVideoTrack
import com.infobip.webrtc.sdk.api.model.video.ScreenCapturer
import com.infobip.webrtc.sdk.impl.event.listener.DefaultNetworkQualityEventListener
import com.infobip.webrtc.ui.listeners.RtcUiCallEventListener
import com.infobip.webrtc.ui.model.CallState
import com.infobip.webrtc.ui.view.CallAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal class CallViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        CallState(
            isIncoming = true,
            isMuted = false,
            isPeerMuted = false,
            elapsedTimeSeconds = 0,
            isSpeakerOn = false,
            isLocalScreenShare = false,
            callAlert = null,
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
        .map { it.remoteVideoTrack }
        .stateIn(viewModelScope, SharingStarted.Eagerly, callsDelegate.remoteVideoTrack())

    val screenShareTrack: StateFlow<RTCVideoTrack?> = state
        .map { it.screenShareTrack }
        .stateIn(viewModelScope, SharingStarted.Eagerly, callsDelegate.screenShareTrack())

    val localVideoTrack: StateFlow<RTCVideoTrack?> = state
        .map { it.localVideoTrack }
        .stateIn(viewModelScope, SharingStarted.Eagerly, callsDelegate.localVideoTrack())

    private val _localTrackToBeRemoved = MutableSharedFlow<RTCVideoTrack>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val localTrackToBeRemoved = _localTrackToBeRemoved.asSharedFlow()

    private val _remoteTrackToBeRemoved = MutableSharedFlow<RTCVideoTrack>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val remoteTrackToBeRemoved = _remoteTrackToBeRemoved.asSharedFlow()

    private val _screenShareTrackToBeRemoved = MutableSharedFlow<RTCVideoTrack>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val screenShareTrackToBeRemoved = _screenShareTrackToBeRemoved.asSharedFlow()

    private val timeFormatter = DateTimeFormatter.ofPattern("mm:ss")

    fun init() {
        callsDelegate.getCallState()?.let { _state.value = it }
    }

    fun updateState(update: CallState.() -> CallState) {
        _state.update { update(_state.value) }
    }

    fun accept() {
        callsDelegate.accept()
        setNetworkQualityListener()
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
            updateState { copy(isLocalScreenShare = true) }
        }.onFailure {
            Log.e(TAG, "Action start screen share failed.", it)
        }
    }

    fun stopScreenShare() {
        runCatching {
            callsDelegate.stopScreenShare()
            updateState { copy(isLocalScreenShare = false) }
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
        if (state.value.isRemoteVideo || state.value.isLocalScreenShare || state.value.isRemoteScreenShare)
            updateState { copy(showControls = !showControls) }
    }

    fun flipCamera() {
        callsDelegate.flipCamera()
    }

    fun setEventListener(callEventListener: RtcUiCallEventListener) {
        callsDelegate.setEventListener(callEventListener)
    }

    fun callDuration(): Int {
        return callsDelegate.duration()
    }

    fun isIncomingCall(): Boolean {
        return state.value.isIncoming
    }

    private fun setNetworkQualityListener() {
        callsDelegate.setNetworkQualityListener(object : DefaultNetworkQualityEventListener() {
            override fun onNetworkQualityChanged(networkQualityChangedEvent: NetworkQualityChangedEvent?) {
                if (state.value.isPip)
                    return
                viewModelScope.launch {
                    withContext(Dispatchers.Main) {
                        val score = networkQualityChangedEvent?.networkQuality?.score
                        val isNetworkIssue = score != null && score <= NetworkQuality.FAIR.score
                        updateState { copy(callAlert = CallAlert.Mode.WeakConnection.takeIf { isNetworkIssue }) }
                    }
                }
            }
        })
    }

    fun emitLocalTrackToRemove() {
        state.value.localVideoTrack?.let {
            _localTrackToBeRemoved.tryEmit(it)
        }
    }
    fun emitRemoteTrackToRemove() {
        state.value.remoteVideoTrack?.let {
            _remoteTrackToBeRemoved.tryEmit(it)
        }
    }

    fun emitScreenShareTrackToRemove() {
        state.value.screenShareTrack?.let {
            _screenShareTrackToBeRemoved.tryEmit(it)
        }
    }
}