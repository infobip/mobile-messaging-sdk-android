package com.infobip.webrtc.ui.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.infobip.webrtc.Injector
import com.infobip.webrtc.TAG
import com.infobip.webrtc.ui.delegate.CallsDelegate
import org.infobip.mobile.messaging.cloud.firebase.MobileMessagingFirebaseService

abstract class IncomingCallService : FirebaseMessagingService() {
    private val callsDelegate: CallsDelegate by lazy { Injector.callsDelegate }

    abstract fun onMessageReceivedDelegate(message: RemoteMessage)
    abstract fun onNewTokenDelegate(token: String)

    override fun onCreate() {
        Injector.getWebrtcUi(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (MobileMessagingFirebaseService.onMessageReceived(this, message))
            return
        if (callsDelegate.isIncomingCall(message.data)) {
            Log.d(TAG, "Incoming call push message received $message")
            callsDelegate.handlePushMessage(message.data)
        } else
            onMessageReceivedDelegate(message)
    }

    override fun onNewToken(token: String) {
        MobileMessagingFirebaseService.onNewToken(this, token)
        onNewTokenDelegate(token)
    }
}