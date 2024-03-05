package com.infobip.webrtc.ui.internal.delegate

import android.content.Context
import org.infobip.mobile.messaging.MobileMessagingCore

internal interface AppCodeDelegate {
    fun getApplicationCode(): String?
}

internal class AppCodeDelegateImpl(
        private val context: Context,
) : AppCodeDelegate {

    override fun getApplicationCode(): String? {
        return MobileMessagingCore.getApplicationCode(context)
    }
}