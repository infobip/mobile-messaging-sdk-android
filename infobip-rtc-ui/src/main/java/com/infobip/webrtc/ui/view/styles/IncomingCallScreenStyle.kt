package com.infobip.webrtc.ui.view.styles

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import com.infobip.webrtc.ui.R
import com.infobip.webrtc.ui.internal.utils.resolveString

/**
 * Style allows you to customize the appearance of incoming call screen.
 *
 * Properties [headlineText] and [messageText] affects also behaviour of incoming call notification action - `accept`.
 * If any of the properties is set, the action won't accept call,
 * but will show incoming call screen to make sure user can see the [headlineText] and [messageText] before call is established.
 * Otherwise, the action will accept the call immediately and call screen is opened.
 */
data class IncomingCallScreenStyle @JvmOverloads constructor(
    val headlineText: String? = null,
    @StyleRes val headlineTextAppearance: Int? = null,
    @ColorInt val headlineTextColor: Int = Defaults.textColor,
    @DrawableRes val headlineBackground: Int? = null,
    val messageText: String? = null,
    @StyleRes val messageTextAppearance: Int? = null,
    @ColorInt val messageTextColor: Int = Defaults.textColor,
    @DrawableRes val messageBackground: Int? = null,
    val callerName: String? = null,
    @StyleRes val callerNameAppearance: Int? = null,
    val callerIconVisible: Boolean = Defaults.callerIconVisible,
) {

    object Defaults {
        const val textColor = Color.WHITE
        const val callerIconVisible: Boolean = true
    }

    companion object {
        internal operator fun invoke(
            context: Context,
            attrs: AttributeSet?
        ): IncomingCallScreenStyle {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.InfobipRtcUi, R.attr.infobipRtcUiStyle, R.style.InfobipRtcUi)
            return typedArray.run {
                IncomingCallScreenStyle(
                    headlineText = resolveString(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_headline, context),
                    headlineTextAppearance = getResourceId(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_headline_appearance, 0).takeIf { it != 0 },
                    headlineTextColor = getColor(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_headline_text_color, Defaults.textColor),
                    headlineBackground = getResourceId(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_headline_background, 0).takeIf { it != 0 },
                    messageText = resolveString(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_message, context),
                    messageTextAppearance = getResourceId(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_message_appearance, 0).takeIf { it != 0 },
                    messageTextColor = getColor(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_message_text_color, Defaults.textColor),
                    messageBackground = getResourceId(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_message_background, 0).takeIf { it != 0 },
                    callerName = resolveString(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_caller_name, context),
                    callerNameAppearance = getResourceId(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_caller_name_appearance, 0).takeIf { it != 0 },
                    callerIconVisible = getBoolean(R.styleable.InfobipRtcUi_rtc_ui_incoming_call_caller_icon_visible, Defaults.callerIconVisible)
                )
            }.also {
                typedArray.recycle()
            }
        }
    }
}