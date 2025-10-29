/*
 * Colors.kt
 * Infobip RTC UI
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package com.infobip.webrtc.ui.view.styles

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.infobip.webrtc.ui.R

data class Colors @JvmOverloads constructor(
    @ColorInt val foreground: Int = Defaults.foreground,
    @ColorInt val background: Int = Defaults.background,
    @ColorInt val overlayBackground: Int = Defaults.overlayBackground,
    @ColorInt val textSecondary: Int = Defaults.textSecondary,
    @ColorInt val alertText: Int = Defaults.foreground,
    @ColorInt val alertBackground: Int = Defaults.alertBackground,
    @ColorInt val accept: Int = Defaults.accept,
    @ColorInt val hangup: Int = Defaults.hangup,
    @ColorInt val actionsBackground: Int = Defaults.actionsBackground,
    @ColorInt val actionsBackgroundChecked: Int = Defaults.foreground,
    @ColorInt val actionsIcon: Int = Defaults.foreground,
    @ColorInt val actionsIconChecked: Int = Defaults.actionsIconChecked,
    @ColorInt val actionsRowBackground: Int = Defaults.background,
    @ColorInt val actionsRowIcon: Int = Defaults.foreground,
    @ColorInt val actionsRowLabel: Int = Defaults.foreground,
    @ColorInt val actionsDivider: Int = Defaults.actionsBackground,
    @ColorInt val sheetBackground: Int = Defaults.background,
    @ColorInt val sheetPill: Int = Defaults.actionsBackground,
) {

    object Defaults {
        @ColorInt
        val foreground = Color.WHITE
        @ColorInt
        val actionsBackground = Color.parseColor("#15FFFFFF")
        @ColorInt
        val actionsIconChecked = Color.parseColor("#B2242424")
        @ColorInt
        val textSecondary = Color.parseColor("#7AFFFFFF")
        @ColorInt
        val background = Color.parseColor("#242424")
        @ColorInt
        val overlayBackground = Color.parseColor("#80242424")
        @ColorInt
        val alertBackground = Color.parseColor("#99050708")
        @ColorInt
        val accept = Color.parseColor("#29B899")
        @ColorInt
        val hangup = Color.parseColor("#C84714")
    }

    companion object {
        internal operator fun invoke(
            context: Context,
            attrs: AttributeSet?
        ): Colors {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.InfobipRtcUi, R.attr.infobipRtcUiStyle, R.style.InfobipRtcUi)
            return typedArray.let {
                Colors(
                    foreground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_foreground, Defaults.foreground),
                    background = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_background, Defaults.background),
                    overlayBackground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_overlay_background, Defaults.overlayBackground),
                    textSecondary = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_text_secondary, Defaults.textSecondary),
                    alertText = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_alert_text, Defaults.foreground),
                    alertBackground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_alert_background, Defaults.alertBackground),
                    accept = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_accept, Defaults.accept),
                    hangup = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_hangup, Defaults.hangup),
                    actionsBackground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_background, Defaults.actionsBackground),
                    actionsBackgroundChecked = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_background_checked, Defaults.foreground),
                    actionsIcon = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_icon, Defaults.foreground),
                    actionsIconChecked = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_icon_checked, Defaults.actionsIconChecked),
                    actionsRowBackground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_row_background, Defaults.background),
                    actionsRowIcon = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_row_icon, Defaults.foreground),
                    actionsRowLabel = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_row_label, Defaults.foreground),
                    actionsDivider = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_actions_divider, Defaults.actionsBackground),
                    sheetBackground = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_sheet_background, Defaults.background),
                    sheetPill = it.getColor(R.styleable.InfobipRtcUi_rtc_ui_color_sheet_pill, Defaults.actionsBackground),
                )
            }.also {
                typedArray.recycle()
            }
        }
    }

}