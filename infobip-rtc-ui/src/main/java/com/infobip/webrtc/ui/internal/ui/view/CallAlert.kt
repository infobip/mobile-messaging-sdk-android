/*
 * CallAlert.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.internal.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.databinding.WidgetCallAlertBinding
import com.infobip.webrtc.ui.internal.core.Injector

internal class CallAlert @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding = WidgetCallAlertBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.run {
            Injector.cache.colors?.let {
                setBackgroundColor(it.alertBackground)
                alertText.setTextColor(it.alertText)
            }
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CallAlert)
            alertText.text = typedArray.getText(R.styleable.CallAlert_alertText)
            val iconResId = typedArray.getResourceId(R.styleable.CallAlert_alertIcon, 0)
            if (iconResId != 0)
                alertIcon.setImageResource(iconResId)
            val iconTint = typedArray.getColor(R.styleable.CallAlert_alertIconTint, 0)
            if (iconTint != 0)
                alertIcon.imageTintList = ColorStateList.valueOf(iconTint)
            typedArray.recycle()
        }
    }

    sealed class Mode(@StringRes val message: Int, @DrawableRes val icon: Int) {
        object WeakConnection : Mode(R.string.mm_call_weak_internet_connection, R.drawable.ic_alert_triangle)
        object Reconnecting : Mode(R.string.mm_connection_problems, R.drawable.ic_alert_triangle)
        object DisabledMic : Mode(R.string.mm_your_microphone_is_muted, R.drawable.ic_mic_off)
    }

    fun setMode(mode: Mode?) {
        if (mode == null)
            return
        with(binding) {
            alertText.text = context.getString(mode.message)
            alertIcon.setImageResource(mode.icon)
        }
    }
}