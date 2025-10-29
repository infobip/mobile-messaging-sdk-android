/*
 * BaseService.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.service

import android.app.Service
import android.content.Context
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.utils.applyLocale

abstract class BaseService: Service() {

    override fun attachBaseContext(newBase: Context?) {
        val newContext = Injector.cache.locale?.let { newBase?.applyLocale(it) } ?: newBase
        super.attachBaseContext(newContext)
    }

}