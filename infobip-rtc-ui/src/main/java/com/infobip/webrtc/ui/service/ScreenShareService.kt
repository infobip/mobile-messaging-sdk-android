package com.infobip.webrtc.ui.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.infobip.webrtc.Injector
import com.infobip.webrtc.TAG
import com.infobip.webrtc.ui.notifications.CallNotificationFactory
import com.infobip.webrtc.ui.notifications.SCREEN_SHARE_NOTIFICATION_ID
import com.infobip.webrtc.ui.utils.stopForegroundRemove

class ScreenShareService : BaseService() {
    private val notificationHelper: CallNotificationFactory by lazy { Injector.notificationFactory }

    companion object {

        fun sendScreenShareServiceIntent(context: Context, action: String) {
            context.startService(Intent(context, ScreenShareService::class.java).apply { setAction(action) })
        }

        const val ACTION_START_SCREEN_SHARE = "com.infobip.calls.ui.ACTION_START_SCREEN_SHARE"
        const val ACTION_STOP_SCREEN_SHARE = "com.infobip.calls.ui.ACTION_STOP_SCREEN_SHARE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Handle action: ${intent?.action.orEmpty()}")
        when (intent?.action) {
            ACTION_START_SCREEN_SHARE -> {
                startForeground(
                        SCREEN_SHARE_NOTIFICATION_ID,
                        notificationHelper.createScreenSharingNotification(this),
                )
            }
            ACTION_STOP_SCREEN_SHARE -> {
                stopForegroundRemove()
                stopSelf()
            }
            else -> Log.d(TAG, "Unhandled intent action: ${intent?.action.orEmpty()}")
        }
        return START_NOT_STICKY
    }
}