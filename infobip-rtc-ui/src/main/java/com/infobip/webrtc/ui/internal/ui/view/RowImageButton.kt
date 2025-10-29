/*
 * RowImageButton.kt
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
import androidx.core.content.ContextCompat
import com.infobip.webrtc.ui.databinding.RowImageButtonBinding
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.utils.hide

internal class RowImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : InCallButtonAbs(context, attrs, defStyleAttr, defStyleRes) {

    private val binding: RowImageButtonBinding

    init {
        RowImageButtonBinding.inflate(LayoutInflater.from(context), this).also { binding = it }
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        with(binding) {
            icon.isActivated = checked
            val iconRes = if (checked && checkedIconResId != null) checkedIconResId else iconResId
            iconRes?.let {
                icon.setImageResource(it)
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binding.clicker.isEnabled = enabled
        val labelColor = if (enabled) Injector.cache.colors?.foreground else Injector.cache.colors?.textSecondary
        labelColor?.let { binding.label.setTextColor(it) }
    }

    override fun refreshEnabled() {
        enabledCondition?.invoke()?.let {
            isEnabled = it
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.clicker.setOnClickListener(l)
    }

    override fun setIcon(@DrawableRes iconRes: Int) {
        iconResId = iconRes
        binding.icon.setImageResource(iconRes)
    }

    override fun setIconTint(colorStateList: ColorStateList?) {
        val color: ColorStateList? = colorStateList ?: Injector.cache.colors?.actionsRowIcon?.let { ColorStateList.valueOf(it) }
        binding.icon.imageTintList = color
    }

    override fun setBackgroundColor(colorStateList: ColorStateList?) {
        val color: ColorStateList? = colorStateList ?: Injector.cache.colors?.actionsRowBackground?.let { ColorStateList.valueOf(it) }
        binding.clicker.setCardBackgroundColor(color)
    }

    override fun setLabelColor(color: Int?) {
        val textColor: Int? = color?.let { ContextCompat.getColor(context, color) } ?: Injector.cache.colors?.actionsRowLabel
        if (textColor != null)
            binding.label.setTextColor(textColor)
    }

    fun setLabelText(text: String?) {
        binding.label.text = text
        binding.label.hide(binding.label.text.isNullOrBlank())
    }
}