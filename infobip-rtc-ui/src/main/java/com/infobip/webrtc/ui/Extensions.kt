package com.infobip.webrtc.ui

//noinspection SuspiciousImport
import android.R
import android.app.Service
import android.content.res.ColorStateList
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit

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

fun <T> T.applyIf(condition: T.() -> Boolean, block: T.() -> Unit): T {
    if (condition()) {
        block()
    }
    return this
}