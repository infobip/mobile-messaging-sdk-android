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
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle.Builder
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle.Defaults

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
 *     .setChatFullScreenErrorTitleText("Oops! Something went wrong")
 *     .setChatFullScreenErrorBackgroundColor(Color.WHITE)
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
 *     <item name="ibChatFullScreenErrorTitleText">@string/error_title</item>
 *     <item name="ibChatFullScreenErrorBackgroundColor">@color/error_bg</item>
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
    @Deprecated("Use networkConnectionErrorTextRes instead.", ReplaceWith("networkConnectionErrorTextRes")) @StringRes val networkConnectionTextRes: Int? = null,
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
    @ColorInt val chatSnackbarErrorTextColor: Int? = null,
    @StyleRes val chatSnackbarErrorTextAppearance: Int? = null,
    @ColorInt val chatSnackbarErrorBackgroundColor: Int? = null,
    val chatSnackbarErrorIcon: Drawable? = null,
    @ColorInt val chatSnackbarErrorIconTint: Int? = null,
    val chatFullScreenErrorTitleText: String? = null,
    @StringRes val chatFullScreenErrorTitleTextRes: Int? = Defaults.errorTitleTextRes,
    @ColorInt val chatFullScreenErrorTitleTextColor: Int = Defaults.errorTitleTextColor,
    @StyleRes val chatFullScreenErrorTitleTextAppearance: Int? = null,
    val chatFullScreenErrorDescriptionText: String? = null,
    @StringRes val chatFullScreenErrorDescriptionTextRes: Int? = Defaults.errorDescriptionTextRes,
    @ColorInt val chatFullScreenErrorDescriptionTextColor: Int = Defaults.errorDescriptionTextColor,
    @StyleRes val chatFullScreenErrorDescriptionTextAppearance: Int? = null,
    @ColorInt val chatFullScreenErrorBackgroundColor: Int = Defaults.errorBackgroundColor,
    val chatFullScreenErrorIcon: Drawable? = null,
    @ColorInt val chatFullScreenErrorIconTint: Int? = null,
    val chatFullScreenErrorRefreshButtonText: String? = null,
    @StringRes val chatFullScreenErrorRefreshButtonTextRes: Int? = Defaults.errorRefreshButtonTextRes,
    @ColorInt val chatFullScreenErrorRefreshButtonTextColor: Int = Defaults.errorRefreshButtonTextColor,
    val chatFullScreenErrorRefreshButtonVisible: Boolean = Defaults.errorRefreshButtonVisible,
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
        @ColorInt val networkConnectionErrorTextColor: Int = Color.WHITE
        @ColorInt val networkConnectionErrorBackgroundColor: Int = "#CF182E".toColorInt()
        @ColorInt val networkConnectionErrorIconTint: Int = Color.WHITE
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
     *     .setChatFullScreenErrorTitleText("Error!")
     *     .setChatFullScreenErrorBackgroundColor(Color.LIGHT_GRAY)
     *     .build()
     * ```
     */
    class Builder {
        private var backgroundColor: Int = Defaults.backgroundColor
        private var progressBarColor: Int = Defaults.progressBarColor
        private var networkConnectionErrorText: String? = null
        private var networkConnectionErrorTextRes: Int? = null
        private var networkConnectionErrorTextAppearance: Int? = null
        private var networkConnectionErrorTextColor: Int = Defaults.networkConnectionErrorTextColor
        private var networkConnectionErrorBackgroundColor: Int = Defaults.networkConnectionErrorBackgroundColor
        private var networkConnectionErrorIcon: Drawable? = null
        private var networkConnectionErrorIconTint: Int = Defaults.networkConnectionErrorIconTint
        private var chatSnackbarErrorTextColor: Int? = null
        private var chatSnackbarErrorTextAppearance: Int? = null
        private var chatSnackbarErrorBackgroundColor: Int? = null
        private var chatSnackbarErrorIcon: Drawable? = null
        private var chatSnackbarErrorIconTint: Int? = null
        private var chatFullScreenErrorTitleText: String? = null
        private var chatFullScreenErrorTitleTextRes: Int? = Defaults.errorTitleTextRes
        private var chatFullScreenErrorTitleTextColor: Int = Defaults.errorTitleTextColor
        private var chatFullScreenErrorTitleTextAppearance: Int? = null
        private var chatFullScreenErrorDescriptionText: String? = null
        private var chatFullScreenErrorDescriptionTextRes: Int? = Defaults.errorDescriptionTextRes
        private var chatFullScreenErrorDescriptionTextColor: Int = Defaults.errorDescriptionTextColor
        private var chatFullScreenErrorDescriptionTextAppearance: Int? = null
        private var chatFullScreenErrorBackgroundColor: Int = Defaults.errorBackgroundColor
        private var chatFullScreenErrorIcon: Drawable? = null
        private var chatFullScreenErrorIconTint: Int? = null
        private var chatFullScreenErrorRefreshButtonText: String? = null
        private var chatFullScreenErrorRefreshButtonTextRes: Int? = Defaults.errorRefreshButtonTextRes
        private var chatFullScreenErrorRefreshButtonTextColor: Int = Defaults.errorRefreshButtonTextColor
        private var chatFullScreenErrorRefreshButtonVisible: Boolean = Defaults.errorRefreshButtonVisible

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
        fun setNetworkConnectionErrorText(networkConnectionErrorText: String?) = apply { this.networkConnectionErrorText = networkConnectionErrorText }
        fun setNetworkConnectionErrorTextRes(@StringRes networkConnectionErrorTextRes: Int?) = apply { this.networkConnectionErrorTextRes = networkConnectionErrorTextRes }
        fun setNetworkConnectionErrorTextAppearance(@StyleRes networkConnectionErrorTextAppearance: Int?) = apply { networkConnectionErrorTextAppearance?.let { this.networkConnectionErrorTextAppearance = it } }
        fun setNetworkConnectionErrorTextColor(@ColorInt networkConnectionErrorTextColor: Int?) = apply { networkConnectionErrorTextColor?.let { this.networkConnectionErrorTextColor = it } }
        fun setNetworkConnectionErrorBackgroundColor(@ColorInt networkConnectionErrorBackgroundColor: Int?) = apply { networkConnectionErrorBackgroundColor?.let { this.networkConnectionErrorBackgroundColor = it } }
        fun setNetworkConnectionErrorIcon(networkConnectionErrorIcon: Drawable?) = apply { networkConnectionErrorIcon?.let { this.networkConnectionErrorIcon = it } }
        fun setNetworkConnectionErrorIconTint(@ColorInt networkConnectionErrorIconTint: Int?) = apply { networkConnectionErrorIconTint?.let { this.networkConnectionErrorIconTint = it } }
        fun setChatSnackbarErrorTextColor(@ColorInt chatSnackbarErrorTextColor: Int?) = apply { chatSnackbarErrorTextColor?.let { this.chatSnackbarErrorTextColor = it } }
        fun setChatSnackbarErrorTextAppearance(@StyleRes chatSnackbarErrorTextAppearance: Int?) = apply { chatSnackbarErrorTextAppearance?.let { this.chatSnackbarErrorTextAppearance = it } }
        fun setChatSnackbarErrorBackgroundColor(@ColorInt chatSnackbarErrorBackgroundColor: Int?) = apply { chatSnackbarErrorBackgroundColor?.let { this.chatSnackbarErrorBackgroundColor = it } }
        fun setChatSnackbarErrorIcon(chatSnackbarErrorIcon: Drawable?) = apply { chatSnackbarErrorIcon?.let { this.chatSnackbarErrorIcon = it } }
        fun setChatSnackbarErrorIconTint(@ColorInt chatSnackbarErrorIconTint: Int?) = apply { chatSnackbarErrorIconTint?.let { this.chatSnackbarErrorIconTint = it } }
        fun setChatFullScreenErrorTitleText(chatFullScreenErrorTitleText: String?) =
            apply { chatFullScreenErrorTitleText?.let { this.chatFullScreenErrorTitleText = it } }

        fun setChatFullScreenErrorTitleTextRes(@StringRes chatFullScreenErrorTitleTextRes: Int?) =
            apply { chatFullScreenErrorTitleTextRes?.let { this.chatFullScreenErrorTitleTextRes = it } }

        fun setChatFullScreenErrorTitleTextColor(@ColorInt chatFullScreenErrorTitleTextColor: Int?) =
            apply { chatFullScreenErrorTitleTextColor?.let { this.chatFullScreenErrorTitleTextColor = it } }

        fun setChatFullScreenErrorTitleTextAppearance(@StyleRes chatFullScreenErrorTitleTextAppearance: Int?) =
            apply { chatFullScreenErrorTitleTextAppearance?.let { this.chatFullScreenErrorTitleTextAppearance = it } }

        fun setChatFullScreenErrorDescriptionText(chatFullScreenErrorDescriptionText: String?) =
            apply { chatFullScreenErrorDescriptionText?.let { this.chatFullScreenErrorDescriptionText = it } }

        fun setChatFullScreenErrorDescriptionTextRes(@StringRes chatFullScreenErrorDescriptionTextRes: Int?) =
            apply { chatFullScreenErrorDescriptionTextRes?.let { this.chatFullScreenErrorDescriptionTextRes = it } }

        fun setChatFullScreenErrorDescriptionTextColor(@ColorInt chatFullScreenErrorDescriptionTextColor: Int?) =
            apply { chatFullScreenErrorDescriptionTextColor?.let { this.chatFullScreenErrorDescriptionTextColor = it } }

        fun setChatFullScreenErrorDescriptionTextAppearance(@StyleRes chatFullScreenErrorDescriptionTextAppearance: Int?) =
            apply {
                chatFullScreenErrorDescriptionTextAppearance?.let {
                    this.chatFullScreenErrorDescriptionTextAppearance = it
                }
            }

        fun setChatFullScreenErrorBackgroundColor(@ColorInt chatFullScreenErrorBackgroundColor: Int?) =
            apply { chatFullScreenErrorBackgroundColor?.let { this.chatFullScreenErrorBackgroundColor = it } }

        fun setChatFullScreenErrorIcon(chatFullScreenErrorIcon: Drawable?) =
            apply { chatFullScreenErrorIcon?.let { this.chatFullScreenErrorIcon = it } }

        fun setChatFullScreenErrorIconTint(@ColorInt chatFullScreenErrorIconTint: Int?) =
            apply { chatFullScreenErrorIconTint?.let { this.chatFullScreenErrorIconTint = it } }

        fun setChatFullScreenErrorRefreshButtonText(chatFullScreenErrorRefreshButtonText: String?) =
            apply { chatFullScreenErrorRefreshButtonText?.let { this.chatFullScreenErrorRefreshButtonText = it } }

        fun setChatFullScreenErrorRefreshButtonTextRes(@StringRes chatFullScreenErrorRefreshButtonTextRes: Int?) =
            apply { chatFullScreenErrorRefreshButtonTextRes?.let { this.chatFullScreenErrorRefreshButtonTextRes = it } }

        fun setChatFullScreenErrorRefreshButtonTextColor(@ColorInt chatFullScreenErrorRefreshButtonTextColor: Int?) =
            apply {
                chatFullScreenErrorRefreshButtonTextColor?.let {
                    this.chatFullScreenErrorRefreshButtonTextColor = it
                }
            }

        fun setChatFullScreenErrorRefreshButtonVisible(chatFullScreenErrorRefreshButtonVisible: Boolean?) =
            apply { chatFullScreenErrorRefreshButtonVisible?.let { this.chatFullScreenErrorRefreshButtonVisible = it } }

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
            chatSnackbarErrorTextColor = chatSnackbarErrorTextColor,
            chatSnackbarErrorTextAppearance = chatSnackbarErrorTextAppearance,
            chatSnackbarErrorBackgroundColor = chatSnackbarErrorBackgroundColor,
            chatSnackbarErrorIcon = chatSnackbarErrorIcon,
            chatSnackbarErrorIconTint = chatSnackbarErrorIconTint,
            chatFullScreenErrorTitleText = chatFullScreenErrorTitleText,
            chatFullScreenErrorTitleTextRes = chatFullScreenErrorTitleTextRes,
            chatFullScreenErrorTitleTextColor = chatFullScreenErrorTitleTextColor,
            chatFullScreenErrorTitleTextAppearance = chatFullScreenErrorTitleTextAppearance,
            chatFullScreenErrorDescriptionText = chatFullScreenErrorDescriptionText,
            chatFullScreenErrorDescriptionTextRes = chatFullScreenErrorDescriptionTextRes,
            chatFullScreenErrorDescriptionTextColor = chatFullScreenErrorDescriptionTextColor,
            chatFullScreenErrorDescriptionTextAppearance = chatFullScreenErrorDescriptionTextAppearance,
            chatFullScreenErrorBackgroundColor = chatFullScreenErrorBackgroundColor,
            chatFullScreenErrorIcon = chatFullScreenErrorIcon,
            chatFullScreenErrorIconTint = chatFullScreenErrorIconTint,
            chatFullScreenErrorRefreshButtonText = chatFullScreenErrorRefreshButtonText,
            chatFullScreenErrorRefreshButtonTextRes = chatFullScreenErrorRefreshButtonTextRes,
            chatFullScreenErrorRefreshButtonTextColor = chatFullScreenErrorRefreshButtonTextColor,
            chatFullScreenErrorRefreshButtonVisible = chatFullScreenErrorRefreshButtonVisible,
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
                    null
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

                val chatSnackbarErrorTextColor = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatSnackbarErrorTextColor,
                    0
                ).takeIfDefined()

                val chatSnackbarErrorTextAppearance = getResourceId(
                    R.styleable.InAppChatViewStyleable_ibChatSnackbarErrorTextAppearance,
                    0
                ).takeIfDefined()

                val chatSnackbarErrorBackgroundColor = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatSnackbarErrorBackgroundColor,
                    0
                ).takeIfDefined()

                val chatSnackbarErrorIcon = getResourceId(
                    R.styleable.InAppChatViewStyleable_ibChatSnackbarErrorIcon,
                    0
                ).takeIfDefined()

                val chatSnackbarErrorIconTint = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatSnackbarErrorIconTint,
                    0
                ).takeIfDefined()

                val (errorTitleTextRes, errorTitleText) = resolveStringWithResId(
                    context,
                    R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorTitleText,
                    Defaults.errorTitleTextRes
                )

                val errorTitleTextColor = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorTitleTextColor,
                    Defaults.errorTitleTextColor
                )

                val errorTitleTextAppearance = getResourceId(
                    R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorTitleTextAppearance,
                    0
                ).takeIfDefined()

                val (errorDescriptionTextRes, errorDescriptionText) = resolveStringWithResId(
                    context,
                    R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorDescriptionText,
                    Defaults.errorDescriptionTextRes
                )

                val errorDescriptionTextColor = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorDescriptionTextColor,
                    Defaults.errorDescriptionTextColor
                )

                val errorDescriptionTextAppearance = getResourceId(
                    R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorDescriptionTextAppearance,
                    0
                ).takeIfDefined()

                val errorBackgroundColor = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorBackgroundColor,
                    Defaults.errorBackgroundColor
                )

                val errorIcon = getResourceId(
                    R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorIcon,
                    R.drawable.ib_chat_error
                )

                val errorIconTint = getColor(
                    R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorIconTint,
                    0
                ).takeIfDefined()

                val (errorRefreshButtonTextRes, errorRefreshButtonText) = resolveStringWithResId(
                    context,
                    R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorRefreshButtonText,
                    Defaults.errorRefreshButtonTextRes
                )

                val errorRefreshButtonTextColor =
                    widgetInfo?.colorPrimary?.takeIf { isIbDefaultTheme || !hasValue(R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorRefreshButtonTextColor) }
                        ?: getColor(
                            R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorRefreshButtonTextColor,
                            Defaults.errorRefreshButtonTextColor
                        )

                val errorRefreshButtonVisible = getBoolean(
                    R.styleable.InAppChatViewStyleable_ibChatFullScreenErrorRefreshButtonVisible,
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
                    chatSnackbarErrorTextColor = chatSnackbarErrorTextColor,
                    chatSnackbarErrorBackgroundColor = chatSnackbarErrorBackgroundColor,
                    chatSnackbarErrorIcon = chatSnackbarErrorIcon?.let { context.getDrawableCompat(it) },
                    chatSnackbarErrorIconTint = chatSnackbarErrorIconTint,
                    chatFullScreenErrorTitleText = errorTitleText,
                    chatFullScreenErrorTitleTextRes = errorTitleTextRes,
                    chatFullScreenErrorTitleTextColor = errorTitleTextColor,
                    chatFullScreenErrorTitleTextAppearance = errorTitleTextAppearance,
                    chatFullScreenErrorDescriptionText = errorDescriptionText,
                    chatFullScreenErrorDescriptionTextRes = errorDescriptionTextRes,
                    chatFullScreenErrorDescriptionTextColor = errorDescriptionTextColor,
                    chatFullScreenErrorDescriptionTextAppearance = errorDescriptionTextAppearance,
                    chatFullScreenErrorBackgroundColor = errorBackgroundColor,
                    chatFullScreenErrorIcon = context.getDrawableCompat(errorIcon),
                    chatFullScreenErrorIconTint = errorIconTint,
                    chatFullScreenErrorRefreshButtonText = errorRefreshButtonText,
                    chatFullScreenErrorRefreshButtonTextRes = errorRefreshButtonTextRes,
                    chatFullScreenErrorRefreshButtonTextColor = errorRefreshButtonTextColor,
                    chatFullScreenErrorRefreshButtonVisible = errorRefreshButtonVisible,
                )
            }
        }
    }
}
