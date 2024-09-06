package com.infobip.webrtc.ui.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.core.TAG
import com.infobip.webrtc.ui.internal.delegate.CallsDelegate
import org.infobip.mobile.messaging.cloud.firebase.MobileMessagingFirebaseService

@Deprecated("Use InfobipRtcUiFirebaseService instead.", ReplaceWith("InfobipRtcUiFirebaseService", "com.infobip.webrtc.ui.service.InfobipRtcUiFirebaseService"))
abstract class IncomingCallService : FirebaseMessagingService() {
    private val callsDelegate: CallsDelegate by lazy { Injector.callsDelegate }

    abstract fun onMessageReceivedDelegate(message: RemoteMessage)
    abstract fun onNewTokenDelegate(token: String)

    override fun onMessageReceived(message: RemoteMessage) {
        if (MobileMessagingFirebaseService.onMessageReceived(this, message))
            return
        //Note: Service can be calls entry point therefore initialize webrtcui with app context
        //Do not initialize it in onCreate because MM sdk may not be initialized yet
        Injector.getWebrtcUi(applicationContext)
        if (!callsDelegate.handleIncomingCall(message.data))
            onMessageReceivedDelegate(message)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "On new FCM token: $token")
        MobileMessagingFirebaseService.onNewToken(this, token)
        onNewTokenDelegate(token)
    }
}