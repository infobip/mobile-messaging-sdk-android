/*
 * InAppChatInputViewStyle.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
import android.content.res.ColorStateList
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
import org.infobip.mobile.messaging.chat.utils.colorStateListOf
import org.infobip.mobile.messaging.chat.utils.getDrawableCompat
import org.infobip.mobile.messaging.chat.utils.isIbDefaultTheme
import org.infobip.mobile.messaging.chat.utils.resolveStringWithResId
import org.infobip.mobile.messaging.chat.utils.takeIfDefined

/**
 * Style configuration for the InAppChat message input field and buttons.
 *
 * This data class defines the visual appearance of the message input area,
 * including the text input field, attachment button, send button, separator line,
 * cursor, and character counter.
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
 * val inputStyle = InAppChatInputViewStyle.Builder()
 *     .setTextColor(Color.BLACK)
 *     .setBackgroundColor(Color.WHITE)
 *     .setHintText("Type a message...")
 *     .setSendIconTint(colorStateListOf(Color.GRAY, Color.BLUE))
 *     .build()
 *
 * val theme = InAppChatTheme(
 *     chatToolbarStyle = toolbarStyle,
 *     attachmentToolbarStyle = attachmentStyle,
 *     chatStyle = chatStyle,
 *     chatInputViewStyle = inputStyle
 * )
 * ```
 *
 * ### XML Theme Configuration Example
 * ```xml
 * <style name="MyChat.Input" parent="IB.Chat.Input">
 *     <item name="ibChatInputTextColor">@color/dark_text</item>
 *     <item name="ibChatInputBackgroundColor">@color/light_background</item>
 *     <item name="ibChatInputHintText">@string/type_message</item>
 * </style>
 *
 * <style name="IB_AppTheme.Chat" parent="IB_ChatDefaultTheme">
 *     <item name="ibChatInputStyle">@style/MyChat.Input</item>
 * </style>
 * ```
 *
 * @see InAppChatTheme for complete theme configuration
 * @see Builder for programmatic customization
 * @see Defaults for default values
 */
