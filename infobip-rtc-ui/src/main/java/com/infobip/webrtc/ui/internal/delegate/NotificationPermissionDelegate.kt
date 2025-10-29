/*
 * NotificationPermissionDelegate.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.delegate

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import org.infobip.mobile.messaging.MobileMessaging

internal interface NotificationPermissionDelegate {
    fun request()
    fun hasPermission(): Boolean
}

internal class NotificationPermissionDelegateImpl(private val context: Context) :
    NotificationPermissionDelegate {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    override fun request() = MobileMessaging.getInstance(context).registerForRemoteNotifications()
    override fun hasPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

}