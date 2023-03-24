package com.infobip.webrtc.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.infobip.webrtc.Injector
import com.infobip.webrtc.TAG
import com.infobip.webrtc.sdk.api.event.call.*
import com.infobip.webrtc.sdk.api.model.ErrorCode
import com.infobip.webrtc.ui.fragments.InCallFragment
import com.infobip.webrtc.ui.fragments.IncomingCallFragment
import com.infobip.webrtc.ui.listeners.DefaultRtcUiCallEventListener
import com.infobip.webrtc.ui.model.CallState
import com.infobip.webrtc.ui.model.Colors
import com.infobip.webrtc.ui.model.Icons
import com.infobip.webrtc.ui.service.OngoingCallService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class CallActivity : AppCompatActivity(R.layout.activity_call) {
    companion object {
        private const val CALLER_EXTRA_KEY = "CALLER_EXTRA_KEY"
        private const val ACCEPT_EXTRA_KEY = "ACCEPT_EXTRA_KEY"

        fun newInstance(context: Context, caller: String, accept: Boolean = false): Intent {
            return Intent(context, CallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(CALLER_EXTRA_KEY, caller)
                putExtra(ACCEPT_EXTRA_KEY, accept)
            }
        }
    }

    private val viewModel: CallViewModel by viewModels()
    private val requestAudioPermissionLauncher =
            registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    showAudioRationale()
                }
            }

    override fun onCreateView(parent: View?, name: String, context: Context, attrs: AttributeSet): View? {
        if (Injector.colors == null)
            Injector.colors = Colors(this, attrs)
        if (Injector.icons == null)
            Injector.icons = Icons(this, attrs)
        return super.onCreateView(parent, name, context, attrs)
    }

    private fun renderState(state: CallState) {
        with(state) {
            if (error.isNotEmpty()) {
                showError(error)
            }
            if (isFinished) {
                showFinishCall()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.orientation = resources.configuration.orientation
        showWhenLocked()
        viewModel.init()
        listenCallEvents()
        applyArguments()
        showFragment()
        viewModel.state
                .onEach(::renderState)
                .flowOn(Dispatchers.Main)
                .flowWithLifecycle(lifecycle)
                .launchIn(lifecycleScope)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> Log.d(TAG, "Audio permission granted")
                shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> showAudioRationale()
                else -> requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun showAudioRationale() {
        AlertDialog.Builder(this)
                .setMessage(R.string.mm_audio_permission_required)
                .setCancelable(false)
                .setPositiveButton(R.string.mm_ok) { _, _ ->
                    requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }.setNegativeButton(R.string.mm_cancel) { _, _ ->
                    viewModel.endCall()
                }.show()
    }

    private fun showFragment() {
        if (viewModel.isIncomingCall()) {
            supportFragmentManager.navigate(IncomingCallFragment(), R.id.navHost)
        } else {
            supportFragmentManager.navigate(InCallFragment(), R.id.navHost)
        }
    }

    private fun applyArguments() {
        val peer = intent.getStringExtra(CALLER_EXTRA_KEY) ?: getString(R.string.mm_unknown)
        val accept = intent.getBooleanExtra(ACCEPT_EXTRA_KEY, false)
        if (accept) {
            viewModel.accept()
            OngoingCallService.sendCallServiceIntent(applicationContext, OngoingCallService.CALL_ESTABLISHED_ACTION)
        }
        viewModel.peerName = peer
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        if (viewModel.state.value.isFinished)
            finishAndHideNotifications()
    }

    private fun showFinishCall() {
        Toast.makeText(this, R.string.mm_call_finished, Toast.LENGTH_LONG).show()
        finishAndHideNotifications()
    }

    private fun finishAndHideNotifications() {
        OngoingCallService.sendCallServiceIntent(applicationContext, OngoingCallService.CALL_ENDED_ACTION)
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

            override fun onError(errorCode: ErrorCode?) {
                viewModel.onError(errorCode?.let { it.description ?: it.name }
                    ?: getString(R.string.mm_unknown_error))
            }

            override fun onCameraVideoAdded(cameraVideoAddedEvent: CameraVideoAddedEvent?) {
                viewModel.updateState { copy(localVideoTrack = cameraVideoAddedEvent?.track) }
            }

            override fun onCameraVideoUpdated(cameraVideoUpdatedEvent: CameraVideoUpdatedEvent?) {
                viewModel.updateState { copy(localVideoTrack = cameraVideoUpdatedEvent?.track) }
            }

            override fun onCameraVideoRemoved() {
                viewModel.updateState { copy(localVideoTrack = null) }
            }

            override fun onScreenShareAdded(screenShareAddedEvent: ScreenShareAddedEvent?) {
                viewModel.updateState { copy(isScreenShare = true) }
            }

            override fun onScreenShareRemoved() {
                viewModel.updateState { copy(isScreenShare = false) }
            }

            override fun onParticipantCameraVideoAdded(participantCameraVideoAddedEvent: ParticipantCameraVideoAddedEvent?) {
                viewModel.updateState { copy(remoteVideoTrack = participantCameraVideoAddedEvent?.track) }
            }

            override fun onParticipantCameraVideoRemoved(participantCameraVideoRemovedEvent: ParticipantCameraVideoRemovedEvent?) {
                viewModel.updateState { copy(remoteVideoTrack = null, showControls = true) }
            }

            override fun onParticipantScreenShareAdded(participantScreenShareAddedEvent: ParticipantScreenShareAddedEvent?) {
                viewModel.updateState { copy(screenShareTrack = participantScreenShareAddedEvent?.track) }
            }

            override fun onParticipantScreenShareRemoved(participantScreenShareRemovedEvent: ParticipantScreenShareRemovedEvent?) {
                viewModel.updateState { copy(screenShareTrack = null, showControls = true) }
            }

            override fun onParticipantUnmuted(participantUnmutedEvent: ParticipantUnmutedEvent?) {
                viewModel.updateState { copy(isPeerMuted = false) }
            }

            override fun onParticipantMuted(participantMutedEvent: ParticipantMutedEvent?) {
                viewModel.updateState { copy(isPeerMuted = true) }
            }

            override fun onParticipantLeft(participantLeftEvent: ParticipantLeftEvent?) {
                viewModel.updateState { copy(isFinished = true) }
            }

            override fun onRinging(callRingingEvent: CallRingingEvent?) {
                viewModel.updateState { copy(isIncoming = true) }
                runOnUiThread {
                    OngoingCallService.sendCallServiceIntent(applicationContext, OngoingCallService.INCOMING_CALL_ACTION)
                }
            }

            override fun onEarlyMedia(callEarlyMediaEvent: CallEarlyMediaEvent?) {
                runOnUiThread {
                    OngoingCallService.sendCallServiceIntent(applicationContext, OngoingCallService.CALL_ESTABLISHED_ACTION)
                }
            }

            override fun onEstablished(callEstablishedEvent: CallEstablishedEvent?) {
                viewModel.updateState { copy(isIncoming = false) }
                runOnUiThread {
                    OngoingCallService.sendCallServiceIntent(applicationContext, OngoingCallService.CALL_ESTABLISHED_ACTION)
                }
            }

            override fun onHangup(callHangupEvent: CallHangupEvent?) {
                viewModel.updateState { copy(isFinished = true) }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
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