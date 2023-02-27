package com.infobip.webrtc.ui.model

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.infobip.webrtc.ui.R

internal data class Colors(
        @ColorInt val rtc_ui_foreground: Int,
        @ColorInt val rtc_ui_notification: Int,
        @ColorInt val rtc_ui_actions_background: Int,
        @ColorInt val rtc_ui_actions_background_checked: Int,
        @ColorInt val rtc_ui_actions_icon: Int,
        @ColorInt val rtc_ui_actions_icon_checked: Int,
        @ColorInt val rtc_ui_text_secondary: Int,
        @ColorInt val rtc_ui_background: Int,
        @ColorInt val rtc_ui_overlay_background: Int,
        @ColorInt val rtc_ui_alert_background: Int,
        @ColorInt val rtc_ui_accept: Int,
        @ColorInt val rtc_ui_hangup: Int,
) {

    companion object {
        internal operator fun invoke(
                context: Context,
                attrs: AttributeSet?
        ): Colors {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.InfobipRtcUi, R.attr.infobipRtcUiStyle, R.style.InfobipRtcUi)
            return typedArray.let {
                Colors(
                        rtc_ui_foreground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_foreground, ContextCompat.getColor(context, R.color.rtc_ui_foreground)),
                        rtc_ui_notification = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_notification, ContextCompat.getColor(context, R.color.rtc_ui_notification)),
                        rtc_ui_actions_background = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_background, ContextCompat.getColor(context, R.color.rtc_ui_actions_background)),
                        rtc_ui_actions_background_checked = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_background_checked, ContextCompat.getColor(context, R.color.rtc_ui_actions_background_checked)),
                        rtc_ui_actions_icon = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_icon, ContextCompat.getColor(context, R.color.rtc_ui_actions_icon)),
                        rtc_ui_actions_icon_checked = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_icon_checked, ContextCompat.getColor(context, R.color.rtc_ui_actions_icon_checked)),
                        rtc_ui_text_secondary = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_text_secondary, ContextCompat.getColor(context, R.color.rtc_ui_text_secondary)),
                        rtc_ui_background = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_background, ContextCompat.getColor(context, R.color.rtc_ui_background)),
                        rtc_ui_overlay_background = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_overlay_background, ContextCompat.getColor(context, R.color.rtc_ui_overlay_background)),
                        rtc_ui_alert_background = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_alert_background, ContextCompat.getColor(context, R.color.rtc_ui_alert_background)),
                        rtc_ui_accept = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_accept, ContextCompat.getColor(context, R.color.rtc_ui_accept)),
                        rtc_ui_hangup = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_hangup, ContextCompat.getColor(context, R.color.rtc_ui_hangup)),
                )
            }.also {
                typedArray.recycle()
            }
        }
    }

}