package com.infobip.webrtc.ui.delegate

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import org.infobip.mobile.messaging.MobileMessaging

internal interface NotificationPermissionDelegate {
    fun request()
    fun isPermissionNeeded(): Boolean
}

internal class NotificationPermissionDelegateImpl(private val context: Context) : NotificationPermissionDelegate {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    override fun request() = MobileMessaging.getInstance(context).registerForRemoteNotifications()
    override fun isPermissionNeeded(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    }

}