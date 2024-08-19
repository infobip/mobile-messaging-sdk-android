package com.infobip.webrtc.ui.internal.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AppOpsManager
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
import androidx.core.content.ContextCompat.registerReceiver
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.infobip.webrtc.sdk.api.model.video.RTCVideoTrack
import com.infobip.webrtc.sdk.api.model.video.ScreenCapturer
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.databinding.FragmentInCallBinding
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.core.TAG
import com.infobip.webrtc.ui.internal.model.CallState
import com.infobip.webrtc.ui.internal.service.ScreenShareService
import com.infobip.webrtc.ui.internal.ui.CallViewModel
import com.infobip.webrtc.ui.internal.ui.view.CircleImageButton
import com.infobip.webrtc.ui.internal.ui.view.InCallButtonAbs
import com.infobip.webrtc.ui.internal.ui.view.PipParamsFactory
import com.infobip.webrtc.ui.internal.ui.view.RowImageButton
import com.infobip.webrtc.ui.internal.utils.px
import com.infobip.webrtc.ui.internal.utils.show
import com.infobip.webrtc.ui.model.InCallButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

    private fun <T> Flow<T>.launchWithLifecycle(
        lifecycle: Lifecycle = viewLifecycleOwner.lifecycle,
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED
    ) = flowWithLifecycle(lifecycle, minActiveState).launchIn(lifecycle.coroutineScope)

    private var _binding: FragmentInCallBinding? = null
    private val customInCallButtons: MutableList<InCallButtonAbs> = mutableListOf()
    private val remoteVideoRenderer
        get() =
            if (viewModel.state.value.isLocalScreenShare || viewModel.state.value.isRemoteScreenShare) binding.remoteVideoScreenSharing else binding.remoteVideo
    private val localVideoRenderer
        get() =
            if (viewModel.state.value.isLocalScreenShare || viewModel.state.value.isRemoteScreenShare) binding.localVideoScreenSharing else binding.localVideo
    private val binding get() = _binding!!
    private val requestVideoPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            toggleVideo()
        }
    }
    private val viewModel: CallViewModel by activityViewModels()
    private val startMediaProjection =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                this@InCallFragment.viewLifecycleOwner.lifecycleScope.launch {
                    ScreenShareService.sendScreenShareServiceIntent(
                        requireContext(),
                        ScreenShareService.ACTION_START_SCREEN_SHARE
                    )
                    //ScreenShareService MUST be running before we start screen share, otherwise there is SecurityException, wait at most 200ms
                    var counter = 0
                    while (!ScreenShareService.isRunning) {
                        if (counter > 10)
                            break
                        Log.d(TAG, "Waiting for ScreenShareService...")
                        delay(20)
                        counter++
                    }
                    if (ScreenShareService.isRunning){
                        Log.d(TAG, "Starting screen sharing")
                        viewModel.shareScreen(ScreenCapturer(result.resultCode, result.data))
                            .onSuccess {
                                onScreenShareStateChanged()
                            }.onFailure {
                                ScreenShareService.sendScreenShareServiceIntent(
                                    requireContext(),
                                    ScreenShareService.ACTION_STOP_SCREEN_SHARE
                                )
                            }
                    }
                }
            }
        }
    private val isPipSupported by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                && hasPipPermission()
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

    private var speakerButton: InCallButtonAbs? = null
    private var muteButton: InCallButtonAbs? = null
    private var flipCamButton: InCallButtonAbs? = null
    private var screenShareButton: InCallButtonAbs? = null
    private var videoButton: InCallButtonAbs? = null
    private var hangupButton: InCallButtonAbs? = null
    private var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>? = null

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
            .launchWithLifecycle()
        viewModel.remoteTrackToBeRemoved
            .onEach {
                it.removeSink(binding.remoteVideo)
                it.removeSink(binding.remoteVideoScreenSharing)
            }
            .catch { Log.e(TAG, "Failed to remove sink", it) }
            .launchWithLifecycle()
        viewModel.localVideoTrack
            .onEach(::handleLocalVideoTrack)
            .launchWithLifecycle()
        viewModel.localTrackToBeRemoved
            .onEach {
                it.removeSink(binding.localVideo)
                it.removeSink(binding.localVideoScreenSharing)
            }
            .catch { Log.e(TAG, "Failed to remove sink", it) }
            .launchWithLifecycle()
        viewModel.screenShareTrack
            .onEach(::handleScreenShareTrack)
            .launchWithLifecycle()
        viewModel.screenShareTrackToBeRemoved
            .onEach { it.removeSink(binding.remoteVideo) }
            .catch { Log.e(TAG, "Failed to remove sink", it) }
            .launchWithLifecycle()
        viewModel.state
            .onEach(::renderState)
            .catch { Log.e(TAG, "Failed to render state", it) }
            .launchWithLifecycle()
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
        populateBottomSheet()
        customize()
        with(binding) {
            localVideo.run {
                init()
                setMirror(true)
                setZOrderMediaOverlay(true)
                setOnTouchListener(getLocalVideoTouchListener())
            }
            remoteVideo.run {
                init()
                setEnableHardwareScaler(true)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            }
            localVideoScreenSharing.run {
                init()
                setMirror(true)
                setZOrderMediaOverlay(true)
            }
            remoteVideoScreenSharing.run {
                init()
                setEnableHardwareScaler(true)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            }
            screenSharingRenderers.setOnTouchListener(getLocalVideoTouchListener())
            viewModel.peerName.let {
                nameInVoice.text = it
                nameInVideo.text = it
                nameInPip.text = it
            }
            remoteVideo.setOnClickListener {
                toggleControls()
            }
            root.setOnClickListener {
                toggleControls()
            }
            screenSharingDisable.setOnClickListener {
                toggleScreenShare()
            }
            if (isPipSupported) {
                collapseCallButton.setOnClickListener { requireActivity().enterPictureInPictureMode(createPipParams()) }
                registerReceiver(
                    requireContext(),
                    pipActionsReceiver,
                    IntentFilter(ACTION_PIP),
                    RECEIVER_NOT_EXPORTED
                )
            }
        }
    }

    private fun toggleControls() {
        viewModel.toggleControlsVisibility()
        if (viewModel.state.value.showControls) {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        } else {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun populateBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.bottomSheetButtons)
        bottomSheetBehavior?.isGestureInsetBottomIgnored = true
        bottomSheetBehavior?.isFitToContents = false
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        // main area
        Injector.cache.inCallButtons.take(4).forEach { inCallButton ->
            val button = CircleImageButton(requireContext()).apply {
                id = inCallButton.id
                setIcon(inCallButton.iconRes)
                inCallButton.checkedIconRes?.let { setCheckedIcon(it) }
            }
            binding.bottomSheet.mainButtons.addView(button)
            initBottomSheetButton(inCallButton, button)
        }
        // dragged area
        Injector.cache.inCallButtons.asSequence().drop(4).forEach { inCallButton ->
            val button = RowImageButton(requireContext()).apply {
                id = inCallButton.id
                setIcon(inCallButton.iconRes)
                setLabelText(getString(inCallButton.labelRes))
                inCallButton.checkedIconRes?.let { setCheckedIcon(it) }
            }
            binding.bottomSheet.secondaryButtons.addView(button)
            initBottomSheetButton(inCallButton, button)
        }

        // calculate half expanded ratio
        binding.bottomSheet.bottomSheetButtons.doOnLayout {
            updateBottomSheetRatio()
        }

        bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    showFlipCam(viewModel.state.value.isLocalVideo)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })
    }

    private fun updateBottomSheetRatio() {
        val mainAreaWithMargins = binding.bottomSheet.mainButtons.height + 32.px + 3.px + 8.px //main buttons, button's margins, pill and pill bottom margin
        val sheetHeight = binding.bottomSheet.bottomSheetButtons.height
        if (sheetHeight != 0) {
            val ratio = mainAreaWithMargins.toFloat() / sheetHeight
            bottomSheetBehavior?.halfExpandedRatio = ratio
        }
        binding.bottomSheet.bottomSheetButtons.requestLayout()
    }

    private fun initBottomSheetButton(buttonDesc: InCallButton, button: InCallButtonAbs) {
        button.run {
            setLabelColor()
            setBackgroundColor()
            setIconTint()
        }
        when (button.id) {
            R.id.rtc_ui_speaker_button -> {
                speakerButton = button
                speakerButton?.setOnClickListener {
                    toggleSpeaker()
                    buttonDesc.onClick()
                }
            }

            R.id.rtc_ui_mute_button -> {
                muteButton = button
                muteButton?.setOnClickListener {
                    toggleMute()
                    buttonDesc.onClick()
                }
            }

            R.id.rtc_ui_flip_cam_button -> {
                flipCamButton = button
                flipCamButton?.setOnClickListener {
                    viewModel.flipCamera()
                    buttonDesc.onClick()
                }
            }

            R.id.rtc_ui_screen_share_button -> {
                screenShareButton = button
                screenShareButton?.setOnClickListener {
                    toggleScreenShare()
                    buttonDesc.onClick()
                }
            }

            R.id.rtc_ui_video_button -> {
                videoButton = button
                videoButton?.setOnClickListener {
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
                buttonDesc.onClick()
            }

            R.id.rtc_ui_hang_up_button -> {
                hangupButton = button
                Injector.cache.colors?.let { res ->
                    hangupButton?.setIconTint(ColorStateList.valueOf(res.rtcUiActionsIcon))
                    hangupButton?.setBackgroundColor(ColorStateList.valueOf(res.rtcUiHangup))
                }
                hangupButton?.setLabelColor()
                hangupButton?.setOnClickListener {
                    viewModel.hangup()
                }
            }

            else -> {
                if (buttonDesc is InCallButton.Custom) {
                    buttonDesc.setChecked?.let {
                        button.checkedCondition = { it.invoke() }
                    }
                    buttonDesc.setEnabled?.let {
                        button.enabledCondition = { it.invoke() }
                    }
                    button.setOnClickListener {
                        buttonDesc.onClick()
                    }
                    customInCallButtons.add(button)
                }
            }
        }
    }

    private fun renderState(state: CallState) {
        with(state) {
            Log.d(TAG, state.toString())
            val timeFormatted = viewModel.formatTime(elapsedTimeSeconds)
            if (isRemoteVideo || isLocalScreenShare && !isPip) {
                binding.elapsedTimeVideo.text = timeFormatted
            } else {
                binding.elapsedTimeVoice.text = timeFormatted
            }
            //audio/video call views
            binding.localVideoScreenSharing.isVisible = isLocalVideo && (isLocalScreenShare || isRemoteScreenShare) && !isPip
            binding.localVideo.isVisible = isLocalVideo && !isLocalScreenShare && !isRemoteScreenShare && !isPip
            binding.remoteVideoScreenSharing.isVisible = isRemoteVideo && (isLocalScreenShare || isRemoteScreenShare) && !isPip
            binding.remoteVideoWrapper.isVisible = (isRemoteVideo || isRemoteScreenShare) && !isLocalScreenShare
            binding.voiceGroup.isVisible = !isRemoteVideo && !isRemoteScreenShare && !isLocalScreenShare && !isPip
            binding.videoGroup.isVisible = (isRemoteVideo || isLocalScreenShare || isRemoteScreenShare) && !isPip && showControls
            binding.elapsedTimeVoice.isVisible = (!isRemoteVideo && !isRemoteScreenShare && !isLocalScreenShare) || isPip
            //screenshare
            binding.screenSharingNotice.isVisible = isLocalScreenShare
            binding.screenSharingDisable.isVisible = isLocalScreenShare && !isPip
            Injector.cache.colors.let { res -> (if (isLocalScreenShare) res?.rtcUiActionsBackground else res?.rtcUiBackground)?.let { binding.background.setBackgroundColor(it) } }
            //another views
            binding.connectionAlert.isVisible = callAlert != null && !isPip && showControls
            binding.connectionAlert.setMode(callAlert)
            binding.mutedMicrophoneAlert.isVisible = isMuted && !isPip && showControls
            binding.peerMuteIndicatorInVideo.isVisible = (isRemoteVideo || isLocalScreenShare || isRemoteScreenShare) && isPeerMuted == true && !isPip && showControls
            binding.peerMuteIndicatorInVoice.isVisible = !isRemoteVideo && !isLocalScreenShare && !isRemoteScreenShare && isPeerMuted == true && !isPip
            binding.collapseCallButton.isVisible = !isPip && showControls && isPipSupported
            //in PIP
            binding.nameInPip.isVisible = isPip
            //buttons
            binding.bottomSheet.bottomSheetButtons.isVisible = !isPip
            showFlipCam(isLocalVideo)
            //check state
            muteButton?.isChecked = !isMuted
            speakerButton?.isChecked = isSpeakerOn
            screenShareButton?.isChecked = isLocalScreenShare
            videoButton?.isChecked = isLocalVideo
            customInCallButtons.forEach { it.refreshChecked(); it.refreshEnabled() }
        }
    }

    /**
     * Show/hide circle button straight away as it does not require to recalculate halfExpandedRatio and redraw
     * For row button show/hide only in expanded state. This way we dodge awkward behaviour when button is show/hidden in half-expanded state.
     */
    private fun showFlipCam(isLocalVideo: Boolean) {
        if (flipCamButton is CircleImageButton) {
            flipCamButton?.show(isLocalVideo)
        } else {
            if (bottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                flipCamButton?.show(isLocalVideo)
            }
        }
        updateBottomSheetRatio()
    }

    private fun handleLocalVideoTrack(track: RTCVideoTrack?) {
        if (viewModel.state.value.isPip) {
            updatePictureInPictureMode()
        }
        runCatching {
            track?.addSink(localVideoRenderer)
        }.onFailure {
            Log.e(TAG, "Handle local video sink failed.", it)
        }
    }

    private fun handleRemoteVideoTrack(track: RTCVideoTrack?) {
        runCatching {
            track?.addSink(remoteVideoRenderer)
        }.onFailure {
            Log.e(TAG, "Handle remote video sink failed.", it)
        }
    }

    private fun handleScreenShareTrack(track: RTCVideoTrack?) {
        runCatching {
            track?.addSink(binding.remoteVideo)
            onScreenShareStateChanged()
        }.onFailure {
            Log.e(TAG, "Handle remote video sink failed.", it)
        }
    }

    /**
     * We have 2 separate renderers for local/video video. One is used when (local) screen sharing
     * is disabled and second when enabled. This function ensures correct rendered is used when screen
     * sharing is enabled/disabled.
     */
    private fun onScreenShareStateChanged() {
        switchLocalTrackSink()
        switchRemoteTrackSink()
    }

    private fun switchLocalTrackSink() {
        runCatching {
            viewModel.state.value.localVideoTrack?.run {
                removeSink(binding.localVideo)
                removeSink(binding.localVideoScreenSharing)
                addSink(localVideoRenderer)
            }
        }.onFailure {
            Log.e(TAG, "Handle local video sink failed.", it)
        }
    }

    private fun switchRemoteTrackSink() {
        runCatching {
            viewModel.state.value.remoteVideoTrack?.run {
                removeSink(binding.remoteVideo)
                removeSink(binding.remoteVideoScreenSharing)
                addSink(remoteVideoRenderer)
            }
        }.onFailure {
            Log.e(TAG, "Handle local video sink failed.", it)
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
        if (viewModel.state.value.isLocalScreenShare) {
            viewModel.stopScreenShare()
            ScreenShareService.sendScreenShareServiceIntent(
                requireContext(),
                ScreenShareService.ACTION_STOP_SCREEN_SHARE
            )
            onScreenShareStateChanged()
        } else {
            bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            val mediaProjectionManager =
                requireActivity().getSystemService(AppCompatActivity.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
        }

    }

    private fun showVideoRationale() {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.mm_video_permission_required)
            .setCancelable(false)
            .setPositiveButton(R.string.mm_ok) { _, _ ->
                requestVideoPermissionLauncher.launch(Manifest.permission.CAMERA)
            }.setNegativeButton(R.string.mm_cancel) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun customize() {
        Injector.cache.colors?.let { res ->
            val foregroundColorStateList = ColorStateList.valueOf(res.rtcUiForeground)
            with(binding) {
                toolbarBackground.setBackgroundColor(res.rtcUiOverlayBackground)
                nameInVideo.setTextColor(foregroundColorStateList)
                nameInVoice.setTextColor(foregroundColorStateList)
                nameInPip.setTextColor(foregroundColorStateList)
                peerMuteIndicatorInVideo.imageTintList = foregroundColorStateList
                peerMuteIndicatorInVoice.imageTintList = foregroundColorStateList
                nameDivider.setBackgroundColor(res.rtcUiForeground)
                elapsedTimeVideo.setTextColor(foregroundColorStateList)
                elapsedTimeVoice.setTextColor(foregroundColorStateList)
                collapseCallButton.imageTintList = foregroundColorStateList
                avatar.imageTintList = foregroundColorStateList
                background.setBackgroundColor(res.rtcUiBackground)
                bottomSheet.pill.backgroundTintList = ColorStateList.valueOf(res.rtcUiColorSheetPill)
                bottomSheet.divider.setBackgroundColor(res.rtcUiColorActionsDivider)
                bottomSheet.bottomSheetButtons.setBackgroundColor(res.rtcUiColorSheetBackground)
            }
        }
    }

    private fun getLocalVideoTouchListener(): View.OnTouchListener {
        return object : View.OnTouchListener {
            var dX = 0f
            var dY = 0f

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
                PipParamsFactory.createVideoPipParams(requireContext(), isMuted)
            else
                PipParamsFactory.createVoicePipParams(requireContext(), isMuted, isSpeakerOn)
        }
    }

    private fun hasPipPermission(): Boolean {
        val context = requireContext()
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager?
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps?.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps?.checkOpNoThrow(
                    AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            false
        }
    }

    override fun onDestroyView() {
        runCatching {
            with(binding) {
                localVideo.release()
                remoteVideo.release()
                localVideoScreenSharing.release()
                remoteVideoScreenSharing.release()
                root.setOnClickListener(null)
                remoteVideo.setOnClickListener(null)
                collapseCallButton.setOnClickListener(null)
                localVideo.setOnTouchListener(null)
                screenSharingRenderers.setOnTouchListener(null)
            }
            _binding = null
            hangupButton?.setOnClickListener(null)
            muteButton?.setOnClickListener(null)
            speakerButton?.setOnClickListener(null)
            flipCamButton?.setOnClickListener(null)
            videoButton?.setOnClickListener(null)
            screenShareButton?.setOnClickListener(null)
            customInCallButtons.forEach { it.setOnClickListener(null) }
            if (isPipSupported)
                context?.unregisterReceiver(pipActionsReceiver)
        }.onFailure {
            Log.e(TAG, "Cleanup failed.", it)
        }
        super.onDestroyView()
    }
}