data class InAppChatInputViewStyle @JvmOverloads constructor(
    @StyleRes val textAppearance: Int? = Defaults.textAppearance,
    @ColorInt val textColor: Int = Defaults.textColor,
    @ColorInt val backgroundColor: Int = Defaults.backgroundColor,
    val hintText: String? = null,
    @StringRes val hintTextRes: Int? = Defaults.hintTextRes,
    @ColorInt val hintTextColor: Int = Defaults.hintTextColor,
    val attachmentIcon: Drawable? = null,
    val attachmentIconTint: ColorStateList? = Defaults.iconTint,
    val attachmentBackgroundDrawable: Drawable? = null,
    @ColorInt val attachmentBackgroundColor: Int? = null,
    val sendIcon: Drawable? = null,
    val sendIconTint: ColorStateList? = Defaults.iconTint,
    val sendBackgroundDrawable: Drawable? = null,
    @ColorInt val sendBackgroundColor: Int? = null,
    @ColorInt val separatorLineColor: Int = Defaults.separatorLineColor,
    val isSeparatorLineVisible: Boolean = Defaults.isSeparatorLineVisible,
    @ColorInt val cursorColor: Int = Defaults.textColor,
    @StyleRes val charCounterTextAppearance: Int? = Defaults.charCounterTextAppearance,
    @ColorInt val charCounterDefaultColor: Int = Defaults.charCounterDefaultColor,
    @ColorInt val charCounterAlertColor: Int = Defaults.charCounterAlertColor,
) {
    /**
     * Default values for [InAppChatInputViewStyle] properties.
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
        @StyleRes val textAppearance: Int = R.style.IB_Chat_Input_TextAppearance
        @ColorInt val textColor: Int = "#242424".toColorInt()
        @ColorInt val backgroundColor: Int = Color.WHITE
        @StringRes val hintTextRes: Int = R.string.ib_chat_message_hint
        @ColorInt val hintTextColor: Int = "#808080".toColorInt()
        val iconTint: ColorStateList = colorStateListOf(
            intArrayOf(-android.R.attr.state_enabled) to "#808080".toColorInt(),
            intArrayOf(android.R.attr.state_enabled) to Color.BLACK,
        )
        @ColorInt val separatorLineColor: Int = "#19000000".toColorInt()
        val isSeparatorLineVisible: Boolean = true
        @StyleRes val charCounterTextAppearance: Int? = null
        @ColorInt val charCounterDefaultColor: Int = "#808080".toColorInt()
        @ColorInt val charCounterAlertColor: Int = "#F44336".toColorInt()
    }

    /**
     * Fluent builder for creating [InAppChatInputViewStyle] instances with customized properties.
     *
     * All setter methods return the builder instance for method chaining.
     *
     * Example:
     * ```kotlin
     * val style = InAppChatInputViewStyle.Builder()
     *     .setTextColor(Color.BLACK)
     *     .setBackgroundColor(Color.WHITE)
     *     .setHintText("Type message...")
     *     .build()
     * ```
     */
    class Builder {
        private var textAppearance: Int = Defaults.textAppearance
        private var textColor: Int = Defaults.textColor
        private var backgroundColor: Int = Defaults.backgroundColor
        private var hintText: String? = null
        private var hintTextRes: Int = Defaults.hintTextRes
        private var hintTextColor: Int = Defaults.hintTextColor
        private var attachmentIcon: Drawable? = null
        private var attachmentIconTint: ColorStateList? = Defaults.iconTint
        private var attachmentBackgroundDrawable: Drawable? = null
        private var attachmentBackgroundColor: Int? = null
        private var sendIcon: Drawable? = null
        private var sendIconTint: ColorStateList? = Defaults.iconTint
        private var sendBackgroundDrawable: Drawable? = null
        private var sendBackgroundColor: Int? = null
        private var separatorLineColor: Int = Defaults.separatorLineColor
        private var isSeparatorLineVisible: Boolean = Defaults.isSeparatorLineVisible
        private var cursorColor: Int = Defaults.textColor
        private var charCounterTextAppearance: Int? = Defaults.charCounterTextAppearance
        private var charCounterDefaultColor: Int = Defaults.charCounterDefaultColor
        private var charCounterAlertColor: Int = Defaults.charCounterAlertColor

        fun setTextAppearance(@StyleRes textAppearance: Int?) = apply { textAppearance?.let { this.textAppearance = it } }
        fun setTextColor(@ColorInt textColor: Int?) = apply { textColor?.let { this.textColor = it } }
        fun setBackgroundColor(@ColorInt backgroundColor: Int?) = apply { backgroundColor?.let { this.backgroundColor = it } }
        fun setHintText(hintText: String?) = apply { hintText?.let { this.hintText = it } }
        fun setHintTextRes(@StringRes hintTextRes: Int?) = apply { hintTextRes?.let { this.hintTextRes = it } }
        fun setHintTextColor(@ColorInt hintTextColor: Int?) = apply { hintTextColor?.let { this.hintTextColor = it } }
        fun setAttachmentIcon(attachmentIcon: Drawable?) = apply { attachmentIcon?.let { this.attachmentIcon = it } }
        fun setAttachmentIconTint(attachmentIconTint: ColorStateList?) = apply { attachmentIconTint?.let { this.attachmentIconTint = it } }
        fun setAttachmentBackgroundDrawable(attachmentBackgroundDrawable: Drawable?) = apply { attachmentBackgroundDrawable?.let { this.attachmentBackgroundDrawable = it } }
        fun setAttachmentBackgroundColor(@ColorInt attachmentBackgroundColor: Int?) = apply { attachmentBackgroundColor?.let { this.attachmentBackgroundColor = it } }
        fun setSendIcon(sendIcon: Drawable?) = apply { sendIcon?.let { this.sendIcon = it } }
        fun setSendIconTint(sendIconTint: ColorStateList?) = apply { sendIconTint?.let { this.sendIconTint = it } }
        fun setSendBackgroundDrawable(sendBackgroundDrawable: Drawable?) = apply { sendBackgroundDrawable?.let { this.sendBackgroundDrawable = it } }
        fun setSendBackgroundColor(@ColorInt sendBackgroundColor: Int?) = apply { sendBackgroundColor?.let { this.sendBackgroundColor = it } }
        fun setSeparatorLineColor(@ColorInt separatorLineColor: Int?) = apply { separatorLineColor?.let { this.separatorLineColor = it } }
        fun setIsSeparatorLineVisible(isSeparatorLineVisible: Boolean?) = apply { isSeparatorLineVisible?.let { this.isSeparatorLineVisible = it } }
        fun setCursorColor(@ColorInt cursorColor: Int?) = apply { cursorColor?.let { this.cursorColor = it } }
        fun setCharCounterTextAppearance(@StyleRes textAppearance: Int?) = apply { textAppearance?.let { this.charCounterTextAppearance = it } }
        fun setCharCounterDefaultColor(@ColorInt charCounterDefaultColor: Int?) = apply { charCounterDefaultColor?.let { this.charCounterDefaultColor = it } }
        fun setCharCounterAlertColor(@ColorInt charCounterAlertColor: Int?) = apply { charCounterAlertColor?.let { this.charCounterAlertColor = it } }

        fun build() = InAppChatInputViewStyle(
            textAppearance = textAppearance,
            textColor = textColor,
            backgroundColor = backgroundColor,
            hintText = hintText,
            hintTextRes = hintTextRes,
            hintTextColor = hintTextColor,
            attachmentIcon = attachmentIcon,
            attachmentIconTint = attachmentIconTint,
            attachmentBackgroundDrawable = attachmentBackgroundDrawable,
            attachmentBackgroundColor = attachmentBackgroundColor,
            sendIcon = sendIcon,
            sendIconTint = sendIconTint,
            sendBackgroundDrawable = sendBackgroundDrawable,
            sendBackgroundColor = sendBackgroundColor,
            separatorLineColor = separatorLineColor,
            isSeparatorLineVisible = isSeparatorLineVisible,
            cursorColor = cursorColor,
            charCounterTextAppearance = charCounterTextAppearance,
            charCounterDefaultColor = charCounterDefaultColor,
            charCounterAlertColor = charCounterAlertColor
        )
    }

    companion object {

        /**
         * Creates [InAppChatInputViewStyle] only from android style inside `IB_AppTheme.Chat` theme,
         * defined by `ibChatInputStyle` attribute provided by integrator.
         * If `ibChatInputStyle` attribute is not defined, default IB style `IB.Chat.Input` is used.
         * Applies [WidgetInfo] livechat widget configuration into existing [InAppChatInputViewStyle] from android style inside `IB_AppTheme.Chat` theme,
         * defined by `ibChatInputStyle` attribute provided by integrator.
         * Priority: `IB_AppTheme.Chat.ibChatInputStyle` > [WidgetInfo] > `IB.Chat.Input` style
         */
        internal operator fun invoke(
            context: Context,
            attrs: AttributeSet?,
            widgetInfo: WidgetInfo?,
        ): InAppChatInputViewStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.InAppChatInputViewStyleable,
                R.attr.ibChatInputStyle,
                R.style.IB_Chat_Input
            ).run {
                val isDefaultTheme = context.theme.isIbDefaultTheme()

                val textAppearance = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputTextAppearance, 0).takeIfDefined()
                val textColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputTextColor, Defaults.textColor)
                val backgroundColor = widgetInfo?.colorBackground.takeIf { isDefaultTheme || !hasValue(R.styleable.InAppChatInputViewStyleable_ibChatInputBackgroundColor) }
                    ?: getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputBackgroundColor, Defaults.backgroundColor)
                val (hintTextRes, hintText) = resolveStringWithResId(context, R.styleable.InAppChatInputViewStyleable_ibChatInputHintText, Defaults.hintTextRes)
                val hintTextColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputHintTextColor, Defaults.hintTextColor)
                val attachmentIcon = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIcon, R.drawable.ib_chat_attachment_btn_icon).takeIfDefined()
                val widgetConfigButtonTintList = widgetInfo.buttonTintList
                val attachmentIconTint = widgetConfigButtonTintList.takeIf { isDefaultTheme || !hasValue(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIconTint) }
                    ?: getColorStateList(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIconTint)
                    ?: widgetConfigButtonTintList
                    ?: Defaults.iconTint
                val attachmentBackgroundDrawable = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentBackgroundDrawable, 0).takeIfDefined()
                val attachmentBackgroundColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentBackgroundColor, 0).takeIfDefined()
                val sendIcon = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputSendIcon, R.drawable.ib_chat_send_btn_icon).takeIfDefined()
                val sendIconTint = widgetConfigButtonTintList.takeIf { isDefaultTheme || !hasValue(R.styleable.InAppChatInputViewStyleable_ibChatInputSendIconTint) }
                    ?: getColorStateList(R.styleable.InAppChatInputViewStyleable_ibChatInputSendIconTint)
                    ?: widgetConfigButtonTintList
                    ?: Defaults.iconTint
                val sendBackgroundDrawable = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputSendBackgroundDrawable, 0).takeIfDefined()
                val sendBackgroundColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputSendBackgroundColor, 0).takeIfDefined()
                val separatorLineColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputSeparatorLineColor, Defaults.separatorLineColor)
                val isSeparatorLineVisible = getBoolean(R.styleable.InAppChatInputViewStyleable_ibChatInputSeparatorLineVisible, Defaults.isSeparatorLineVisible)
                val cursorColor = widgetInfo?.colorPrimary?.takeIf { isDefaultTheme || !hasValue(R.styleable.InAppChatInputViewStyleable_ibChatInputCursorColor) }
                    ?: getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputCursorColor, textColor)
                val charCounterTextAppearance = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputCharCounterTextAppearance, 0).takeIfDefined()
                val charCounterDefaultColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputCharCounterDefaultColor, Defaults.charCounterDefaultColor)
                val charCounterAlertColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputCharCounterAlertColor, Defaults.charCounterAlertColor)

                recycle()
                return InAppChatInputViewStyle(
                    textAppearance = textAppearance,
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    hintText = hintText,
                    hintTextRes = hintTextRes,
                    hintTextColor = hintTextColor,
                    attachmentIcon = (attachmentIcon ?: R.drawable.ib_chat_attachment_btn_icon).let(context::getDrawableCompat),
                    attachmentIconTint = attachmentIconTint,
                    attachmentBackgroundDrawable = attachmentBackgroundDrawable?.let(context::getDrawableCompat),
                    attachmentBackgroundColor = attachmentBackgroundColor,
                    sendIcon = (sendIcon ?: R.drawable.ib_chat_send_btn_icon).let(context::getDrawableCompat),
                    sendIconTint = sendIconTint,
                    sendBackgroundDrawable = sendBackgroundDrawable?.let(context::getDrawableCompat),
                    sendBackgroundColor = sendBackgroundColor,
                    separatorLineColor = separatorLineColor,
                    isSeparatorLineVisible = isSeparatorLineVisible,
                    cursorColor = cursorColor,
                    charCounterTextAppearance = charCounterTextAppearance,
                    charCounterDefaultColor = charCounterDefaultColor,
                    charCounterAlertColor = charCounterAlertColor
                )
            }
        }

        private val WidgetInfo?.buttonTintList: ColorStateList?
            get() {
                return this?.colorPrimary?.let { colorPrimary ->
                    colorStateListOf(
                        intArrayOf(-android.R.attr.state_enabled) to Defaults.hintTextColor,
                        intArrayOf(android.R.attr.state_enabled) to colorPrimary,
                    )
                }
            }
    }
}
