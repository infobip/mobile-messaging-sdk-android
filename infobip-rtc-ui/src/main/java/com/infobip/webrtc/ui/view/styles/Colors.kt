package com.infobip.webrtc.ui.view.styles

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.infobip.webrtc.ui.R

data class Colors(
    @ColorInt val rtcUiForeground: Int,
    @ColorInt val rtcUiActionsBackground: Int,
    @ColorInt val rtcUiActionsBackgroundChecked: Int,
    @ColorInt val rtcUiActionsIcon: Int,
    @ColorInt val rtcUiActionsIconChecked: Int,
    @ColorInt val rtcUiTextSecondary: Int,
    @ColorInt val rtcUiBackground: Int,
    @ColorInt val rtcUiOverlayBackground: Int,
    @ColorInt val rtcUiAlertBackground: Int,
    @ColorInt val rtcUiAlertText: Int,
    @ColorInt val rtcUiAccept: Int,
    @ColorInt val rtcUiHangup: Int,
    @ColorInt val rtcUiColorActionsRowBackground: Int,
    @ColorInt val rtcUiColorActionsRowIcon: Int,
    @ColorInt val rtcUiColorActionsRowLabel: Int,
    @ColorInt val rtcUiColorSheetPill: Int,
    @ColorInt val rtcUiColorActionsDivider: Int,
    @ColorInt val rtcUiColorSheetBackground: Int,
) {

    companion object {
        internal operator fun invoke(
                context: Context,
                attrs: AttributeSet?
        ): Colors {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.InfobipRtcUi, R.attr.infobipRtcUiStyle, R.style.InfobipRtcUi)
            return typedArray.let {
                Colors(
                        rtcUiForeground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_foreground, ContextCompat.getColor(context, R.color.rtc_ui_foreground)),
                        rtcUiActionsBackground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_background, ContextCompat.getColor(context, R.color.rtc_ui_actions_background)),
                        rtcUiActionsBackgroundChecked = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_background_checked, ContextCompat.getColor(context, R.color.rtc_ui_actions_background_checked)),
                        rtcUiActionsIcon = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_icon, ContextCompat.getColor(context, R.color.rtc_ui_actions_icon)),
                        rtcUiActionsIconChecked = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_icon_checked, ContextCompat.getColor(context, R.color.rtc_ui_actions_icon_checked)),
                        rtcUiTextSecondary = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_text_secondary, ContextCompat.getColor(context, R.color.rtc_ui_text_secondary)),
                        rtcUiBackground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_background, ContextCompat.getColor(context, R.color.rtc_ui_background)),
                        rtcUiOverlayBackground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_overlay_background, ContextCompat.getColor(context, R.color.rtc_ui_overlay_background)),
                        rtcUiAlertBackground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_alert_background, ContextCompat.getColor(context, R.color.rtc_ui_alert_background)),
                        rtcUiAlertText = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_alert_text, ContextCompat.getColor(context, R.color.rtc_ui_foreground)),
                        rtcUiAccept = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_accept, ContextCompat.getColor(context, R.color.rtc_ui_accept)),
                        rtcUiHangup = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_hangup, ContextCompat.getColor(context, R.color.rtc_ui_hangup)),
                        rtcUiColorActionsRowBackground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_row_background, ContextCompat.getColor(context, R.color.rtc_ui_background)),
                        rtcUiColorActionsRowIcon = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_row_icon, ContextCompat.getColor(context, R.color.rtc_ui_actions_icon)),
                        rtcUiColorActionsRowLabel = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_row_label, ContextCompat.getColor(context, R.color.rtc_ui_foreground)),
                        rtcUiColorSheetPill = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_sheet_pill, ContextCompat.getColor(context, R.color.rtc_ui_actions_background)),
                        rtcUiColorActionsDivider = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_divider, ContextCompat.getColor(context, R.color.rtc_ui_actions_background)),
                        rtcUiColorSheetBackground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_sheet_background, ContextCompat.getColor(context, R.color.rtc_ui_background)),
                )
            }.also {
                typedArray.recycle()
            }
        }
    }

}