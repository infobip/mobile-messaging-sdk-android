/*
 * CallViewModel.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.ui

import android.content.res.Configuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infobip.webrtc.sdk.api.event.network.NetworkQualityChangedEvent
import com.infobip.webrtc.sdk.api.model.CallStatus
import com.infobip.webrtc.sdk.api.model.network.NetworkQuality
import com.infobip.webrtc.sdk.api.model.video.RTCVideoTrack
import com.infobip.webrtc.sdk.api.model.video.ScreenCapturer
import com.infobip.webrtc.sdk.impl.event.listener.DefaultNetworkQualityEventListener
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.listener.RtcUiCallEventListener
import com.infobip.webrtc.ui.internal.model.CallState
import com.infobip.webrtc.ui.internal.ui.view.CallAlert
import com.infobip.webrtc.ui.logging.RtcUiLogger
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
            isEstablished = false,
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
        updateState { copy(isEstablished = true) }
    }

    fun decline(error: String = "") {
        updateState { copy(isFinished = true, error = error) }
        callsDelegate.decline()
    }

    fun hangup(error: String = "") {
        updateState { copy(isFinished = true, error = error) }
        callsDelegate.hangup()
    }

    fun endCall(error: String = "") {
        if (isEstablished()) {
            hangup(error)
        } else {
            decline(error)
        }
    }

    fun onError(message: String?) {
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

    fun cleanError() {
        updateState { copy(error = null) }
    }

    fun formatTime(durationSeconds: Int): String {
        return timeFormatter.format(LocalTime.ofSecondOfDay(durationSeconds.toLong()))
    }

    fun shareScreen(screenCapturer: ScreenCapturer): Result<Unit> {
        val result = runCatching {
            callsDelegate.shareScreen(screenCapturer)
            updateState { copy(isLocalScreenShare = true) }
        }
        result.onFailure {
            RtcUiLogger.e("Action start screen share failed.", throwable = it)
        }
        return result
    }

    fun stopScreenShare() {
        runCatching {
            updateState { copy(isLocalScreenShare = false) }
            callsDelegate.stopScreenShare()
        }.onFailure {
            RtcUiLogger.e("Action stop screen share failed.", throwable = it)
        }
    }

    fun toggleVideo() {
        runCatching {
            val newValue = !state.value.isLocalVideo
            callsDelegate.cameraVideo(newValue)
        }.onFailure {
            RtcUiLogger.e("Action camera failed.", throwable = it)
        }
    }

    fun toggleMic() {
        runCatching {
            val newValue = !state.value.isMuted
            callsDelegate.mute(newValue)
            updateState { copy(isMuted = newValue) }
        }.onFailure {
            RtcUiLogger.e("Action mute failed.", throwable = it)
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

    fun isEstablished(): Boolean {
        return state.value.isEstablished
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
