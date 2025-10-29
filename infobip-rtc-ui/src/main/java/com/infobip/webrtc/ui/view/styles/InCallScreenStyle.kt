/*
 * InCallScreenStyle.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.view.styles

import android.content.Context
import android.util.AttributeSet
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.internal.utils.resolveString

/**
 * Style allows you to customize the appearance of in call screen.
 */
data class InCallScreenStyle @JvmOverloads constructor(
    val callerName: String? = null,
) {

    companion object {
        internal operator fun invoke(
            context: Context,
            attrs: AttributeSet?
        ): InCallScreenStyle {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.InfobipRtcUi, R.attr.infobipRtcUiStyle, R.style.InfobipRtcUi)
            return typedArray.run {
                InCallScreenStyle(
                    callerName = resolveString(R.styleable.InfobipRtcUi_rtc_ui_in_call_caller_name, context),
                )
            }.also {
                typedArray.recycle()
            }
        }
    }
}