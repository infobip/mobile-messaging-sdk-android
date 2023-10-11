package com.infobip.webrtc.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
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

    init {
        WidgetCallAlertBinding.inflate(LayoutInflater.from(context), this).apply {
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

}