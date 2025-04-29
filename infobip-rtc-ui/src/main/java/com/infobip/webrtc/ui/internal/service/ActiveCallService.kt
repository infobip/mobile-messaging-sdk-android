package com.infobip.webrtc.ui.internal.service

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.infobip.webrtc.sdk.api.model.CallStatus
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.RtcUiCallErrorMapper
import com.infobip.webrtc.ui.internal.core.Cache
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.core.RtcUiCallErrorMapperFactory
import com.infobip.webrtc.ui.internal.core.TAG
import com.infobip.webrtc.ui.internal.delegate.CallsDelegate
import com.infobip.webrtc.ui.internal.delegate.PhoneStateDelegate
import com.infobip.webrtc.ui.internal.delegate.PhoneStateDelegateFactory
import com.infobip.webrtc.ui.internal.delegate.Vibrator
import com.infobip.webrtc.ui.internal.delegate.VibratorImpl
import com.infobip.webrtc.ui.internal.model.CallAction
import com.infobip.webrtc.ui.internal.notification.CALL_NOTIFICATION_ID
import com.infobip.webrtc.ui.internal.notification.CallNotificationFactory
import com.infobip.webrtc.ui.model.RtcUiError

class ActiveCallService : BaseService() {

    companion object {

        const val PEER_EXTRA = "com.infobip.calls.ui.service.OngoingCallService.PEER_EXTRA"
        const val CALL_STATUS_EXTRA = "com.infobip.calls.ui.service.OngoingCallService.CALL_STATUS_EXTRA"

        private val Context.hasPushPermission: Boolean
            get() {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                else
                    true
            }

        fun startIntent(
            context: Context,
            action: CallAction,
            peer: String? = null,
            callStatus: CallStatus? = null,
        ): Intent {
            return Intent(context, ActiveCallService::class.java).apply {
                setAction(action.name)
                putExtra(PEER_EXTRA, peer)
                putExtra(CALL_STATUS_EXTRA, callStatus?.name)
            }
        }

        fun start(
            context: Context,
            action: CallAction,
            peer: String? = null,
            callStatus: CallStatus? = null,
            foreground: Boolean = false
        ) {
            runCatching {
                val intent = startIntent(context, action, peer, callStatus)
                if (foreground && context.hasPushPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }.onFailure {
                Log.e(TAG, "Could not start service with argument = action: $action, peer: $peer, foreground: $foreground", it)
            }
        }

    }

    private val vibrateDelegate: Vibrator by lazy { VibratorImpl(this@ActiveCallService) }
    private val incomingCallRingtone: Ringtone by lazy { RingtoneManager.getRingtone(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)) }
    private val audioManager: AudioManager by lazy { applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val notificationHelper: CallNotificationFactory by lazy { Injector.notificationFactory }
    private val callsDelegate: CallsDelegate by lazy { Injector.callsDelegate }
    private val phoneStateDelegate: PhoneStateDelegate by lazy { PhoneStateDelegateFactory.getPhoneStateDelegate(this@ActiveCallService) }
    private val cache: Cache by lazy { Injector.cache }
    private val errorMapper: RtcUiCallErrorMapper by lazy { RtcUiCallErrorMapperFactory.create(this@ActiveCallService) }

    private var cachedPeer: String? = null
    private var reconnectingTonePlayer: MediaPlayer? = null
    private var isCallActive: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Handle action: ${intent?.action.orEmpty()}")
        val action: String? = intent?.action
        val callAction = CallAction.fromValue(action) ?: return START_NOT_STICKY
        val peer = intent?.getStringExtra(PEER_EXTRA)?.takeIf { it.isNotBlank() }?.also { cachedPeer = it }
            ?: cachedPeer?.takeIf { it.isNotBlank() }
            ?: applicationContext.getString(R.string.mm_unknown)

        when (callAction) {
            CallAction.INCOMING_CALL_START -> {
                val activeCallStatus = intent?.getStringExtra(CALL_STATUS_EXTRA)?.let { runCatching { CallStatus.valueOf(it) }.getOrNull() } ?: CallStatus.FINISHED
                val phoneState = phoneStateDelegate.getState()

                if (phoneState == -1 && cache.autoDeclineOnMissingReadPhoneStatePermission) { //could not get phone state because of missing permission
                    showToast(RtcUiError.MISSING_READ_PHONE_STATE_PERMISSION)
                    callsDelegate.decline()
                    onCallFinished()
                } else if (phoneState > 0 && cache.autoDeclineWhenOngoingCellularCall) { //there is ongoing or ringing cellular phone call
                    showToast(RtcUiError.INCOMING_WEBRTC_CALL_WHILE_CELLULAR_CALL)
                    callsDelegate.decline()
                    onCallFinished()
                } else if (!hasPushPermission && cache.autoDeclineOnMissingNotificationPermission) {
                    showToast(RtcUiError.MISSING_POST_NOTIFICATIONS_PERMISSION)
                    callsDelegate.decline()
                    onCallFinished()
                } else if (activeCallStatus != CallStatus.FINISHED && activeCallStatus != CallStatus.FINISHING && hasPushPermission) {
                    startForeground(notificationHelper.createIncomingCallNotification(this, peer, getString(R.string.mm_incoming_call)))
                    startRinging()
                } else {
                    Log.e(TAG, "Incoming call not handled! callStatus=$activeCallStatus, hasPushPermission=$hasPushPermission")
                }
            }

            CallAction.SILENT_INCOMING_CALL_START -> {
                startForeground(notificationHelper.createIncomingCallNotificationSilent(this, peer, getString(R.string.mm_incoming_call)))
            }

            CallAction.INCOMING_CALL_ACCEPTED -> {
                stopRinging()
            }

            CallAction.CALL_DECLINE -> {
                callsDelegate.decline()
                onCallFinished()
            }

            CallAction.CALL_RINGING,
            CallAction.CALL_EARLY_MEDIA -> {
            }

            CallAction.CALL_ESTABLISHED -> {
                stopRinging()
                startForeground(notificationHelper.createOngoingCallNotification(this, peer, getString(R.string.mm_in_call)))
                isCallActive = true
            }

            CallAction.CALL_HANGUP -> {
                callsDelegate.hangup()
                onCallFinished()
            }

            CallAction.CALL_FINISHED -> {
                onCallFinished()
            }

            CallAction.CALL_RECONNECTING -> {
                startReconnectingTone()
            }

            CallAction.CALL_RECONNECTED -> {
                stopReconnectingToneAndPlayReconnection()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForeground(notification: Notification) {
        if (hasPushPermission) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL else 0
            ServiceCompat.startForeground(this, CALL_NOTIFICATION_ID, notification, serviceType)
        }
    }

    private fun onCallFinished() {
        ScreenShareService.sendScreenShareServiceIntent(
            applicationContext,
            ScreenShareService.ACTION_STOP_SCREEN_SHARE
        )
        stopRinging()
        stopReconnectingTone()
        if (isCallActive) {
            playCallFinishedTone()
            isCallActive = false
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startRinging() {
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_VIBRATE -> vibrateDelegate.vibrate()
            AudioManager.RINGER_MODE_NORMAL -> {
                incomingCallRingtone.play()
                vibrateDelegate.vibrate()
            }
        }
    }

    private fun stopRinging() {
        incomingCallRingtone.stop()
        vibrateDelegate.stopVibrate()
    }

    private fun startReconnectingTone() {
        if (reconnectingTonePlayer != null)
            stopReconnectingTone()

        runCatching {
            reconnectingTonePlayer = createMediaPlayer(
                R.raw.reconnecting,
                AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING,
                AudioManager.STREAM_VOICE_CALL,
                true
            ).apply {
                prepare()
                start()
            }
        }.onFailure {
            Log.e(TAG, "startReconnectingTone() failed")
        }
    }

    private fun stopReconnectingTone() {
        reconnectingTonePlayer?.releaseSafely()
        reconnectingTonePlayer = null
    }

    private fun stopReconnectingToneAndPlayReconnection() {
        stopReconnectingTone()

        runCatching {
            createMediaPlayer(
                R.raw.reconnected,
                AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING,
                AudioManager.STREAM_VOICE_CALL,
                false
            ).apply {
                setOnCompletionListener {
                    releaseSafely()
                }
                prepare()
                start()
            }
        }.onFailure {
            Log.e(TAG, "stopReconnectingTone() failed")
        }
    }

    private fun playCallFinishedTone() {
        runCatching {
            createMediaPlayer(
                R.raw.finished,
                AudioAttributes.USAGE_NOTIFICATION,
                AudioManager.STREAM_VOICE_CALL,
                false
            ).apply {
                setOnCompletionListener {
                    releaseSafely()
                }
                prepare()
                start()
            }
        }.onFailure {
            Log.e(TAG, "playCallFinishedTone() failed")
        }
    }

    private fun createMediaPlayer(
        soundId: Int,
        usage: Int,
        streamType: Int,
        looping: Boolean
    ): MediaPlayer {
        return MediaPlayer().apply {
            val attrs = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(usage)
                .setLegacyStreamType(streamType)
                .build()
            setAudioAttributes(attrs)
            isLooping = looping
            val assetFileDescriptor = resources.openRawResourceFd(soundId)
            setDataSource(assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset, assetFileDescriptor.length)
        }
    }

    private fun MediaPlayer.releaseSafely() {
        runCatching {
            stop()
            release()
        }.onFailure {
            Log.e(TAG, "stop() and released() failed")
        }
    }

    private fun showToast(errorCode: RtcUiError) {
        val message = errorMapper.getMessageForError(errorCode)
        if (message?.isNotBlank() == true) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}