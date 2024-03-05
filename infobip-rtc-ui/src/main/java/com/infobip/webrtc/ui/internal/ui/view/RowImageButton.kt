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
        val labelColor = if (enabled) Injector.cache.colors?.rtcUiForeground else Injector.cache.colors?.rtcUiTextSecondary
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
        Injector.cache.colors?.rtcUiColorActionsRowIcon?.let { customizedColor ->
            val color = colorStateList?.getColorForState(intArrayOf(-android.R.attr.state_activated), customizedColor) ?: customizedColor
            binding.icon.imageTintList = ColorStateList.valueOf(color)
        }
    }

    override fun setBackgroundColor(colorStateList: ColorStateList?) {
        Injector.cache.colors?.rtcUiColorActionsRowBackground?.let { customizedColor ->
            val color = colorStateList?.getColorForState(intArrayOf(-android.R.attr.state_activated), customizedColor) ?: customizedColor
            binding.clicker.setCardBackgroundColor(color)
        }
    }

    override fun setLabelColor(color: Int?) {
        Injector.cache.colors?.let { customized ->
            val textColor = color?.let { ContextCompat.getColor(context, color) } ?: customized.rtcUiColorActionsRowLabel
            binding.label.setTextColor(textColor)
        }
    }

    fun setLabelText(text: String?) {
        binding.label.text = text
        binding.label.hide(binding.label.text.isNullOrBlank())
    }
}