package org.infobip.mobile.messaging.chat.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import org.infobip.mobile.messaging.chat.mobileapi.LivechatRegistrationChecker

class PersonalizationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Handler(Looper.getMainLooper()).postDelayed({
            LivechatRegistrationChecker(context).sync()
        }, 2000L)
    }
}