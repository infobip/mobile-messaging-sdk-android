package com.infobip.webrtc.ui.internal.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.databinding.WidgetCircleImageButtonBinding
import com.infobip.webrtc.ui.internal.core.Injector
import com.infobip.webrtc.ui.internal.utils.activatedColorStateList

internal class CircleImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : InCallButtonAbs(context, attrs, defStyleAttr, defStyleRes) {

    private val binding: WidgetCircleImageButtonBinding
    private val actionsBackgroundColorStateList by lazy {
        Injector.cache.colors?.let {
            activatedColorStateList(
                it.actionsBackgroundChecked,
                it.actionsBackground
            )
        }
    }
    private val actionsIconColorStateList by lazy {
        Injector.cache.colors?.let {
            activatedColorStateList(
                it.actionsIconChecked,
                it.actionsIcon
            )
        }
    }

    init {
        WidgetCircleImageButtonBinding.inflate(LayoutInflater.from(context), this).also { binding = it }
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.CircleImageButton)
        iconResId = styledAttrs.getResourceId(R.styleable.CircleImageButton_circleBtnIcon, 0).takeIf { it > 0 }
        checkedIconResId = styledAttrs.getResourceId(R.styleable.CircleImageButton_circleBtnCheckedIcon, 0).takeIf { it > 0 }
        with(binding) {
            iconResId?.let {
                icon.apply {
                    setImageResource(it)
                    setActivatedColorList(
                        context,
                        styledAttrs,
                        R.styleable.CircleImageButton_circleBtnIconColor,
                        R.styleable.CircleImageButton_circleBtnCheckedIconColor
                    ) {
                        imageTintList = it
                    }
                }
            }

            circleBackground.setActivatedColorList(
                context,
                styledAttrs,
                R.styleable.CircleImageButton_circleBackground,
                R.styleable.CircleImageButton_checkedCircleBackground
            ) {
                binding.circleBackground.imageTintList = it
            }
        }
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        with(binding) {
            circleBackground.isActivated = checked
            icon.isActivated = checked
            val iconRes = if (checked && checkedIconResId != null) checkedIconResId else iconResId
            iconRes?.let {
                icon.setImageResource(it)
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binding.circleBackground.isEnabled = enabled
    }

    override fun refreshEnabled() {
        enabledCondition?.invoke()?.let {
            isEnabled = it
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.circleBackground.setOnClickListener(l)
    }

    override fun setIcon(@DrawableRes iconRes: Int) {
        iconResId = iconRes
        binding.icon.setImageResource(iconRes)
    }

    override fun setIconTint(colorStateList: ColorStateList?) {
        val stateList = colorStateList ?: actionsIconColorStateList
        binding.icon.imageTintList = stateList
    }

    override fun setBackgroundColor(colorStateList: ColorStateList?) {
        val stateList = colorStateList ?: actionsBackgroundColorStateList
        binding.circleBackground.imageTintList = stateList
    }
}