/*
 * InAppChatStyle.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.graphics.toColorInt
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.colorBackground
import org.infobip.mobile.messaging.chat.utils.colorPrimary
import org.infobip.mobile.messaging.chat.utils.colorPrimaryDark
import org.infobip.mobile.messaging.chat.utils.getDrawableCompat
import org.infobip.mobile.messaging.chat.utils.isIbDefaultTheme
import org.infobip.mobile.messaging.chat.utils.resolveStringWithResId
import org.infobip.mobile.messaging.chat.utils.takeIfDefined

/**
 * Style configuration for the InAppChat main view.
 *
 * This data class defines the visual appearance and behavior of the chat view,
 * including styling for the loading spinner, network connection error banner, and
 * error screen displayed when chat fails to load.
 *
 * Customize styling through any of three configuration sources:
 * - Programmatic [Builder]
 * - XML theme attributes (prefixed with `ibChat*`)
 * - Server-side WidgetInfo configuration
 *
 * ### Property Resolution Priority
 * When multiple configuration sources are present, properties are resolved in this order:
 * 1. Runtime values set via [Builder] (highest priority)
 * 2. XML theme attributes (attributes prefixed with `ibChat*`)
 * 3. WidgetInfo server configuration
 * 4. [Defaults] object values (lowest priority)
 *
 * ### Programmatic Usage Example
 * ```kotlin
 * val chatStyle = InAppChatStyle.Builder()
 *     .setBackgroundColor(Color.WHITE)
 *     .setProgressBarColor(Color.BLUE)
 *     .setErrorTitleText("Oops! Something went wrong")
 *     .setErrorBackgroundColor(Color.WHITE)
 *     .setNetworkConnectionErrorBackgroundColor(Color.RED)
 *     .build()
 *
 * val theme = InAppChatTheme(
 *     chatToolbarStyle = toolbarStyle,
 *     attachmentToolbarStyle = attachmentStyle,
 *     chatStyle = chatStyle,
 *     chatInputViewStyle = inputStyle
 * )
 *
 * InAppChat.getInstance(context).setTheme(theme)
 * ```
 *
 * ### XML Theme Configuration Example
 * ```xml
 * <style name="MyChat" parent="IB.Chat">
 *     <item name="ibChatBackgroundColor">@color/white</item>
 *     <item name="ibChatProgressBarColor">@color/blue</item>
 *     <item name="ibChatErrorTitleText">@string/error_title</item>
 *     <item name="ibChatErrorBackgroundColor">@color/error_bg</item>
 *     <item name="ibChatNetworkConnectionErrorBackgroundColor">@color/error_red</item>
 * </style>
 *
 * <style name="IB_AppTheme.Chat" parent="IB_ChatDefaultTheme">
 *     <item name="ibChatStyle">@style/MyChat</item>
 * </style>
 * ```
 *
 * @see InAppChatTheme for complete theme configuration
 * @see Builder for programmatic customization
 * @see Defaults for default values
 */
