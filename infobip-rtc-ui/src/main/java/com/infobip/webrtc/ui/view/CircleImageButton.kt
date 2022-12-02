package com.infobip.webrtc.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Checkable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StyleableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.activatedColorStateList
import com.infobip.webrtc.ui.databinding.WidgetCircleImageButtonBinding

class CircleImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes), Checkable {

    private val binding: WidgetCircleImageButtonBinding
    private var _isChecked: Boolean = false
    private var iconResId: Int? = null
    private var checkedIconResId: Int? = null

    init {
        WidgetCircleImageButtonBinding.inflate(LayoutInflater.from(context), this).also { binding = it }
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.CircleImageButton)
        iconResId = styledAttrs.getResourceId(R.styleable.CircleImageButton_icon, 0).takeIf { it > 0 }
        checkedIconResId = styledAttrs.getResourceId(R.styleable.CircleImageButton_checkedIcon, 0).takeIf { it > 0 }
        with(binding) {
            iconResId?.let {
                icon.apply {
                    setImageResource(it)
                    setActivatedColorList(context, styledAttrs, R.styleable.CircleImageButton_iconColor, R.styleable.CircleImageButton_checkedIconColor) {
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
                setCircleColor(it)
            }
        }
    }

    fun setCircleColor(colorStateList: ColorStateList?) {
        binding.circleBackground.imageTintList = colorStateList
    }

    private fun ImageView.setActivatedColorList(
        context: Context,
        typedArray: TypedArray,
        @StyleableRes colorStyleableRes: Int,
        @StyleableRes activatedColorStyleableRes: Int,
        applyColorList: (ColorStateList) -> Unit
    ) {
        typedArray.getResourceId(colorStyleableRes, 0).takeIf { it > 0 }?.let { color ->
            typedArray.getResourceId(activatedColorStyleableRes, 0).takeIf { it > 0 }?.let { checkedColor ->
                applyColorList(
                    activatedColorStateList(
                        ContextCompat.getColor(context, checkedColor),
                        ContextCompat.getColor(context, color)
                    )
                )
            } ?: run { applyColorList(ColorStateList.valueOf(ContextCompat.getColor(context, color))) }
        }
    }

    override fun setChecked(checked: Boolean) {
        _isChecked = checked
        with(binding) {
            circleBackground.isActivated = checked
            icon.isActivated = checked
            val iconRes = if (checked && checkedIconResId != null) checkedIconResId else iconResId
            iconRes?.let {
                icon.setImageResource(it)
            }
        }
    }

    override fun isChecked(): Boolean = _isChecked

    override fun toggle() {
        isChecked = !_isChecked
    }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.circleBackground.setOnClickListener(l)
    }

    fun setIcon(@DrawableRes iconRes: Int) {
        iconResId = iconRes
        binding.icon.setImageResource(iconRes)
    }

    fun setIconTint(colorStateList: ColorStateList?) {
        binding.icon.imageTintList = colorStateList
    }

}