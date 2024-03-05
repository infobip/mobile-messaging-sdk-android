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