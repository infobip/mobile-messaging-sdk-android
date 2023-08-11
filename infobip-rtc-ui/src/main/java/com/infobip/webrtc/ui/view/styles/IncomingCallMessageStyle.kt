package com.infobip.webrtc.ui.view.styles

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.utils.resolveString

internal data class IncomingCallMessageStyle(
    val headlineText: String? = null,
    @StyleRes val headlineTextAppearance: Int? = null,
    @ColorInt val headlineTextColor: Int,
    @DrawableRes val headlineBackground: Int?,
    val messageText: String? = null,
    @StyleRes val messageTextAppearance: Int? = null,
    @ColorInt val messageTextColor: Int,
    @DrawableRes val messageBackground: Int?,
    ) {

    companion object {
        internal operator fun invoke(
            context: Context,
            attrs: AttributeSet?
        ): IncomingCallMessageStyle {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.InfobipRtcUi, R.attr.infobipRtcUiStyle, R.style.InfobipRtcUi)
            return typedArray.run {
                IncomingCallMessageStyle(
                    headlineText = resolveString(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_headline, context),
                    headlineTextAppearance = getResourceId(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_headline_appearance, 0).takeIf { it != 0 },
                    headlineTextColor = getColor(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_headline_text_color, ContextCompat.getColor(context, R.color.rtc_ui_foreground)),
                    headlineBackground = getResourceId(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_headline_background, 0).takeIf { it != 0 },
                    messageText = resolveString(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_message, context),
                    messageTextAppearance = getResourceId(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_message_appearance, 0).takeIf { it != 0 },
                    messageTextColor = getColor(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_message_text_color, ContextCompat.getColor(context, R.color.rtc_ui_foreground)),
                    messageBackground = getResourceId(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_message_background, 0).takeIf { it != 0 },
                )
            }.also {
                typedArray.recycle()
            }
        }
    }
}