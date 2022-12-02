package com.infobip.webrtc.ui.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.IBinder
import android.util.Log
import com.infobip.webrtc.Injector
import com.infobip.webrtc.TAG
import com.infobip.webrtc.sdk.api.call.CallStatus
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.delegate.CallsDelegate
import com.infobip.webrtc.ui.delegate.Vibrator
import com.infobip.webrtc.ui.notifications.CALL_NOTIFICATION_ID
import com.infobip.webrtc.ui.notifications.CallNotificationFactory
import com.infobip.webrtc.ui.stopForegroundRemove

class OngoingCallService : Service() {

    companion object {

        fun sendCallServiceIntent(context: Context, action: String) {
            context.startService(Intent(context, OngoingCallService::class.java).apply { setAction(action) })
        }

        const val INCOMING_CALL_ACTION = "com.infobip.calls.ui.service.OngoingCallService.INCOMING_CALL_ACTION"
        const val CALL_ENDED_ACTION = "com.infobip.calls.ui.service.OngoingCallService.END_CALL_ACTION"
        const val CALL_ESTABLISHED_ACTION = "com.infobip.calls.ui.service.OngoingCallService.CALL_ESTABLISHED_ACTION"
        const val CALL_DECLINED_ACTION = "com.infobip.calls.ui.service.OngoingCallService.CALL_DECLINED_ACTION"
        const val CALL_HANGUP_ACTION = "com.infobip.calls.ui.service.OngoingCallService.CALL_HANGUP_ACTION"
        const val NAME_EXTRA = "com.infobip.calls.ui.service.OngoingCallService.NAME_EXTRA"
        const val CALL_STATUS_EXTRA = "com.infobip.calls.ui.service.OngoingCallService.CALL_STATUS_EXTRA"
    }

    private val vibrateDelegate: Vibrator by lazy { Injector.vibrator }

    //todo custom ringtone uri
    private val incomingCallRingtone: Ringtone by lazy { RingtoneManager.getRingtone(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)) }
    private val audioManager: AudioManager by lazy { applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val notificationHelper: CallNotificationFactory by lazy { Injector.notificationFactory }
    private val callsDelegate: CallsDelegate by lazy { Injector.callsDelegate }
    private var peerName: String = ""
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Handle action: ${intent?.action.orEmpty()}")
        when (intent?.action) {
            INCOMING_CALL_ACTION -> {
                val activeCallStatus = runCatching { CallStatus.valueOf(intent.getStringExtra(CALL_STATUS_EXTRA).orEmpty()) }.getOrDefault(CallStatus.FINISHED)
                if (activeCallStatus != CallStatus.FINISHED && activeCallStatus != CallStatus.FINISHING) {
                    peerName = intent.getStringExtra(NAME_EXTRA) ?: applicationContext.getString(R.string.mm_unknown)
                    startForeground(CALL_NOTIFICATION_ID, notificationHelper.createIncomingCallNotification(peerName, getString(R.string.mm_incoming_call)))
                    startMedia()
                }
            }
            CALL_ENDED_ACTION -> {
                onCallEnded()
            }
            CALL_ESTABLISHED_ACTION -> {
                stopMedia()
                startForeground(CALL_NOTIFICATION_ID, notificationHelper.createOngoingCallNotification(peerName, getString(R.string.mm_in_call)))
            }
            CALL_DECLINED_ACTION -> {
                callsDelegate.decline()
                onCallEnded()
            }
            CALL_HANGUP_ACTION -> {
                callsDelegate.hangup()
                stop()
            }
            else -> Log.d(TAG, "Unhandled intent action: ${intent?.action.orEmpty()}")
        }
        return START_NOT_STICKY
    }

    private fun stop() {
        stopForegroundRemove()
        stopSelf()
    }

    private fun onCallEnded() {
        if (callsDelegate.hasScreenShare()){
            ScreenShareService.sendScreenShareServiceIntent(applicationContext, ScreenShareService.ACTION_STOP_SCREEN_SHARE)
        }
        stopMedia()
        stop()
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
}