/*
 * InfobipRtcUiFirebaseService.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.service

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.infobip.webrtc.sdk.impl.push.PushRegistrationService
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.core.TAG
import org.infobip.mobile.messaging.cloud.firebase.MobileMessagingFirebaseService

abstract class InfobipRtcUiFirebaseService: FirebaseMessagingService() {

    abstract fun onMessageReceivedDelegate(message: RemoteMessage)
    abstract fun onNewTokenDelegate(token: String)

    override fun onMessageReceived(message: RemoteMessage) {
        if (Companion.onMessageReceived(this, message)) {
            Log.d(TAG, "RemoteMessage handled by InfobipRtcUiFirebaseService")
            return
        }
        if (MobileMessagingFirebaseService.onMessageReceived(this, message)) {
            Log.d(TAG, "RemoteMessage handled by MobileMessagingFirebaseService")
            return
        }
        onMessageReceivedDelegate(message)
        Log.d(TAG, "RemoteMessage delegated to ${this::class.java.simpleName}")
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "On new FCM token: $token")
        Companion.onNewToken(this, token)
        MobileMessagingFirebaseService.onNewToken(this, token)
        onNewTokenDelegate(token)
    }


    companion object {

        @JvmStatic
        fun onMessageReceived(context: Context, message: RemoteMessage): Boolean {
            //Note: Service can be calls entry point therefore initialize InfobipRtcUi with app context
            //Do not initialize it in onCreate because MM sdk may not be initialized yet
            Injector.getWebrtcUi(context)
            return Injector.callsDelegate.handleIncomingCall(message.data)
        }

        @JvmStatic
        fun onNewToken(context: Context, token: String) {
            PushRegistrationService.updatePushRegistration(context, token)
        }

    }

}