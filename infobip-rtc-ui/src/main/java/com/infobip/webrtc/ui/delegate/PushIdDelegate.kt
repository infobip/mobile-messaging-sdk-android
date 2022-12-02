package com.infobip.webrtc.ui.delegate

import android.content.Context
import android.util.Log
import com.infobip.webrtc.TAG
import org.infobip.mobile.messaging.MobileMessaging

internal interface PushIdDelegate {
    fun getPushRegistrationId(): String?
}

internal class PushIdDelegateImpl(private val context: Context) : PushIdDelegate {

    override fun getPushRegistrationId(): String? {
        return runCatching {
            MobileMessaging.getInstance(context)
                .installation
                .pushRegistrationId
        }.onFailure { Log.e(TAG, "Failed to obtain push registration id.", it) }
            .getOrNull()
    }

}