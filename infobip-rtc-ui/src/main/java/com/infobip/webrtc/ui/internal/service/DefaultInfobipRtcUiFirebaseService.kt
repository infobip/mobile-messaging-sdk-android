/*
 * DefaultInfobipRtcUiFirebaseService.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.service

import android.annotation.SuppressLint
import com.google.firebase.messaging.RemoteMessage
import com.infobip.webrtc.ui.service.InfobipRtcUiFirebaseService

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
internal class DefaultInfobipRtcUiFirebaseService: InfobipRtcUiFirebaseService() {
    override fun onMessageReceivedDelegate(message: RemoteMessage) {}

    override fun onNewTokenDelegate(token: String) {}
}