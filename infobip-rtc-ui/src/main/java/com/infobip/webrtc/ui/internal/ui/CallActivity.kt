/*
 * CallActivity.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.infobip.webrtc.sdk.api.event.call.CallEarlyMediaEvent
import com.infobip.webrtc.sdk.api.event.call.CallEstablishedEvent
import com.infobip.webrtc.sdk.api.event.call.CallHangupEvent
import com.infobip.webrtc.sdk.api.event.call.CallRecordingStartedEvent
import com.infobip.webrtc.sdk.api.event.call.CallRecordingStoppedEvent
import com.infobip.webrtc.sdk.api.event.call.CallRingingEvent
import com.infobip.webrtc.sdk.api.event.call.CameraVideoAddedEvent
import com.infobip.webrtc.sdk.api.event.call.CameraVideoRemovedEvent
import com.infobip.webrtc.sdk.api.event.call.CameraVideoUpdatedEvent
import com.infobip.webrtc.sdk.api.event.call.ConferenceJoinedEvent
import com.infobip.webrtc.sdk.api.event.call.ConferenceLeftEvent
import com.infobip.webrtc.sdk.api.event.call.ConferenceRecordingStartedEvent
import com.infobip.webrtc.sdk.api.event.call.ConferenceRecordingStoppedEvent
import com.infobip.webrtc.sdk.api.event.call.DialogJoinedEvent
import com.infobip.webrtc.sdk.api.event.call.DialogLeftEvent
import com.infobip.webrtc.sdk.api.event.call.DialogRecordingStartedEvent
import com.infobip.webrtc.sdk.api.event.call.DialogRecordingStoppedEvent
import com.infobip.webrtc.sdk.api.event.call.MachineDetectionFailedEvent
import com.infobip.webrtc.sdk.api.event.call.MachineDetectionFinishedEvent
import com.infobip.webrtc.sdk.api.event.call.MessageReceivedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantBlindedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantCameraVideoAddedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantCameraVideoRemovedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantDeafEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantDisconnectedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantJoinedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantJoiningEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantLeftEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantMutedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantReconnectedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantRoleChangedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantScreenShareAddedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantScreenShareRemovedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantStartedTalkingEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantStoppedTalkingEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantUnblindedEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantUndeafEvent
import com.infobip.webrtc.sdk.api.event.call.ParticipantUnmutedEvent
import com.infobip.webrtc.sdk.api.event.call.ReconnectedEvent
import com.infobip.webrtc.sdk.api.event.call.ReconnectingEvent
import com.infobip.webrtc.sdk.api.event.call.RoleChangedEvent
import com.infobip.webrtc.sdk.api.event.call.ScreenShareAddedEvent
import com.infobip.webrtc.sdk.api.event.call.ScreenShareRemovedEvent
import com.infobip.webrtc.sdk.api.event.call.StartedTalkingEvent
import com.infobip.webrtc.sdk.api.event.call.StoppedTalkingEvent
import com.infobip.webrtc.sdk.api.event.call.TalkingWhileMutedEvent
import com.infobip.webrtc.sdk.api.model.ErrorCode
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.RtcUiCallErrorMapper
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.core.RtcUiCallErrorMapperFactory
import com.infobip.webrtc.ui.internal.delegate.PhoneStateDelegate
import com.infobip.webrtc.ui.internal.delegate.PhoneStateDelegateFactory
import com.infobip.webrtc.ui.internal.listener.DefaultRtcUiCallEventListener
import com.infobip.webrtc.ui.internal.model.CallAction
import com.infobip.webrtc.ui.internal.service.ActiveCallService
import com.infobip.webrtc.ui.internal.ui.fragment.InCallFragment
import com.infobip.webrtc.ui.internal.ui.fragment.IncomingCallFragment
import com.infobip.webrtc.ui.internal.ui.view.CallAlert
import com.infobip.webrtc.ui.internal.utils.applyLocale
import com.infobip.webrtc.ui.internal.utils.applyWindowInsets
import com.infobip.webrtc.ui.internal.utils.navigate
import com.infobip.webrtc.ui.internal.utils.setStatusBarColor
import com.infobip.webrtc.ui.internal.utils.throttleFirst
import com.infobip.webrtc.ui.logging.RtcUiLogger
import com.infobip.webrtc.ui.model.RtcUiError
import com.infobip.webrtc.ui.view.styles.Colors
import com.infobip.webrtc.ui.view.styles.Icons
import com.infobip.webrtc.ui.view.styles.InCallScreenStyle
import com.infobip.webrtc.ui.view.styles.IncomingCallScreenStyle
import com.infobip.webrtc.ui.view.styles.InfobipRtcUiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class CallActivity : AppCompatActivity(R.layout.activity_call) {

    companion object {
        private const val CALLER_EXTRA_KEY = "CALLER_EXTRA_KEY"
        private const val ACCEPT_EXTRA_KEY = "ACCEPT_EXTRA_KEY"

        fun startIntent(context: Context, caller: String, accept: Boolean = false): Intent {
            return Intent(context, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(CALLER_EXTRA_KEY, caller)
                putExtra(ACCEPT_EXTRA_KEY, accept)
            }
        }
    }

    private val viewModel: CallViewModel by viewModels()
    private val phoneStateDelegate: PhoneStateDelegate by lazy { PhoneStateDelegateFactory.getPhoneStateDelegate(this@CallActivity) }
    private val errorMapper: RtcUiCallErrorMapper by lazy { RtcUiCallErrorMapperFactory.create(this@CallActivity) }
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result: Map<String, Boolean> ->
            if (result[Manifest.permission.RECORD_AUDIO] == false) {
                /**
                 * Starting in Android 11 (API level 30), if the user taps Deny for a specific permission more than once
                 * during your app's lifetime of installation on a device, the user doesn't see the system permissions
                 * dialog if your app requests that permission again.
                 * https://developer.android.com/training/permissions/requesting#handle-denial
                 */
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    showRationale(Manifest.permission.RECORD_AUDIO) //ask permission again or hangup call
                } else {
                    val errorMsg = "${getString(R.string.mm_audio_permission_required)} ${getString(R.string.mm_call_finished)}."
                    viewModel.endCall(errorMsg)
                }
            }
        }

    override fun attachBaseContext(newBase: Context) {
        val newContext = Injector.cache.locale?.let { newBase.applyLocale(it) } ?: newBase
        super.attachBaseContext(newContext)
    }

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        Injector.cache.theme = InfobipRtcUiTheme(
            incomingCallScreenStyle = Injector.cache.incomingCallScreenStyle ?: IncomingCallScreenStyle(this, attrs),
            inCallScreenStyle = Injector.cache.inCallScreenStyle ?: InCallScreenStyle(this, attrs),
            colors = Injector.cache.colors ?: Colors(this, attrs),
            icons = Injector.cache.icons ?: Icons(this, attrs),
        )
        setStatusBarColor(Injector.cache.colors?.background)
        return super.onCreateView(parent, name, context, attrs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyWindowInsets()
        viewModel.orientation = resources.configuration.orientation
        showWhenLocked()
        viewModel.init()
        listenCallEvents()
        applyArguments()
        showFragment()
        subscribeCallState()
        requestPermissions()
    }

    private fun subscribeCallState() {
        val messages = Channel<String>(Channel.BUFFERED)

        messages.consumeAsFlow()
            .filter { it.isNotBlank() }
            .throttleFirst(3500) //duration of Toast.LENGTH_LONG
            .onEach { Toast.makeText(this@CallActivity, it, Toast.LENGTH_LONG).show() }
            .flowOn(Dispatchers.Main)
            .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
            .catch { RtcUiLogger.e("Call messages flow error.", throwable = it) }
            .launchIn(lifecycleScope)

        viewModel.state
            .onEach { state ->
                val message = state.error?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.mm_call_finished).takeIf { state.isFinished && phoneStateDelegate.getState() != 2 } //To not replace toast from PhoneStateBroadcastReceiver
                message?.let {
                    messages.trySend(message)
                    viewModel.cleanError()
                }
                if (state.isFinished)
                    finishAndHideNotifications()
            }
            .flowOn(Dispatchers.Main)
            .flowWithLifecycle(lifecycle)
            .catch { RtcUiLogger.e("Call state flow error.", throwable = it) }
            .launchIn(lifecycleScope)
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permission = Manifest.permission.RECORD_AUDIO
            when {
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> RtcUiLogger.d("$permission permission granted")
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> showRationale(permission)
                else -> permissionsToRequest.add(permission)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val btPermission = Manifest.permission.BLUETOOTH_CONNECT
            when {
                ContextCompat.checkSelfPermission(this, btPermission) == PackageManager.PERMISSION_GRANTED -> RtcUiLogger.d("$btPermission permission granted")
                ActivityCompat.shouldShowRequestPermissionRationale(this, btPermission) -> showRationale(btPermission)
                else -> permissionsToRequest.add(btPermission)
            }
            val phoneStatePermission = Manifest.permission.READ_PHONE_STATE
            when {
                ContextCompat.checkSelfPermission(this, phoneStatePermission) == PackageManager.PERMISSION_GRANTED -> RtcUiLogger.d("$phoneStatePermission permission granted")
                ActivityCompat.shouldShowRequestPermissionRationale(this, phoneStatePermission) -> showRationale(phoneStatePermission)
                else -> permissionsToRequest.add(phoneStatePermission)
            }
        }
        if (permissionsToRequest.isNotEmpty())
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private fun showRationale(permission: String) {
        val messageRes = when (permission) {
            Manifest.permission.RECORD_AUDIO -> R.string.mm_audio_permission_required
            Manifest.permission.BLUETOOTH_CONNECT -> R.string.mm_bt_connect_permission_required
            Manifest.permission.READ_PHONE_STATE -> R.string.mm_read_phone_state_permission_required
            else -> null
        }

        if (messageRes != null) {
            AlertDialog.Builder(this)
                .setMessage(messageRes)
                .setCancelable(false)
                .setPositiveButton(R.string.mm_ok) { _, _ ->
                    requestPermissionLauncher.launch(arrayOf(permission))
                }.setNegativeButton(R.string.mm_cancel) { _, _ ->
                    if (permission == Manifest.permission.RECORD_AUDIO) {
                        viewModel.endCall()
                    }
                }.show()
        }
    }

    private fun showFragment() {
        if (viewModel.isEstablished()) {
            supportFragmentManager.navigate(InCallFragment(), R.id.navHost)
        } else {
            supportFragmentManager.navigate(IncomingCallFragment(), R.id.navHost)
        }
    }

    private fun applyArguments() {
        val peer = intent.getStringExtra(CALLER_EXTRA_KEY) ?: getString(R.string.mm_unknown)
        val accept = intent.getBooleanExtra(ACCEPT_EXTRA_KEY, false)
        viewModel.peerName = peer

        if (!viewModel.isEstablished()) {
            val action = if (accept) {
                viewModel.accept()
                CallAction.INCOMING_CALL_ACCEPTED
            } else {
                CallAction.SILENT_INCOMING_CALL_START
            }
            ActiveCallService.start(applicationContext, action, peer)
        }
    }

    private fun finishAndHideNotifications() {
        ActiveCallService.start(applicationContext, CallAction.CALL_FINISHED)
        finishAndRemoveTask()
    }

    @Suppress("DEPRECATION")
    private fun showWhenLocked() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
    }

    private fun listenCallEvents() {
        viewModel.setEventListener(object : DefaultRtcUiCallEventListener() {

            //region Call core events
            override fun onRinging(callRingingEvent: CallRingingEvent?) {
                RtcUiLogger.d("onRinging")
                runOnUiThread {
                    ActiveCallService.start(applicationContext, CallAction.CALL_RINGING)
                }
            }

            override fun onEarlyMedia(callEarlyMediaEvent: CallEarlyMediaEvent?) {
                RtcUiLogger.d("onEarlyMedia")
            }

            override fun onEstablished(callEstablishedEvent: CallEstablishedEvent?) {
                RtcUiLogger.d("onEstablished")
                viewModel.updateState { copy(isEstablished = true) }
                runOnUiThread {
                    ActiveCallService.start(applicationContext, CallAction.CALL_ESTABLISHED)
                }
            }

            override fun onHangup(callHangupEvent: CallHangupEvent?) {
                RtcUiLogger.d("onHangup")
                val message = errorMapper.getMessageForError(RtcUiError(callHangupEvent?.errorCode ?: ErrorCode.UNKNOWN))
                viewModel.updateState { copy(isFinished = true, error = message) }
            }

            override fun onError(errorCode: ErrorCode?) {
                RtcUiLogger.d("onError")
                val message = errorMapper.getMessageForError(RtcUiError(errorCode ?: ErrorCode.UNKNOWN))
                viewModel.onError(message)
            }

            override fun onRoleChanged(roleChangedEvent: RoleChangedEvent?) {
                RtcUiLogger.d("onRoleChanged")
            }

            override fun onMessageReceived(messageReceivedEvent: MessageReceivedEvent?) {
                RtcUiLogger.d("onMessageReceived")
            }

            override fun onReconnecting(reconnectingEvent: ReconnectingEvent?) {
                RtcUiLogger.d("onReconnecting")
                viewModel.updateState { copy(callAlert = CallAlert.Mode.Reconnecting) }
                runOnUiThread {
                    ActiveCallService.start(applicationContext, CallAction.CALL_RECONNECTING)
                }
            }

            override fun onReconnected(reconnectedEvent: ReconnectedEvent?) {
                RtcUiLogger.d("onReconnected")
                viewModel.updateState { copy(callAlert = null) }
                runOnUiThread {
                    ActiveCallService.start(applicationContext, CallAction.CALL_RECONNECTED)
                }
            }
            //endregion

            //region Call recording
            override fun onCallRecordingStarted(callRecordingStarted: CallRecordingStartedEvent?) {
                RtcUiLogger.d("onCallRecordingStarted")
            }

            override fun onCallRecordingStopped(callRecordingStoppedEvent: CallRecordingStoppedEvent?) {
                RtcUiLogger.d("onCallRecordingStopped")
            }
            //endregion

            //region Local talking
            override fun onTalkingWhileMuted(talkingWhileMutedEvent: TalkingWhileMutedEvent?) {
                RtcUiLogger.d("onTalkingWhileMuted")
            }

            override fun onStartedTalking(startedTalkingEvent: StartedTalkingEvent?) {
                RtcUiLogger.d("onStartedTalking")
            }

            override fun onStoppedTalking(stoppedTalkingEvent: StoppedTalkingEvent?) {
                RtcUiLogger.d("onStoppedTalking")
            }
            //endregion

            //region Local media
            override fun onCameraVideoAdded(cameraVideoAddedEvent: CameraVideoAddedEvent?) {
                RtcUiLogger.d("onCameraVideoAdded")
                viewModel.emitLocalTrackToRemove()
                viewModel.updateState { copy(localVideoTrack = cameraVideoAddedEvent?.track) }
            }

            override fun onCameraVideoUpdated(cameraVideoUpdatedEvent: CameraVideoUpdatedEvent?) {
                RtcUiLogger.d("onCameraVideoUpdated")
                viewModel.emitLocalTrackToRemove()
                viewModel.updateState { copy(localVideoTrack = cameraVideoUpdatedEvent?.track) }
            }

            override fun onCameraVideoRemoved(cameraVideoRemovedEvent: CameraVideoRemovedEvent?) {
                RtcUiLogger.d("onCameraVideoRemoved")
                viewModel.emitLocalTrackToRemove()
                viewModel.updateState { copy(localVideoTrack = null) }
            }

            override fun onScreenShareAdded(screenShareAddedEvent: ScreenShareAddedEvent?) {
                RtcUiLogger.d("onScreenShareAdded")
                viewModel.updateState { copy(isLocalScreenShare = true) }
            }

            override fun onScreenShareRemoved(screenShareRemovedEvent: ScreenShareRemovedEvent?) {
                RtcUiLogger.d("onScreenShareRemoved")
                viewModel.updateState { copy(isLocalScreenShare = false) }
            }
            //endregion

            //region Conference
            override fun onConferenceJoined(conferenceJoinedEvent: ConferenceJoinedEvent?) {
                RtcUiLogger.d("onConferenceJoined")
            }

            override fun onConferenceLeft(conferenceLeftEvent: ConferenceLeftEvent?) {
                RtcUiLogger.d("onConferenceLeft")
            }

            override fun onConferenceRecordingStarted(conferenceRecordingStartedEvent: ConferenceRecordingStartedEvent?) {
                RtcUiLogger.d("onConferenceRecordingStarted")
            }

            override fun onConferenceRecordingStopped(conferenceRecordingStoppedEvent: ConferenceRecordingStoppedEvent?) {
                RtcUiLogger.d("onConferenceRecordingStopped")
            }
            //endregion

            //region Participant
            override fun onParticipantJoining(participantJoiningEvent: ParticipantJoiningEvent?) {
                RtcUiLogger.d("onParticipantJoining")
            }

            override fun onParticipantJoined(participantJoinedEvent: ParticipantJoinedEvent?) {
                RtcUiLogger.d("onParticipantJoined")
            }

            override fun onParticipantLeft(participantLeftEvent: ParticipantLeftEvent?) {
                RtcUiLogger.d("onParticipantLeft")
            }

            override fun onParticipantCameraVideoAdded(participantCameraVideoAddedEvent: ParticipantCameraVideoAddedEvent?) {
                RtcUiLogger.d("onParticipantCameraVideoAdded")
                viewModel.emitRemoteTrackToRemove()
                viewModel.updateState { copy(remoteVideoTrack = participantCameraVideoAddedEvent?.track) }
            }

            override fun onParticipantCameraVideoRemoved(participantCameraVideoRemovedEvent: ParticipantCameraVideoRemovedEvent?) {
                RtcUiLogger.d("onParticipantCameraVideoRemoved")
                viewModel.emitRemoteTrackToRemove()
                viewModel.updateState { copy(remoteVideoTrack = null, showControls = true) }
            }

            override fun onParticipantScreenShareAdded(participantScreenShareAddedEvent: ParticipantScreenShareAddedEvent?) {
                RtcUiLogger.d("onParticipantScreenShareAdded")
                viewModel.emitScreenShareTrackToRemove()
                viewModel.updateState { copy(screenShareTrack = participantScreenShareAddedEvent?.track) }
            }

            override fun onParticipantScreenShareRemoved(participantScreenShareRemovedEvent: ParticipantScreenShareRemovedEvent?) {
                RtcUiLogger.d("onParticipantScreenShareRemoved")
                viewModel.emitScreenShareTrackToRemove()
                viewModel.updateState { copy(screenShareTrack = null, showControls = true) }
            }

            override fun onParticipantMuted(participantMutedEvent: ParticipantMutedEvent?) {
                RtcUiLogger.d("onParticipantMuted")
                viewModel.updateState { copy(isPeerMuted = true) }
            }

            override fun onParticipantUnmuted(participantUnmutedEvent: ParticipantUnmutedEvent?) {
                RtcUiLogger.d("onParticipantUnmuted")
                viewModel.updateState { copy(isPeerMuted = false) }
            }

            override fun onParticipantDeafen(participantDeafEvent: ParticipantDeafEvent?) {
                RtcUiLogger.d("onParticipantDeafen")
            }

            override fun onParticipantUndeafen(participantUndeafEvent: ParticipantUndeafEvent?) {
                RtcUiLogger.d("onParticipantUndeafen")
            }

            override fun onParticipantStartedTalking(participantStartedTalkingEvent: ParticipantStartedTalkingEvent?) {
                RtcUiLogger.d("onParticipantStartedTalking")
            }

            override fun onParticipantStoppedTalking(participantStoppedTalkingEvent: ParticipantStoppedTalkingEvent?) {
                RtcUiLogger.d("onParticipantStoppedTalking")
            }

            override fun onParticipantBlinded(participantBlindedEvent: ParticipantBlindedEvent?) {
                RtcUiLogger.d("onParticipantBlinded")
            }

            override fun onParticipantUnblinded(participantUnblindedEvent: ParticipantUnblindedEvent?) {
                RtcUiLogger.d("onParticipantUnblinded")
            }

            override fun onParticipantDisconnected(participantDisconnectedEvent: ParticipantDisconnectedEvent?) {
                RtcUiLogger.d("onParticipantDisconnected")
            }

            override fun onParticipantReconnected(participantReconnectedEvent: ParticipantReconnectedEvent?) {
                RtcUiLogger.d("onParticipantReconnected")
            }

            override fun onParticipantRoleChanged(participantRoleChangedEvent: ParticipantRoleChangedEvent?) {
                RtcUiLogger.d("onParticipantRoleChanged")
            }
            //endregion

            //region Dialog
            override fun onDialogJoined(dialogJoinedEvent: DialogJoinedEvent?) {
                RtcUiLogger.d("onDialogJoined")
            }

            override fun onDialogLeft(dialogLeftEvent: DialogLeftEvent?) {
                RtcUiLogger.d("onDialogLeft")
            }

            override fun onDialogRecordingStarted(dialogRecordingStartedEvent: DialogRecordingStartedEvent?) {
                RtcUiLogger.d("onDialogRecordingStarted")
            }

            override fun onDialogRecordingStopped(dialogRecordingStoppedEvent: DialogRecordingStoppedEvent?) {
                RtcUiLogger.d("onDialogRecordingStopped")
            }
            //endregion

            //region Machine detection
            override fun onMachineDetectionFinished(machineDetectionFinishedEvent: MachineDetectionFinishedEvent?) {
                RtcUiLogger.d("onMachineDetectionFinished")
            }

            override fun onMachineDetectionFailed(machineDetectionFailedEvent: MachineDetectionFailedEvent?) {
                RtcUiLogger.d("onMachineDetectionFailed")
            }
            //endregion
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.updateState { copy(isPip = isInPictureInPictureMode) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!viewModel.state.value.isPip && viewModel.orientation != newConfig.orientation) {
            viewModel.orientation = newConfig.orientation
            intent.action = null
            intent.removeExtra(ACCEPT_EXTRA_KEY)
            recreate()
        }
    }

}
