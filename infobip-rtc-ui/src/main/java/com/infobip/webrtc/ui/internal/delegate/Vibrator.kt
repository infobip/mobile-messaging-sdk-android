/*
 * Vibrator.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.delegate

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import android.os.Vibrator as VibratorService
internal interface Vibrator {
    fun vibrate()
    fun stopVibrate()
}

internal class VibratorImpl(appContext: Context) : Vibrator {
    private val pattern = longArrayOf(0, 500, 1000)
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) VibratorManagerImpl(appContext, pattern) else VibratorServiceImpl(appContext, pattern)

    override fun vibrate() {
        vibrator.vibrate()
    }

    override fun stopVibrate() {
        vibrator.stopVibrate()
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private class VibratorManagerImpl(appContext: Context, pattern: LongArray) : Vibrator {
        private val effect = CombinedVibration.createParallel(VibrationEffect.createWaveform(pattern, 0))
        private val vibratorManager: VibratorManager? = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager

        override fun vibrate() {
            vibratorManager?.vibrate(effect)
        }

        override fun stopVibrate() {
            vibratorManager?.cancel()
        }

    }

    @Suppress("DEPRECATION")
    private class VibratorServiceImpl(appContext: Context, private val pattern: LongArray) :
        Vibrator {
        private val vibratorService: VibratorService? = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? VibratorService

        override fun vibrate() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibratorService?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                vibratorService?.vibrate(pattern, 0)
            }
        }

        override fun stopVibrate() {
            vibratorService?.cancel()
        }

    }
}