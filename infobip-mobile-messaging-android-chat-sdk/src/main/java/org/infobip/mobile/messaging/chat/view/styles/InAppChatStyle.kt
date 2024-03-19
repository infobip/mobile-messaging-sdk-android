package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.colorBackground
import org.infobip.mobile.messaging.chat.utils.colorPrimaryDark
import org.infobip.mobile.messaging.chat.utils.isIbDefaultTheme
import org.infobip.mobile.messaging.chat.utils.isMMBaseTheme
import org.infobip.mobile.messaging.chat.utils.resolveStringWithResId
import org.infobip.mobile.messaging.chat.utils.resolveThemeColor
import org.infobip.mobile.messaging.chat.utils.takeIfDefined

data class InAppChatStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val progressBarColor: Int,
        val networkConnectionText: String? = null,
        @StringRes val networkConnectionTextRes: Int? = null,
        @StyleRes val networkConnectionTextAppearance: Int? = null,
        @ColorInt val networkConnectionTextColor: Int,
        @ColorInt val networkConnectionLabelBackgroundColor: Int,
) {
    companion object {

        /**
         * Creates [InAppChatStyle] only from android style inside "IB_AppTheme.Chat" theme,
         * defined by "ibChatStyle" attribute provided by integrator.
         * If "ibChatStyle" attribute is not defined, default IB style "IB.Chat" is used.
         * Applies [WidgetInfo] livechat widget configuration into existing [InAppChatStyle] from android style inside "IB_AppTheme.Chat" theme,
         * defined by "ibChatStyle" attribute provided by integrator.
         * Priority: IB_AppTheme.Chat ibChatStyle > [WidgetInfo] > IB.Chat style
         */
        internal operator fun invoke(
                context: Context,
                attrs: AttributeSet?,
                widgetInfo: WidgetInfo?
        ): InAppChatStyle {
            context.obtainStyledAttributes(
                    attrs,
                    R.styleable.InAppChatViewStyleable,
                    R.attr.ibChatStyle,
                    R.style.IB_Chat
            ).run {
                val theme = context.theme
                val isIbDefaultTheme = context.theme.isIbDefaultTheme()

                val deprecatedProgressBarColor by lazy { context.resolveThemeColor(R.attr.colorPrimaryDark) }
                val backgroundColor = resolveBackgroundColor(widgetInfo, isIbDefaultTheme)
                val progressBarColor = resolveProgressBarColor(widgetInfo, isIbDefaultTheme, deprecatedProgressBarColor, theme)

                val connectionErrorLabelBackgroundColor = getColor(
                        R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorLabelBackgroundColor,
                        Color.GRAY
                )
                val connectionErrorTextColor = getColor(
                        R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorTextColor,
                        Color.BLACK
                )

                val (connectionErrorTextRes, connectionErrorText) = resolveStringWithResId(context, R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorText, R.string.ib_chat_no_connection)

                val connectionErrorTextAppearance = getResourceId(
                        R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorTextAppearance,
                        0
                ).takeIfDefined()

                recycle()
                return InAppChatStyle(
                        backgroundColor = backgroundColor,
                        progressBarColor = progressBarColor,
                        networkConnectionText = connectionErrorText,
                        networkConnectionTextRes = connectionErrorTextRes,
                        networkConnectionTextColor = connectionErrorTextColor,
                        networkConnectionTextAppearance = connectionErrorTextAppearance,
                        networkConnectionLabelBackgroundColor = connectionErrorLabelBackgroundColor,
                )
            }
        }

        //apply widget color if it is default theme (R.attr.ibChatDefaultStyledTheme) or attribute is missing, xml value otherwise
        private fun TypedArray.resolveProgressBarColor(widgetInfo: WidgetInfo?, isIbDefaultTheme: Boolean, deprecatedProgressBarColor: Int?, theme: Resources.Theme?): Int {
            val defaultColor = deprecatedProgressBarColor
                    ?: widgetInfo?.colorPrimaryDark.takeIf { theme.isMMBaseTheme() }
                    ?: Color.BLACK

            return widgetInfo?.colorPrimaryDark?.takeIf { isIbDefaultTheme }
                    ?: getColor(R.styleable.InAppChatViewStyleable_ibChatProgressBarColor, defaultColor)
        }

        private fun TypedArray.resolveBackgroundColor(widgetInfo: WidgetInfo?, isIbDefaultTheme: Boolean): Int {
            val defaultColor = widgetInfo?.colorBackground ?: Color.WHITE
            return widgetInfo?.colorBackground?.takeIf { isIbDefaultTheme }
                    ?: getColor(R.styleable.InAppChatViewStyleable_ibChatBackgroundColor, defaultColor)
        }
    }
}