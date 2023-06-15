package com.infobip.webrtc.ui.utils

//noinspection SuspiciousImport
import android.R
import android.app.Service
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import java.util.Locale

internal fun activatedColorStateList(@ColorInt activatedColor: Int, @ColorInt color: Int) = ColorStateList(
    arrayOf(
        intArrayOf(R.attr.state_activated),
        intArrayOf(-R.attr.state_activated)
    ),
    intArrayOf(
        activatedColor,
        color
    )
)

internal fun FragmentManager.navigate(destination: Fragment, @IdRes containerId: Int) {
    val tag = destination::class.java.simpleName
    if (fragments.firstOrNull()?.tag != tag) {
        commit {
            replace(containerId, destination, tag)
        }
    }
}

@Suppress("DEPRECATION")
internal fun Service.stopForegroundRemove() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    } else {
        stopForeground(true)
    }
}

internal fun <T> T.applyIf(condition: T.() -> Boolean, block: T.() -> Unit): T {
    if (condition()) {
        block()
    }
    return this
}

@Suppress("DEPRECATION")
internal fun Context.applyLocale(locale: Locale): Context {
    val currentConfig = this.resources.configuration
    val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        currentConfig.locales.get(0)
    } else {
        currentConfig.locale
    }
    return if (currentLocale.language != locale.language) {
        Locale.setDefault(locale)
        val newConfig = Configuration(currentConfig)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newConfig.setLocale(locale)
        } else {
            newConfig.locale = locale
        }
        newConfig.setLayoutDirection(locale)
        this.createConfigurationContext(newConfig)
    } else {
        this
    }
}