data class InAppChatStyle @JvmOverloads constructor(
    @ColorInt val backgroundColor: Int = Defaults.backgroundColor,
    @ColorInt val progressBarColor: Int = Defaults.progressBarColor,
    @Deprecated("Use networkConnectionErrorText instead.", ReplaceWith("networkConnectionErrorText")) val networkConnectionText: String? = null,
    @Deprecated("Use networkConnectionErrorTextRes instead.", ReplaceWith("networkConnectionErrorTextRes")) @StringRes val networkConnectionTextRes: Int? = Defaults.networkConnectionErrorTextRes,
    @Deprecated("Use networkConnectionErrorTextAppearance instead.", ReplaceWith("networkConnectionErrorTextAppearance")) @StyleRes val networkConnectionTextAppearance: Int? = null,
    @Deprecated("Use networkConnectionErrorTextColor instead.", ReplaceWith("networkConnectionErrorTextColor")) @ColorInt val networkConnectionTextColor: Int = Defaults.networkConnectionErrorTextColor,
    @Deprecated("Use networkConnectionErrorBackgroundColor instead.", ReplaceWith("networkConnectionErrorBackgroundColor")) @ColorInt val networkConnectionLabelBackgroundColor: Int = Defaults.networkConnectionErrorBackgroundColor,
    val networkConnectionErrorText: String? = networkConnectionText,
    @StringRes val networkConnectionErrorTextRes: Int? = networkConnectionTextRes,
    @StyleRes val networkConnectionErrorTextAppearance: Int? = networkConnectionTextAppearance,
    @ColorInt val networkConnectionErrorTextColor: Int = networkConnectionTextColor,
    @ColorInt val networkConnectionErrorBackgroundColor: Int = networkConnectionLabelBackgroundColor,
    val networkConnectionErrorIcon: Drawable? = null,
    @ColorInt val networkConnectionErrorIconTint: Int = Defaults.networkConnectionErrorIconTint,
    val networkConnectionErrorIconVisible: Boolean = Defaults.networkConnectionErrorIconVisible,
    val errorTitleText: String? = null,
    @StringRes val errorTitleTextRes: Int? = Defaults.errorTitleTextRes,
    @ColorInt val errorTitleTextColor: Int = Defaults.errorTitleTextColor,
    @StyleRes val errorTitleTextAppearance: Int? = null,
    val errorDescriptionText: String? = null,
    @StringRes val errorDescriptionTextRes: Int? = Defaults.errorDescriptionTextRes,
    @ColorInt val errorDescriptionTextColor: Int = Defaults.errorDescriptionTextColor,
    @StyleRes val errorDescriptionTextAppearance: Int? = null,
    @ColorInt val errorBackgroundColor: Int = Defaults.errorBackgroundColor,
    val errorIcon: Drawable? = null,
    @ColorInt val errorIconTint: Int? = null,
    val errorRefreshButtonText: String? = null,
    @StringRes val errorRefreshButtonTextRes: Int? = Defaults.errorRefreshButtonTextRes,
    @ColorInt val errorRefreshButtonTextColor: Int = Defaults.errorRefreshButtonTextColor,
    val errorRefreshButtonVisible: Boolean = Defaults.errorRefreshButtonVisible,
) {
    /**
     * Default values for [InAppChatStyle] properties.
     *
     * These values are applied when properties are not explicitly set through
     * [Builder], XML theme attributes, or WidgetInfo server configuration.
     *
     * The 4-level resolution priority is:
     * 1. Runtime Builder values (highest priority)
     * 2. XML theme attributes
     * 3. WidgetInfo configuration
     * 4. These default values (lowest priority)
     */
    object Defaults {
        @ColorInt val backgroundColor: Int = Color.WHITE
        @ColorInt val progressBarColor: Int = Color.BLACK
        @StringRes val networkConnectionErrorTextRes: Int = R.string.ib_chat_no_connection
        @ColorInt val networkConnectionErrorTextColor: Int = Color.WHITE
        @ColorInt val networkConnectionErrorBackgroundColor: Int = "#CF182E".toColorInt()
        @ColorInt val networkConnectionErrorIconTint: Int = Color.WHITE
        val networkConnectionErrorIconVisible: Boolean = true
        @StringRes val errorTitleTextRes: Int = R.string.ib_chat_error
        @ColorInt val errorTitleTextColor: Int = Color.BLACK
        @StringRes val errorDescriptionTextRes: Int = R.string.ib_chat_error_description
        @ColorInt val errorDescriptionTextColor: Int = "#808080".toColorInt()
        @ColorInt val errorBackgroundColor: Int = Color.WHITE
        @StringRes val errorRefreshButtonTextRes: Int = R.string.ib_chat_refresh
        @ColorInt val errorRefreshButtonTextColor: Int = Color.BLACK
        val errorRefreshButtonVisible: Boolean = true
    }

    /**
     * Fluent builder for creating [InAppChatStyle] instances with customized properties.
     *
     * All setter methods return the builder instance for method chaining.
     *
     * Example:
     * ```kotlin
     * val style = InAppChatStyle.Builder()
     *     .setBackgroundColor(Color.WHITE)
     *     .setErrorTitleText("Error!")
     *     .setErrorBackgroundColor(Color.LIGHT_GRAY)
     *     .build()
     * ```
     */
    class Builder {
        private var backgroundColor: Int = Defaults.backgroundColor
        private var progressBarColor: Int = Defaults.progressBarColor
        private var networkConnectionErrorText: String? = null
        private var networkConnectionErrorTextRes: Int? = Defaults.networkConnectionErrorTextRes
        private var networkConnectionErrorTextAppearance: Int? = null
        private var networkConnectionErrorTextColor: Int = Defaults.networkConnectionErrorTextColor
        private var networkConnectionErrorBackgroundColor: Int = Defaults.networkConnectionErrorBackgroundColor
        private var networkConnectionErrorIcon: Drawable? = null
        private var networkConnectionErrorIconTint: Int = Defaults.networkConnectionErrorIconTint
        private var networkConnectionErrorIconVisible: Boolean = Defaults.networkConnectionErrorIconVisible
        private var errorTitleText: String? = null
        private var errorTitleTextRes: Int? = Defaults.errorTitleTextRes
        private var errorTitleTextColor: Int = Defaults.errorTitleTextColor
        private var errorTitleTextAppearance: Int? = null
        private var errorDescriptionText: String? = null
        private var errorDescriptionTextRes: Int? = Defaults.errorDescriptionTextRes
        private var errorDescriptionTextColor: Int = Defaults.errorDescriptionTextColor
        private var errorDescriptionTextAppearance: Int? = null
        private var errorBackgroundColor: Int = Defaults.errorBackgroundColor
        private var errorIcon: Drawable? = null
        private var errorIconTint: Int? = null
        private var errorRefreshButtonText: String? = null
        private var errorRefreshButtonTextRes: Int? = Defaults.errorRefreshButtonTextRes
        private var errorRefreshButtonTextColor: Int = Defaults.errorRefreshButtonTextColor
        private var errorRefreshButtonVisible: Boolean = Defaults.errorRefreshButtonVisible

        fun setBackgroundColor(@ColorInt backgroundColor: Int?) = apply { backgroundColor?.let { this.backgroundColor = it } }
        fun setProgressBarColor(@ColorInt progressBarColor: Int?) = apply { progressBarColor?.let { this.progressBarColor = it } }
        @Deprecated("Use setNetworkConnectionErrorText() instead.", ReplaceWith("setNetworkConnectionErrorText(networkConnectionText)"))
        fun setNetworkConnectionText(networkConnectionText: String?) = setNetworkConnectionErrorText(networkConnectionText)
        @Deprecated("Use setNetworkConnectionErrorTextRes() instead.", ReplaceWith("setNetworkConnectionErrorTextRes(networkConnectionTextRes)"))
        fun setNetworkConnectionTextRes(@StringRes networkConnectionTextRes: Int?) = setNetworkConnectionErrorTextRes(networkConnectionTextRes)
        @Deprecated("Use setNetworkConnectionErrorTextAppearance() instead.", ReplaceWith("setNetworkConnectionErrorTextAppearance(networkConnectionTextAppearance)"))
        fun setNetworkConnectionTextAppearance(@StyleRes networkConnectionTextAppearance: Int?) = setNetworkConnectionErrorTextAppearance(networkConnectionTextAppearance)
        @Deprecated("Use setNetworkConnectionErrorTextColor() instead.", ReplaceWith("setNetworkConnectionErrorTextColor(networkConnectionTextColor)"))
        fun setNetworkConnectionTextColor(@ColorInt networkConnectionTextColor: Int?) = setNetworkConnectionErrorTextColor(networkConnectionTextColor)
        @Deprecated("Use setNetworkConnectionErrorBackgroundColor() instead.", ReplaceWith("setNetworkConnectionErrorBackgroundColor(networkConnectionLabelBackgroundColor)"))
        fun setNetworkConnectionLabelBackgroundColor(@ColorInt networkConnectionLabelBackgroundColor: Int?) = setNetworkConnectionErrorBackgroundColor(networkConnectionLabelBackgroundColor)
        fun setNetworkConnectionErrorText(networkConnectionErrorText: String?) = apply { networkConnectionErrorText?.let { this.networkConnectionErrorText = it } }
        fun setNetworkConnectionErrorTextRes(@StringRes networkConnectionErrorTextRes: Int?) = apply { networkConnectionErrorTextRes?.let { this.networkConnectionErrorTextRes = it } }
        fun setNetworkConnectionErrorTextAppearance(@StyleRes networkConnectionErrorTextAppearance: Int?) = apply { networkConnectionErrorTextAppearance?.let { this.networkConnectionErrorTextAppearance = it } }
        fun setNetworkConnectionErrorTextColor(@ColorInt networkConnectionErrorTextColor: Int?) = apply { networkConnectionErrorTextColor?.let { this.networkConnectionErrorTextColor = it } }
        fun setNetworkConnectionErrorBackgroundColor(@ColorInt networkConnectionErrorBackgroundColor: Int?) = apply { networkConnectionErrorBackgroundColor?.let { this.networkConnectionErrorBackgroundColor = it } }
        fun setNetworkConnectionErrorIcon(networkConnectionErrorIcon: Drawable?) = apply { networkConnectionErrorIcon?.let { this.networkConnectionErrorIcon = it } }
        fun setNetworkConnectionErrorIconTint(@ColorInt networkConnectionErrorIconTint: Int?) = apply { networkConnectionErrorIconTint?.let { this.networkConnectionErrorIconTint = it } }
        fun setNetworkConnectionErrorIconVisible(networkConnectionErrorIconVisible: Boolean?) = apply { networkConnectionErrorIconVisible?.let { this.networkConnectionErrorIconVisible = it } }
        fun setErrorTitleText(errorTitleText: String?) = apply { errorTitleText?.let { this.errorTitleText = it} }
        fun setErrorTitleTextRes(@StringRes errorTitleTextRes: Int?) = apply { errorTitleTextRes?.let { this.errorTitleTextRes = it} }
        fun setErrorTitleTextColor(@ColorInt errorTitleTextColor: Int?) = apply { errorTitleTextColor?.let { this.errorTitleTextColor = it} }
        fun setErrorTitleTextAppearance(@StyleRes errorTitleTextAppearance: Int?) = apply { errorTitleTextAppearance?.let { this.errorTitleTextAppearance = it} }
        fun setErrorDescriptionText(errorDescriptionText: String?) = apply { errorDescriptionText?.let { this.errorDescriptionText = it} }
        fun setErrorDescriptionTextRes(@StringRes errorDescriptionTextRes: Int?) = apply { errorDescriptionTextRes?.let { this.errorDescriptionTextRes = it} }
        fun setErrorDescriptionTextColor(@ColorInt errorDescriptionTextColor: Int?) = apply { errorDescriptionTextColor?.let { this.errorDescriptionTextColor = it} }
        fun setErrorDescriptionTextAppearance(@StyleRes errorDescriptionTextAppearance: Int?) = apply { errorDescriptionTextAppearance?.let { this.errorDescriptionTextAppearance = it} }
        fun setErrorBackgroundColor(@ColorInt errorBackgroundColor: Int?) = apply { errorBackgroundColor?.let { this.errorBackgroundColor = it} }
        fun setErrorIcon(errorIcon: Drawable?) = apply { errorIcon?.let { this.errorIcon = it} }
        fun setErrorIconTint(@ColorInt errorIconTint: Int?) = apply { errorIconTint?.let { this.errorIconTint = it} }
        fun setErrorRefreshButtonText(errorRefreshButtonText: String?) = apply { errorRefreshButtonText?.let { this.errorRefreshButtonText = it} }
        fun setErrorRefreshButtonTextRes(@StringRes errorRefreshButtonTextRes: Int?) = apply { errorRefreshButtonTextRes?.let { this.errorRefreshButtonTextRes = it} }
        fun setErrorRefreshButtonTextColor(@ColorInt errorRefreshButtonTextColor: Int?) = apply { errorRefreshButtonTextColor?.let { this.errorRefreshButtonTextColor = it} }
        fun setErrorRefreshButtonVisible(errorRefreshButtonVisible: Boolean?) = apply { errorRefreshButtonVisible?.let { this.errorRefreshButtonVisible = it} }

        fun build() = InAppChatStyle(
            backgroundColor = backgroundColor,
            progressBarColor = progressBarColor,
            networkConnectionErrorText = networkConnectionErrorText,
            networkConnectionErrorTextRes = networkConnectionErrorTextRes,
            networkConnectionErrorTextAppearance = networkConnectionErrorTextAppearance,
            networkConnectionErrorTextColor = networkConnectionErrorTextColor,
            networkConnectionErrorBackgroundColor = networkConnectionErrorBackgroundColor,
            networkConnectionErrorIcon = networkConnectionErrorIcon,
            networkConnectionErrorIconTint = networkConnectionErrorIconTint,
            networkConnectionErrorIconVisible = networkConnectionErrorIconVisible,
            errorTitleText = errorTitleText,
            errorTitleTextRes = errorTitleTextRes,
            errorTitleTextColor = errorTitleTextColor,
            errorTitleTextAppearance = errorTitleTextAppearance,
            errorDescriptionText = errorDescriptionText,
            errorDescriptionTextRes = errorDescriptionTextRes,
            errorDescriptionTextColor = errorDescriptionTextColor,
            errorDescriptionTextAppearance = errorDescriptionTextAppearance,
            errorBackgroundColor = errorBackgroundColor,
            errorIcon = errorIcon,
            errorIconTint = errorIconTint,
            errorRefreshButtonText = errorRefreshButtonText,
            errorRefreshButtonTextRes = errorRefreshButtonTextRes,
            errorRefreshButtonTextColor = errorRefreshButtonTextColor,
            errorRefreshButtonVisible = errorRefreshButtonVisible,
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

                //Take widget color only if default theme is used or attribute is not defined in integrator's theme
                val backgroundColor = widgetInfo?.colorBackground?.takeIf {
                    isIbDefaultTheme || !hasValue(R.styleable.InAppChatViewStyleable_ibChatBackgroundColor)
                } ?: getColor(R.styleable.InAppChatViewStyleable_ibChatBackgroundColor, Defaults.backgroundColor)

                val progressBarColor =  widgetInfo?.colorPrimaryDark?.takeIf {
                    isIbDefaultTheme || !hasValue(R.styleable.InAppChatViewStyleable_ibChatProgressBarColor)
                } ?: getColor(R.styleable.InAppChatViewStyleable_ibChatProgressBarColor, Defaults.progressBarColor)

                val networkConnectionErrorBackgroundColor = getColor(R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorBackgroundColor, 0).takeIfDefined()
                        ?: getColor(R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorLabelBackgroundColor, Defaults.networkConnectionErrorBackgroundColor)

                val networkConnectionErrorTextColor = getColor(
                        R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorTextColor,
                        Defaults.networkConnectionErrorTextColor
                )

                val (networkConnectionErrorTextRes, networkConnectionErrorText) = resolveStringWithResId(
                    context,
                    R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorText,
                    Defaults.networkConnectionErrorTextRes
                )

                val networkConnectionErrorTextAppearance = getResourceId(
                        R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorTextAppearance,
                        0
                ).takeIfDefined()

                val networkConnectionErrorIcon = getResourceId(
                    R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorIcon,
                    R.drawable.ib_chat_network_error_icon
                )

                val networkConnectionErrorIconTint = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorIconTint,
                    Defaults.networkConnectionErrorIconTint
                )

                val networkConnectionErrorIconVisible = getBoolean(
                    R.styleable.InAppChatViewStyleable_ibChatNetworkConnectionErrorIconVisible,
                    Defaults.networkConnectionErrorIconVisible
                )

                val (errorTitleTextRes, errorTitleText) = resolveStringWithResId(
                    context,
                    R.styleable.InAppChatViewStyleable_ibChatErrorTitleText,
                    Defaults.errorTitleTextRes
                )

                val errorTitleTextColor = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatErrorTitleTextColor,
                    Defaults.errorTitleTextColor
                )

                val errorTitleTextAppearance = getResourceId(
                    R.styleable.InAppChatViewStyleable_ibChatErrorTitleTextAppearance,
                    0
                ).takeIfDefined()

                val (errorDescriptionTextRes, errorDescriptionText) = resolveStringWithResId(
                    context,
                    R.styleable.InAppChatViewStyleable_ibChatErrorDescriptionText,
                    Defaults.errorDescriptionTextRes
                )

                val errorDescriptionTextColor = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatErrorDescriptionTextColor,
                    Defaults.errorDescriptionTextColor
                )

                val errorDescriptionTextAppearance = getResourceId(
                    R.styleable.InAppChatViewStyleable_ibChatErrorDescriptionTextAppearance,
                    0
                ).takeIfDefined()

                val errorBackgroundColor = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatErrorBackgroundColor,
                    Defaults.errorBackgroundColor
                )

                val errorIcon = getResourceId(
                    R.styleable.InAppChatViewStyleable_ibChatErrorIcon,
                    R.drawable.ib_chat_error
                )

                val errorIconTint = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatErrorIconTint,
                    0
                ).takeIfDefined()

                val (errorRefreshButtonTextRes, errorRefreshButtonText) = resolveStringWithResId(
                    context,
                    R.styleable.InAppChatViewStyleable_ibChatErrorRefreshButtonText,
                    Defaults.errorRefreshButtonTextRes
                )

                val errorRefreshButtonTextColor =
                    widgetInfo?.colorPrimary?.takeIf { isIbDefaultTheme || !hasValue(R.styleable.InAppChatViewStyleable_ibChatErrorRefreshButtonTextColor) }
                        ?: getColor(R.styleable.InAppChatViewStyleable_ibChatErrorRefreshButtonTextColor, Defaults.errorRefreshButtonTextColor)

                val errorRefreshButtonVisible = getBoolean(
                    R.styleable.InAppChatViewStyleable_ibChatErrorRefreshButtonVisible,
                    Defaults.errorRefreshButtonVisible
                )

                recycle()
                return InAppChatStyle(
                    backgroundColor = backgroundColor,
                    progressBarColor = progressBarColor,
                    networkConnectionErrorText = networkConnectionErrorText,
                    networkConnectionErrorTextRes = networkConnectionErrorTextRes,
                    networkConnectionErrorTextColor = networkConnectionErrorTextColor,
                    networkConnectionErrorTextAppearance = networkConnectionErrorTextAppearance,
                    networkConnectionErrorBackgroundColor = networkConnectionErrorBackgroundColor,
                    networkConnectionErrorIcon = context.getDrawableCompat(networkConnectionErrorIcon),
                    networkConnectionErrorIconTint = networkConnectionErrorIconTint,
                    networkConnectionErrorIconVisible = networkConnectionErrorIconVisible,
                    errorTitleText = errorTitleText,
                    errorTitleTextRes = errorTitleTextRes,
                    errorTitleTextColor = errorTitleTextColor,
                    errorTitleTextAppearance = errorTitleTextAppearance,
                    errorDescriptionText = errorDescriptionText,
                    errorDescriptionTextRes = errorDescriptionTextRes,
                    errorDescriptionTextColor = errorDescriptionTextColor,
                    errorDescriptionTextAppearance = errorDescriptionTextAppearance,
                    errorBackgroundColor = errorBackgroundColor,
                    errorIcon = context.getDrawableCompat(errorIcon),
                    errorIconTint = errorIconTint,
                    errorRefreshButtonText = errorRefreshButtonText,
                    errorRefreshButtonTextRes = errorRefreshButtonTextRes,
                    errorRefreshButtonTextColor = errorRefreshButtonTextColor,
                    errorRefreshButtonVisible = errorRefreshButtonVisible,
                )
            }
        }
    }
}
