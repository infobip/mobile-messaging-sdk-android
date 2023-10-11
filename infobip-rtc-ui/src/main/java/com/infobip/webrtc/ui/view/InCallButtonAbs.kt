package com.infobip.webrtc.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import androidx.annotation.CallSuper
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.infobip.webrtc.ui.utils.activatedColorStateList

abstract class InCallButtonAbs(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(
    context,
    attrs,
    defStyleAttr,
    defStyleRes
), Checkable {

    private var _isChecked: Boolean = false
    protected var iconResId: Int? = null
    protected var checkedIconResId: Int? = null
    var checkedCondition: (() -> Boolean)? = null
    var enabledCondition: (() -> Boolean)? = null

    abstract fun setBackgroundColor(colorStateList: ColorStateList? = null)

    abstract fun setIconTint(colorStateList: ColorStateList? = null)

    abstract fun setIcon(@DrawableRes iconRes: Int)

    open fun setCheckedIcon(@DrawableRes checkedIconRes: Int) {
        this.checkedIconResId = checkedIconRes
    }

    open fun setLabelColor(@ColorRes color: Int? = null) {
    }

    override fun isChecked(): Boolean = _isChecked

    @CallSuper
    override fun setChecked(checked: Boolean) {
        _isChecked = checked
    }

    override fun toggle() {
        isChecked = !_isChecked
    }

    abstract fun refreshEnabled()
    fun refreshChecked() {
        checkedCondition?.let {
            isChecked = it.invoke()
        }
    }

    protected fun View.setActivatedColorList(
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
}

