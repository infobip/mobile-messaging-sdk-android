package com.infobip.webrtc.ui.service

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
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

        var isRunning = false

        fun sendScreenShareServiceIntent(context: Context, action: String) {
            context.startService(Intent(context, ScreenShareService::class.java).apply {
                setAction(
                    action
                )
            })
        }

        const val ACTION_START_SCREEN_SHARE = "com.infobip.calls.ui.ACTION_START_SCREEN_SHARE"
        const val ACTION_STOP_SCREEN_SHARE = "com.infobip.calls.ui.ACTION_STOP_SCREEN_SHARE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Handle action: ${intent?.action.orEmpty()}")
        when (intent?.action) {
            ACTION_START_SCREEN_SHARE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        SCREEN_SHARE_NOTIFICATION_ID,
                        notificationHelper.createScreenSharingNotification(this),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(
                        SCREEN_SHARE_NOTIFICATION_ID,
                        notificationHelper.createScreenSharingNotification(this)
                    )
                }
                isRunning = true
            }

            ACTION_STOP_SCREEN_SHARE -> {
                stopForegroundRemove()
                stopSelf()
                isRunning = false
            }

            else -> Log.d(TAG, "Unhandled intent action: ${intent?.action.orEmpty()}")
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }
}