/*
 * PushIdDelegate.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.delegate

import android.content.Context
import com.infobip.webrtc.ui.logging.RtcUiLogger
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
        }.onFailure { RtcUiLogger.e("Failed to obtain push registration id.", throwable = it) }
            .getOrNull()
    }

}
