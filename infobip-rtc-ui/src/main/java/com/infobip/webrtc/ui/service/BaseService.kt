package com.infobip.webrtc.ui.service

import android.app.Service
import android.content.Context
import com.infobip.webrtc.Injector
import com.infobip.webrtc.ui.utils.applyLocale

abstract class BaseService: Service() {

    override fun attachBaseContext(newBase: Context?) {
        val newContext = Injector.locale?.let { newBase?.applyLocale(it) } ?: newBase
        super.attachBaseContext(newContext)
    }

}