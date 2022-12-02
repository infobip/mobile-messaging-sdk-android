package com.infobip.webrtc.ui.delegate

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.annotation.RequiresApi

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
        private val vibrator: VibratorManager? = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager

        override fun vibrate() {
            vibrator?.vibrate(effect)
        }

        override fun stopVibrate() {
            vibrator?.cancel()
        }

    }

    @Suppress("DEPRECATION")
    private class VibratorServiceImpl(appContext: Context, private val pattern: LongArray) : Vibrator {
        private val vibrator: android.os.Vibrator? = appContext.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator

        override fun vibrate() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                vibrator?.vibrate(pattern, 0)
            }
        }

        override fun stopVibrate() {
            vibrator?.cancel()
        }

    }
}