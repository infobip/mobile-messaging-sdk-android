package com.infobip.webrtc.ui.internal.service

import android.annotation.SuppressLint
import com.google.firebase.messaging.RemoteMessage
import com.infobip.webrtc.ui.service.InfobipRtcUiFirebaseService

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
internal class DefaultInfobipRtcUiFirebaseService: InfobipRtcUiFirebaseService() {
    override fun onMessageReceivedDelegate(message: RemoteMessage) {}

    override fun onNewTokenDelegate(token: String) {}
}