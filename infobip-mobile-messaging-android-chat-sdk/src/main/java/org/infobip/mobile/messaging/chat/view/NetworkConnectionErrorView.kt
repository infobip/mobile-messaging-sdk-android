/*
 * NetworkConnectionErrorView.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2026 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import org.infobip.mobile.messaging.chat.databinding.IbViewNetworkConnectionErrorBinding
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils
import org.infobip.mobile.messaging.chat.utils.show
import org.infobip.mobile.messaging.chat.utils.toColorStateList
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle

class NetworkConnectionErrorView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayout(context, attributes, defStyle, defStyleRes) {

    private val binding = IbViewNetworkConnectionErrorBinding.inflate(LayoutInflater.from(context), this)
    private val localizationUtils = LocalizationUtils.getInstance(context)

    fun applyStyle(style: InAppChatStyle) {
        with(binding) {
            setBackgroundColor(style.networkConnectionErrorBackgroundColor)
            val errorText = style.networkConnectionErrorTextRes?.let { localizationUtils.getString(it) } ?: style.networkConnectionErrorText
            errorText?.let { ibLcConnectionError.text = it }
            style.networkConnectionErrorTextAppearance?.let { TextViewCompat.setTextAppearance(ibLcConnectionError, it) }
            ibLcConnectionError.setTextColor(style.networkConnectionErrorTextColor)
            style.networkConnectionErrorIcon?.let { ibLcConnectionErrorIcon.setImageDrawable(it) }
            ibLcConnectionErrorIcon.imageTintList = style.networkConnectionErrorIconTint.toColorStateList()
            ibLcConnectionErrorIcon.show(style.networkConnectionErrorIconVisible)
        }
    }

}