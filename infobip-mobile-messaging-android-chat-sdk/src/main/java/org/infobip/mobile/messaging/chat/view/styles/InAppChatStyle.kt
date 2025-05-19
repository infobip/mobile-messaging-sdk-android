package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
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
import org.infobip.mobile.messaging.chat.utils.resolveStringWithResId
import org.infobip.mobile.messaging.chat.utils.takeIfDefined

data class InAppChatStyle @JvmOverloads constructor(
        @ColorInt val backgroundColor: Int = Defaults.backgroundColor,
        @ColorInt val progressBarColor: Int = Defaults.progressBarColor,
        val networkConnectionText: String? = null,
        @StringRes val networkConnectionTextRes: Int? = Defaults.networkConnectionTextRes,
        @StyleRes val networkConnectionTextAppearance: Int? = null,
        @ColorInt val networkConnectionTextColor: Int = Defaults.networkConnectionTextColor,
        @ColorInt val networkConnectionLabelBackgroundColor: Int = Defaults.networkConnectionLabelBackgroundColor,
) {
    object Defaults {
        @ColorInt val backgroundColor: Int = Color.WHITE
        @ColorInt val progressBarColor: Int = Color.BLACK
        @StringRes val networkConnectionTextRes: Int = R.string.ib_chat_no_connection
        @ColorInt val networkConnectionTextColor: Int = Color.BLACK
        @ColorInt val networkConnectionLabelBackgroundColor: Int = Color.parseColor("#808080")
    }

    class Builder {
        private var backgroundColor: Int = Defaults.backgroundColor
        private var progressBarColor: Int = Defaults.progressBarColor
        private var networkConnectionText: String? = null
        private var networkConnectionTextRes: Int? = Defaults.networkConnectionTextRes
        private var networkConnectionTextAppearance: Int? = null
        private var networkConnectionTextColor: Int = Defaults.networkConnectionTextColor
        private var networkConnectionLabelBackgroundColor: Int = Defaults.networkConnectionLabelBackgroundColor

        fun setBackgroundColor(@ColorInt backgroundColor: Int?) = apply { backgroundColor?.let { this.backgroundColor = it } }
        fun setProgressBarColor(@ColorInt progressBarColor: Int?) = apply { progressBarColor?.let { this.progressBarColor = it } }
        fun setNetworkConnectionText(networkConnectionText: String?) = apply { networkConnectionText?.let { this.networkConnectionText = it } }
        fun setNetworkConnectionTextRes(@StringRes networkConnectionTextRes: Int?) = apply { networkConnectionTextRes?.let { this.networkConnectionTextRes = it } }
        fun setNetworkConnectionTextAppearance(@StyleRes networkConnectionTextAppearance: Int?) = apply { networkConnectionTextAppearance?.let { this.networkConnectionTextAppearance = it } }
        fun setNetworkConnectionTextColor(@ColorInt networkConnectionTextColor: Int?) = apply { networkConnectionTextColor?.let { this.networkConnectionTextColor = it } }
        fun setNetworkConnectionLabelBackgroundColor(@ColorInt networkConnectionLabelBackgroundColor: Int?) = apply { networkConnectionLabelBackgroundColor?.let { this.networkConnectionLabelBackgroundColor = it } }

        fun build() = InAppChatStyle(
            backgroundColor = backgroundColor,
            progressBarColor = progressBarColor,
            networkConnectionText = networkConnectionText,
            networkConnectionTextRes = networkConnectionTextRes,
            networkConnectionTextAppearance = networkConnectionTextAppearance,
            networkConnectionTextColor = networkConnectionTextColor,
            networkConnectionLabelBackgroundColor = networkConnectionLabelBackgroundColor
        )
    }

    companion object {

        /**
         * Creates [InAppChatStyle] only from android style inside `IB_AppTheme.Chat` theme,
         * defined by `ibChatStyle` attribute provided by integrator.
         * If `ibChatStyle` attribute is not defined, default IB style `IB.Chat` is used.
         * Applies [WidgetInfo] livechat widget configuration into existing [InAppChatStyle] from android style inside `IB_AppTheme.Chat` theme,
         * defined by `ibChatStyle` attribute provided by integrator.
         * Priority: `IB_AppTheme.Chat.ibChatStyle` > [WidgetInfo] > `IB.Chat` style
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
                val isIbDefaultTheme = context.theme.isIbDefaultTheme()

                val backgroundColor = widgetInfo?.colorBackground?.takeIf { isIbDefaultTheme }
                    ?: getColor(R.styleable.InAppChatViewStyleable_ibChatBackgroundColor, Defaults.backgroundColor)

                val progressBarColor =  widgetInfo?.colorPrimaryDark?.takeIf { isIbDefaultTheme }
                    ?: getColor(R.styleable.InAppChatViewStyleable_ibChatProgressBarColor, Defaults.progressBarColor)

                val connectionErrorLabelBackgroundColor = getColor(
                        R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorLabelBackgroundColor,
                        Defaults.networkConnectionLabelBackgroundColor
                )
                val connectionErrorTextColor = getColor(
                        R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorTextColor,
                        Defaults.networkConnectionTextColor
                )
                val (connectionErrorTextRes, connectionErrorText) = resolveStringWithResId(
                    context,
                    R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorText,
                    Defaults.networkConnectionTextRes
                )
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
    }
}