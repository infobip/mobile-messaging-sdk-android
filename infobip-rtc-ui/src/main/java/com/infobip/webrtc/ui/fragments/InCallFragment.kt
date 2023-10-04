package com.infobip.webrtc.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Rect
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.infobip.webrtc.Injector.colors
import com.infobip.webrtc.TAG
import com.infobip.webrtc.sdk.api.model.video.RTCVideoTrack
import com.infobip.webrtc.sdk.api.model.video.ScreenCapturer
import com.infobip.webrtc.ui.*
import com.infobip.webrtc.ui.databinding.FragmentInCallBinding
import com.infobip.webrtc.ui.model.CallState
import com.infobip.webrtc.ui.service.ScreenShareService
import com.infobip.webrtc.ui.utils.activatedColorStateList
import com.infobip.webrtc.ui.view.PipParamsFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.webrtc.RendererCommon
import kotlin.time.Duration.Companion.seconds

class InCallFragment : Fragment() {
    companion object {
        internal const val ACTION_PIP =
            "com.infobip.conversations.app.ui.call.InCallFragment.ACTION_PIP"
        internal const val EXTRAS_PIP_ACTION =
            "com.infobip.conversations.app.ui.call.InCallFragment.EXTRAS_PIP_ACTION"
        const val PIP_ACTION_MUTE = 1
        const val PIP_ACTION_SPEAKER = 2
        const val PIP_ACTION_VIDEO = 3
        const val PIP_ACTION_HANGUP = 4

        fun pipActionIntent(pipAction: Int): Intent {
            return Intent(ACTION_PIP).apply {
                putExtra(EXTRAS_PIP_ACTION, pipAction)
            }
        }
    }

