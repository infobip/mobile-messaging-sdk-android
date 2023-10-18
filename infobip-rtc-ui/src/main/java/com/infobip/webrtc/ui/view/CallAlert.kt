package com.infobip.webrtc.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.infobip.webrtc.Injector
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.databinding.WidgetCallAlertBinding

class CallAlert @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding = WidgetCallAlertBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.run {
            Injector.colors?.let {
                setBackgroundColor(it.rtcUiAlertBackground)
                alertText.setTextColor(it.rtcUiAlertText)
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