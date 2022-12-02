package com.infobip.webrtc.ui.service

import android.annotation.SuppressLint
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class DefaultIncomingCallService : IncomingCallService() {
    override fun onMessageReceivedDelegate(message: RemoteMessage) {}
    override fun onNewTokenDelegate(token: String) {}
}