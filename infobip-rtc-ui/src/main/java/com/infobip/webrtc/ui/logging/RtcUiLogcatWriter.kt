/*
 * RtcUiLogcatWriter.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.logging

import android.util.Log

/**
 * Default [RtcUiWriter] implementation that outputs to Android Logcat.
 */
class RtcUiLogcatWriter : RtcUiWriter {

    override fun write(level: RtcUiLogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            RtcUiLogLevel.VERBOSE -> Log.v(tag, message, throwable)
            RtcUiLogLevel.DEBUG -> Log.d(tag, message, throwable)
            RtcUiLogLevel.INFO -> Log.i(tag, message, throwable)
            RtcUiLogLevel.WARN -> Log.w(tag, message, throwable)
            RtcUiLogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }
}
