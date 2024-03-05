package com.infobip.webrtc.ui.internal.service

import android.app.Notification
import android.content.Context
import android.content.Intent
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
import com.infobip.webrtc.sdk.api.model.CallStatus
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.RtcUiCallErrorMapper
import com.infobip.webrtc.ui.internal.core.Cache
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.core.RtcUiCallErrorMapperFactory
import com.infobip.webrtc.ui.internal.core.TAG
import com.infobip.webrtc.ui.internal.delegate.CallsDelegate
import com.infobip.webrtc.ui.internal.delegate.NotificationPermissionDelegate
import com.infobip.webrtc.ui.internal.delegate.PhoneStateDelegate
import com.infobip.webrtc.ui.internal.delegate.PhoneStateDelegateFactory
import com.infobip.webrtc.ui.internal.delegate.Vibrator
import com.infobip.webrtc.ui.internal.delegate.VibratorImpl
import com.infobip.webrtc.ui.internal.notification.CALL_NOTIFICATION_ID
import com.infobip.webrtc.ui.internal.notification.CallNotificationFactory
import com.infobip.webrtc.ui.model.RtcUiError

class OngoingCallService : BaseService() {

    companion object {

        fun sendCallServiceIntent(context: Context, action: String) {
            context.startService(Intent(context, OngoingCallService::class.java).apply { setAction(action) })
        }

        const val INCOMING_CALL_ACTION = "com.infobip.calls.ui.service.OngoingCallService.INCOMING_CALL_ACTION"
        const val SILENT_INCOMING_CALL_ACTION = "com.infobip.calls.ui.service.OngoingCallService.SILENT_INCOMING_CALL_ACTION"
        const val CALL_ENDED_ACTION = "com.infobip.calls.ui.service.OngoingCallService.END_CALL_ACTION"
        const val CALL_ESTABLISHED_ACTION = "com.infobip.calls.ui.service.OngoingCallService.CALL_ESTABLISHED_ACTION"
        const val CALL_DECLINED_ACTION = "com.infobip.calls.ui.service.OngoingCallService.CALL_DECLINED_ACTION"
        const val CALL_HANGUP_ACTION = "com.infobip.calls.ui.service.OngoingCallService.CALL_HANGUP_ACTION"
        const val CALL_RECONNECTING_ACTION = "com.infobip.calls.ui.service.OngoingCallService.CALL_RECONNECTING_ACTION"
        const val CALL_RECONNECTED_ACTION = "com.infobip.calls.ui.service.OngoingCallService.CALL_RECONNECTED_ACTION"

        const val NAME_EXTRA = "com.infobip.calls.ui.service.OngoingCallService.NAME_EXTRA"
        const val CALL_STATUS_EXTRA = "com.infobip.calls.ui.service.OngoingCallService.CALL_STATUS_EXTRA"
    }

    private val vibrateDelegate: Vibrator by lazy { VibratorImpl(this@OngoingCallService) }
    private val incomingCallRingtone: Ringtone by lazy { RingtoneManager.getRingtone(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)) }
    private val audioManager: AudioManager by lazy { applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val notificationHelper: CallNotificationFactory by lazy { Injector.notificationFactory }
    private val callsDelegate: CallsDelegate by lazy { Injector.callsDelegate }
    private val notificationPermissionDelegate: NotificationPermissionDelegate by lazy { Injector.notificationPermissionDelegate }
    private val hasNotificationPermission
        get() = notificationPermissionDelegate.hasPermission()
    private val phoneStateDelegate: PhoneStateDelegate by lazy { PhoneStateDelegateFactory.getPhoneStateDelegate(this@OngoingCallService) }
    private val cache: Cache by lazy { Injector.cache }
    private val errorMapper: RtcUiCallErrorMapper by lazy { RtcUiCallErrorMapperFactory.create(this@OngoingCallService) }

    private var peerName: String = ""
    private var reconnectingTonePlayer: MediaPlayer? = null
    private var isCallActive: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Handle action: ${intent?.action.orEmpty()}")
        when (intent?.action) {
            INCOMING_CALL_ACTION -> {
                val activeCallStatus = runCatching { CallStatus.valueOf(intent.getStringExtra(
                    CALL_STATUS_EXTRA
                ).orEmpty()) }.getOrDefault(CallStatus.FINISHED)
                val phoneState = phoneStateDelegate.getState()

                if (phoneState == -1 && cache.autoDeclineOnMissingReadPhoneStatePermission) { //could not get phone state because of missing permission
                    showToast(RtcUiError.MISSING_READ_PHONE_STATE_PERMISSION)
                    callsDelegate.decline()
                } else if (phoneState > 0 && cache.autoDeclineWhenOngoingCellularCall) { //there is ongoing or ringing cellular phone call
                    showToast(RtcUiError.INCOMING_WEBRTC_CALL_WHILE_CELLULAR_CALL)
                    callsDelegate.decline()
                } else if (!hasNotificationPermission && cache.autoDeclineOnMissingNotificationPermission) {
                    showToast(RtcUiError.MISSING_POST_NOTIFICATIONS_PERMISSION)
                    callsDelegate.decline()
                } else if (activeCallStatus != CallStatus.FINISHED && activeCallStatus != CallStatus.FINISHING && hasNotificationPermission) {
                    peerName = intent.getStringExtra(NAME_EXTRA) ?: applicationContext.getString(R.string.mm_unknown)
                    startForeground(notificationHelper.createIncomingCallNotification(this, peerName, getString(R.string.mm_incoming_call)))
                    startMedia()
                } else {
                    Log.e(TAG, "Incoming call not handled! callStatus=$activeCallStatus, hasNotificationPermission=$hasNotificationPermission")
                }
            }

            SILENT_INCOMING_CALL_ACTION -> {
                startForeground(notificationHelper.createIncomingCallNotificationSilent(this, peerName, getString(R.string.mm_incoming_call)))
            }

            CALL_ENDED_ACTION -> {
                onCallEnded()
            }

            CALL_ESTABLISHED_ACTION -> {
                stopMedia()
                startForeground(notificationHelper.createOngoingCallNotification(this, peerName, getString(R.string.mm_in_call)))
                isCallActive = true
            }

            CALL_DECLINED_ACTION -> {
                callsDelegate.decline()
                onCallEnded()
            }

            CALL_HANGUP_ACTION -> {
                callsDelegate.hangup()
                onCallEnded()
            }

            CALL_RECONNECTING_ACTION -> {
                startReconnectingTone()
            }

            CALL_RECONNECTED_ACTION -> {
                stopReconnectingToneAndPlayReconnection()
            }

            else -> Log.d(TAG, "Unhandled intent action: ${intent?.action.orEmpty()}")
        }
        return START_NOT_STICKY
    }

    private fun startForeground(notification: Notification) {
        if (hasNotificationPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(CALL_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
            } else {
                startForeground(CALL_NOTIFICATION_ID, notification)
            }
        }
    }

    private fun onCallEnded() {
        ScreenShareService.sendScreenShareServiceIntent(
            applicationContext,
            ScreenShareService.ACTION_STOP_SCREEN_SHARE
        )
        stopMedia()
        stopReconnectingTone()
        if (isCallActive){
            playCallFinishedTone()
            isCallActive = false
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startMedia() {
        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_VIBRATE -> vibrateDelegate.vibrate()
            AudioManager.RINGER_MODE_NORMAL -> {
                incomingCallRingtone.play()
                vibrateDelegate.vibrate()
            }
        }
    }

    private fun stopMedia() {
        incomingCallRingtone.stop()
        vibrateDelegate.stopVibrate()
    }

    private fun startReconnectingTone()  {
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