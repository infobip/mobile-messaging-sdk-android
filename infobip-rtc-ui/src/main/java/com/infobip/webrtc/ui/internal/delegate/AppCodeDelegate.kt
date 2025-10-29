/*
 * AppCodeDelegate.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
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