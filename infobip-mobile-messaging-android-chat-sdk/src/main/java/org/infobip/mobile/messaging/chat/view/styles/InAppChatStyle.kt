package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.*

data class InAppChatStyle(
    @ColorInt val backgroundColor: Int,
    @ColorInt val progressBarColor: Int,
    val networkConnectionText: String? = null,
    @StringRes val networkConnectionTextRes: Int? = null,
    @StyleRes val networkConnectionTextAppearance: Int? = null,
    @ColorInt val networkConnectionTextColor: Int,
    @ColorInt val networkConnectionLabelBackgroundColor: Int,
    val isIbDefaultTheme: Boolean,
) {
    companion object {

        /**
         * Creates [InAppChatStyle] only from android style inside "IB_AppTheme.Chat" theme,
         * defined by "ibChatStyle" attribute provided by integrator.
         * If "ibChatStyle" attribute is not defined, default IB style "IB.Chat" is used.
         * Priority: IB_AppTheme.Chat - ibChatStyle > IB.Chat style
         */
        internal operator fun invoke(
            context: Context,
            attrs: AttributeSet?
        ): InAppChatStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.InAppChatViewStyleable,
                R.attr.ibChatStyle,
                R.style.IB_Chat
            ).run {
                val deprecatedProgressBarColor by lazy { context.resolveThemeColor(R.attr.colorPrimaryDark) }
                val backgroundColor =
                    getColor(R.styleable.InAppChatViewStyleable_ibChatBackgroundColor, Color.WHITE)
                val progressBarColor = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatProgressBarColor,
                    deprecatedProgressBarColor ?: Color.BLACK
                )

                val connectionErrorLabelBackgroundColor = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorLabelBackgroundColor,
                    Color.GRAY
                )
                val connectionErrorTextColor = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorTextColor,
                    Color.BLACK
                )

                var connectionErrorText: String?
                var connectionErrorTextRes: Int? = null
                val connectionErrorTextNonResource =
                    getNonResourceString(R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorText)
                if (connectionErrorTextNonResource != null) {
                    connectionErrorText = connectionErrorTextNonResource
                } else {
                    connectionErrorText =
                        getString(R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorText)
                    connectionErrorTextRes = getResourceId(
                        R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorText,
                        0
                    ).takeIfDefined()
                }

                if (connectionErrorTextRes != null && connectionErrorText == null) {
                    connectionErrorText = context.getString(connectionErrorTextRes)
                }

                val connectionErrorTextAppearance = getResourceId(
                    R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorTextAppearance,
                    0
                ).takeIfDefined()

                recycle()
                return InAppChatStyle(
                    backgroundColor = backgroundColor,
                    progressBarColor = progressBarColor,
                    networkConnectionText = connectionErrorText,
                    networkConnectionTextRes = connectionErrorTextRes
                        ?: R.string.ib_chat_no_connection,
                    networkConnectionTextColor = connectionErrorTextColor,
                    networkConnectionTextAppearance = connectionErrorTextAppearance,
                    networkConnectionLabelBackgroundColor = connectionErrorLabelBackgroundColor,
                    isIbDefaultTheme = context.theme.isIbDefaultTheme()
                )
            }
        }

        /**
         * Applies [WidgetInfo] livechat widget configuration into existing [InAppChatStyle] from android style inside "IB_AppTheme.Chat" theme,
         * defined by "ibChatStyle" attribute provided by integrator.
         * Priority: IB_AppTheme.Chat ibChatStyle > [WidgetInfo] > IB.Chat style
         */
        internal fun InAppChatStyle.applyWidgetConfig(
            context: Context,
            widgetInfo: WidgetInfo
        ): InAppChatStyle {
            //theme config
            var style = this
            val theme = context.theme

            @ColorInt
            val backgroundColor = widgetInfo.colorBackground

            @ColorInt
            val colorPrimaryDark = widgetInfo.colorPrimaryDark

            if (style.isIbDefaultTheme) { //if it is IB default theme apply widget color automatically to all components
                if (backgroundColor != null) {
                    style = style.copy(backgroundColor = backgroundColor)
                }
                if (colorPrimaryDark != null) {
                    style = style.copy(progressBarColor = colorPrimaryDark)
                }
            } else { //if it is theme provided by integrator apply widget color only components which are not defined by integrator
                val backgroundColorDefined = theme.isAttributePresent(
                    R.styleable.InAppChatViewStyleable_ibChatBackgroundColor,
                    R.attr.ibChatStyle,
                    R.styleable.InAppChatViewStyleable
                )
                if (!backgroundColorDefined && backgroundColor != null) {
                    style = style.copy(backgroundColor = backgroundColor)
                }

                val isBaseTheme = theme.isMMBaseTheme()
                val deprecatedColorPrimaryDarkDefined =
                    theme.isAttributePresent(R.attr.colorPrimaryDark)
                val applyWidgetColorPrimaryDark =
                    (isBaseTheme || !deprecatedColorPrimaryDarkDefined)

                val newProgressBarColorDefined = theme.isAttributePresent(
                    R.styleable.InAppChatViewStyleable_ibChatProgressBarColor,
                    R.attr.ibChatStyle,
                    R.styleable.InAppChatViewStyleable
                )
                if (applyWidgetColorPrimaryDark && !newProgressBarColorDefined && colorPrimaryDark != null) {
                    style = style.copy(progressBarColor = colorPrimaryDark)
                }
            }

            return style
        }

    }
}