    private var _binding: FragmentInCallBinding? = null
    private val binding get() = _binding!!
    private val requestVideoPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            toggleVideo()
        }
    }
    private val viewModel: CallViewModel by activityViewModels()
    private val requestMediaProjection =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                viewModel.shareScreen(ScreenCapturer(it.resultCode, it.data))
            }
        }
    private val pipActionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PIP) {
                when (intent.getIntExtra(EXTRAS_PIP_ACTION, 0)) {
                    PIP_ACTION_MUTE -> toggleMute()
                    PIP_ACTION_SPEAKER -> toggleSpeaker()
                    PIP_ACTION_VIDEO -> toggleVideo()
                    PIP_ACTION_HANGUP -> viewModel.hangup()
                }
                updatePictureInPictureMode()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentInCallBinding.inflate(inflater, container, false).also { _binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.remoteVideoTrack
            .onEach(::handleRemoteVideoTrack)
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .launchIn(viewLifecycleOwner.lifecycleScope)
        viewModel.localVideoTrack
            .onEach(::handleLocalVideoTrack)
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .launchIn(viewLifecycleOwner.lifecycleScope)
        viewModel.state
            .onEach(::renderState)
            .catch { Log.e(TAG, "Failed to render state", it) }
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .launchIn(viewLifecycleOwner.lifecycleScope)
        setUpScreen()
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            while (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                viewModel.updateState { copy(elapsedTimeSeconds = viewModel.callDuration()) }
                delay(1.seconds)
            }
        }
    }

    @SuppressLint("NewApi", "ClickableViewAccessibility")
    private fun setUpScreen() {
        customize()
        with(binding) {
            localVideo.init()
            localVideo.setMirror(true)
            localVideo.setZOrderMediaOverlay(true)
            localVideo.setOnTouchListener(getLocalVideoTouchListener())
            remoteVideo.init()
            remoteVideo.setEnableHardwareScaler(true)
            remoteVideo.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            viewModel.peerName.let {
                nameInVoice.text = it
                nameInVideo.text = it
                nameInPip.text = it
            }

            remoteVideo.setOnClickListener {
                viewModel.toggleControlsVisibility()
            }
            root.setOnClickListener {
                viewModel.toggleControlsVisibility()
            }
            hangupButton.setOnClickListener {
                viewModel.hangup()
            }
            muteButton.setOnClickListener {
                toggleMute()
            }
            speakerButton.setOnClickListener {
                toggleSpeaker()
            }
            flipCamButton.setOnClickListener {
                viewModel.flipCamera()
            }
            videoButton.setOnClickListener {
                val cameraPermission = Manifest.permission.CAMERA
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        cameraPermission
                    ) == PackageManager.PERMISSION_GRANTED -> toggleVideo()
                    shouldShowRequestPermissionRationale(cameraPermission) -> showVideoRationale()
                    else -> requestVideoPermissionLauncher.launch(cameraPermission)
                }
            }
            screenShareButton.setOnClickListener {
                toggleScreenShare()
            }
            val isPipSupported =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && requireActivity().packageManager.hasSystemFeature(
                    PackageManager.FEATURE_PICTURE_IN_PICTURE
                )
            if (isPipSupported) {
                collapseCallButton.setOnClickListener {
                    requireActivity().enterPictureInPictureMode(createPipParams())
                }
                registerReceiver(
                    requireContext(),
                    pipActionsReceiver,
                    IntentFilter(ACTION_PIP),
                    RECEIVER_NOT_EXPORTED
                )
            }
            collapseCallButton.isVisible = isPipSupported
        }
    }

    private fun renderState(state: CallState) {
        with(state) {
            Log.d(TAG, state.toString())
            val timeFormatted = viewModel.formatTime(elapsedTimeSeconds)
            if (isRemoteVideo) {
                binding.elapsedTimeVideo.text = timeFormatted
            } else {
                binding.elapsedTimeVoice.text = timeFormatted
            }
            //audio/video call views
            binding.localVideo.isVisible = isLocalVideo
            binding.remoteVideoWrapper.isVisible = isRemoteVideo
            binding.voiceGroup.isVisible = !isRemoteVideo && !isPip
            binding.videoGroup.isVisible = isRemoteVideo && !isPip && showControls
            binding.elapsedTimeVoice.isVisible = !isRemoteVideo || isPip
            //another views
            binding.weakConnectionAlert.isVisible = isWeakConnection && !isPip && showControls
            binding.mutedMicrophoneAlert.isVisible = isMuted && !isPip && showControls
            binding.peerMuteIndicatorInVideo.isVisible = isRemoteVideo && isPeerMuted == true && !isPip && showControls
            binding.peerMuteIndicatorInVoice.isVisible = !isRemoteVideo && isPeerMuted == true && !isPip
            binding.collapseCallButton.isVisible = !isPip && showControls
            //in PIP
            binding.nameInPip.isVisible = isPip
            //buttons
            binding.flipCam.isVisible = isLocalVideo && !isPip && showControls
            binding.mute.isVisible = !isPip && showControls
            binding.speaker.isVisible = !isPip && showControls
            binding.hangupButton.isVisible = !isPip && showControls
            binding.screenShare.isVisible = !isPip && showControls
            binding.video.isVisible = !isPip && showControls
            //check state
            binding.muteButton.isChecked = !isMuted
            binding.speakerButton.isChecked = isSpeakerOn
            binding.screenShareButton.isChecked = isScreenShare
            binding.videoButton.isChecked = isLocalVideo
        }
    }

    private fun handleLocalVideoTrack(track: RTCVideoTrack?) {
        with(binding) {
            runCatching {
                viewModel.getLocalVideo()?.removeSink(localVideo)
                track?.addSink(localVideo)
            }.onFailure {
                Log.e(TAG, "Handle local video sink failed.", it)
            }
        }
    }

    private fun handleRemoteVideoTrack(track: RTCVideoTrack?) {
        with(binding) {
            runCatching {
                viewModel.getRemoteVideos()?.iterator()?.forEach {
                    it.camera?.removeSink(binding.remoteVideo)
                    it.screenShare?.removeSink(binding.remoteVideo)
                }
                track?.addSink(remoteVideo)
            }.onFailure {
                Log.e(TAG, "Handle remote video sink failed.", it)
            }
        }
    }

    private fun toggleVideo() {
        viewModel.toggleVideo()
    }

    private fun toggleMute() {
        viewModel.toggleMute()
    }

    private fun toggleSpeaker() {
        viewModel.toggleSpeaker()
    }

    private fun toggleScreenShare() {
        if (viewModel.state.value.isScreenShare) {
            viewModel.stopScreenShare()
            ScreenShareService.sendScreenShareServiceIntent(
                requireContext(),
                ScreenShareService.ACTION_STOP_SCREEN_SHARE
            )
        } else {
            ScreenShareService.sendScreenShareServiceIntent(
                requireContext(),
                ScreenShareService.ACTION_START_SCREEN_SHARE
            )
            val mediaProjectionManager =
                requireActivity().getSystemService(AppCompatActivity.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            requestMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
        }
    }

    private fun showVideoRationale() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.mm_video_permission_required)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun customize() {
        colors?.let { res ->
            val foregroundColorStateList = ColorStateList.valueOf(res.rtc_ui_foreground)
            val actionsBackgroundColorStateList = activatedColorStateList(
                res.rtc_ui_actions_background_checked,
                res.rtc_ui_actions_background
            )
            val actionsIconColorStateList =
                activatedColorStateList(
                    res.rtc_ui_actions_icon_checked,
                    res.rtc_ui_actions_icon
                )

            with(binding) {
                toolbarBackground.setBackgroundColor(res.rtc_ui_overlay_background)
                nameInVideo.setTextColor(foregroundColorStateList)
                nameInVoice.setTextColor(foregroundColorStateList)
                nameInPip.setTextColor(foregroundColorStateList)
                peerMuteIndicatorInVideo.imageTintList = foregroundColorStateList
                peerMuteIndicatorInVoice.imageTintList = foregroundColorStateList
                nameDivider.setBackgroundColor(res.rtc_ui_foreground)
                elapsedTimeVideo.setTextColor(foregroundColorStateList)
                elapsedTimeVoice.setTextColor(foregroundColorStateList)
                collapseCallButton.imageTintList = foregroundColorStateList
                avatar.imageTintList = foregroundColorStateList
                muteButton.setCircleColor(actionsBackgroundColorStateList)
                muteButton.setIconTint(actionsIconColorStateList)
                videoButton.setCircleColor(actionsBackgroundColorStateList)
                videoButton.setIconTint(actionsIconColorStateList)
                speakerButton.setCircleColor(actionsBackgroundColorStateList)
                speakerButton.setIconTint(actionsIconColorStateList)
                flipCamButton.setCircleColor(actionsBackgroundColorStateList)
                flipCamButton.setIconTint(actionsIconColorStateList)
                screenShareButton.setCircleColor(actionsBackgroundColorStateList)
                screenShareButton.setIconTint(actionsIconColorStateList)
                hangupButton.setIconTint(ColorStateList.valueOf(res.rtc_ui_actions_icon))
                hangupButton.setCircleColor(ColorStateList.valueOf(res.rtc_ui_hangup))
                background.setBackgroundColor(res.rtc_ui_background)
            }
        }
    }

    private fun getLocalVideoTouchListener(): View.OnTouchListener {
        return object : View.OnTouchListener {
            var dX = 0f
            var dY = 0f
            var lastAction = 0

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View, event: MotionEvent): Boolean {
                val boundaries = Rect()
                binding.root.getLocalVisibleRect(boundaries)
                boundaries.right = boundaries.right - view.width
                boundaries.bottom = boundaries.bottom - view.height

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = view.x - event.rawX
                        dY = view.y - event.rawY
                        lastAction = MotionEvent.ACTION_DOWN
                    }
                    MotionEvent.ACTION_MOVE -> {
                        var x = event.rawX + dX
                        var y = event.rawY + dY
                        if (!boundaries.isEmpty) {
                            if (x > boundaries.right) x = boundaries.right.toFloat()
                            else if (x < boundaries.left) x = boundaries.left.toFloat()
                            if (y < boundaries.top) y = boundaries.top.toFloat()
                            else if (y > boundaries.bottom) y = boundaries.bottom.toFloat()
                        }
                        view.animate()
                            .x(x)
                            .y(y)
                            .setDuration(0)
                            .start()
                        lastAction = MotionEvent.ACTION_MOVE
                    }
                    MotionEvent.ACTION_UP -> {
                    }
                    else -> return false
                }
                return true
            }

        }
    }

    private fun updatePictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().setPictureInPictureParams(createPipParams())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createPipParams(): PictureInPictureParams {
        with(viewModel.state.value) {
            return if (isLocalVideo)
                PipParamsFactory.createVideoPipParams(requireContext(), isMuted, isLocalVideo)
            else
                PipParamsFactory.createVoicePipParams(requireContext(), isMuted, isSpeakerOn)
        }
    }

    override fun onDestroyView() {
        with(binding) {
            localVideo.release()
            remoteVideo.release()
            root.setOnClickListener(null)
            remoteVideo.setOnClickListener(null)
            hangupButton.setOnClickListener(null)
            muteButton.setOnClickListener(null)
            speakerButton.setOnClickListener(null)
            flipCamButton.setOnClickListener(null)
            videoButton.setOnClickListener(null)
            screenShareButton.setOnClickListener(null)
            collapseCallButton.setOnClickListener(null)
        }
        _binding = null
        super.onDestroyView()
    }
}