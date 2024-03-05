package com.infobip.webrtc.ui.internal.service

import android.annotation.SuppressLint
import com.google.firebase.messaging.RemoteMessage
import com.infobip.webrtc.ui.service.IncomingCallService

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class DefaultIncomingCallService : IncomingCallService() {
    override fun onMessageReceivedDelegate(message: RemoteMessage) {}
    override fun onNewTokenDelegate(token: String) {}
}