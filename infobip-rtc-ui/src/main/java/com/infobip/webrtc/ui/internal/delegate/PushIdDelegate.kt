/*
 * PushIdDelegate.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.delegate

import android.content.Context
import android.util.Log
import com.infobip.webrtc.ui.internal.core.TAG
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