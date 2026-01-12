/*
 * ErrorView.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.databinding.IbViewErrorBinding
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils
import org.infobip.mobile.messaging.chat.utils.show
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle

class ErrorView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
) : ConstraintLayout(context, attributes, defStyle, defStyleRes) {

    private val binding = IbViewErrorBinding.inflate(LayoutInflater.from(context), this)
    private val localizationUtils = LocalizationUtils.getInstance(context)
    private val defaultDescription = localizationUtils.getString(R.string.ib_chat_error_description)
    private var reason: String? = null

    fun setAction(clickAction: OnClickListener) {
        binding.ibLcErrorViewActionBtn.setOnClickListener(clickAction)
    }

    fun setReason(reason: String?) {
        this.reason = reason
        updateDescription(binding.ibLcErrorViewDesc.text.toString(), reason)
    }

    @SuppressLint("SetTextI18n")
    private fun updateDescription(description: String, reason: String?) {
        binding.ibLcErrorViewDesc.text = if (description.startsWith(defaultDescription) && reason?.isNotBlank() == true) {
            defaultDescription + "\n$reason"
        } else {
            description
        }
    }

    fun applyStyle(style: InAppChatStyle) {
        with(binding) {
            setBackgroundColor(style.errorBackgroundColor)
            val titleText = style.errorTitleTextRes?.let { localizationUtils.getString(it) } ?: style.errorTitleText
            titleText?.let { ibLcErrorViewTitle.text = it }
            style.errorTitleTextAppearance?.let { TextViewCompat.setTextAppearance(ibLcErrorViewTitle, it) }
            ibLcErrorViewTitle.setTextColor(style.errorTitleTextColor)
            val descriptionText = style.errorDescriptionTextRes?.let { localizationUtils.getString(it) } ?: style.errorDescriptionText
            descriptionText?.let { updateDescription(it, this@ErrorView.reason) }
            style.errorDescriptionTextAppearance?.let { TextViewCompat.setTextAppearance(ibLcErrorViewDesc, it) }
            ibLcErrorViewDesc.setTextColor(style.errorDescriptionTextColor)
            style.errorIcon?.let { ibLcErrorViewImage.setImageDrawable(it) }
            style.errorIconTint?.let { ibLcErrorViewImage.setColorFilter(it) }
            val refreshButtonText = style.errorRefreshButtonTextRes?.let { localizationUtils.getString(it) } ?: style.errorRefreshButtonText
            refreshButtonText?.let { ibLcErrorViewActionBtn.text = it }
            ibLcErrorViewActionBtn.setTextColor(style.errorRefreshButtonTextColor)
            ibLcErrorViewActionBtn.show(style.errorRefreshButtonVisible)
        }
    }

}
