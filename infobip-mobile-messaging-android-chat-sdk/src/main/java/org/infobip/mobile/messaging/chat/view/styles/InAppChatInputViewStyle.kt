package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.colorBackground
import org.infobip.mobile.messaging.chat.utils.colorPrimary
import org.infobip.mobile.messaging.chat.utils.colorStateListOf
import org.infobip.mobile.messaging.chat.utils.getDrawableCompat
import org.infobip.mobile.messaging.chat.utils.isIbDefaultTheme
import org.infobip.mobile.messaging.chat.utils.resolveStringWithResId
import org.infobip.mobile.messaging.chat.utils.takeIfDefined

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
) {
    object Defaults {
        @StyleRes
        val textAppearance: Int = R.style.IB_Chat_Input_TextAppearance
        @ColorInt
        val textColor: Int = Color.parseColor("#242424")
        @ColorInt
        val backgroundColor: Int = Color.WHITE
        @StringRes
        val hintTextRes: Int = R.string.ib_chat_message_hint
        @ColorInt
        val hintTextColor: Int = Color.parseColor("#808080")
        val iconTint: ColorStateList = colorStateListOf(
            intArrayOf(-android.R.attr.state_enabled) to Color.parseColor("#808080"),
            intArrayOf(android.R.attr.state_enabled) to Color.BLACK,
        )
        @ColorInt
        val separatorLineColor: Int = Color.parseColor("#19000000")
        val isSeparatorLineVisible: Boolean = true
    }

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
            cursorColor = cursorColor
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
            widgetInfo: WidgetInfo?
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
                val backgroundColor = widgetInfo?.colorBackground.takeIf { isDefaultTheme }
                    ?: getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputBackgroundColor, Defaults.backgroundColor)
                val (hintTextRes, hintText) = resolveStringWithResId(context, R.styleable.InAppChatInputViewStyleable_ibChatInputHintText, Defaults.hintTextRes)
                val hintTextColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputHintTextColor, Defaults.hintTextColor)
                val attachmentIcon = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIcon, R.drawable.ib_chat_attachment_btn_icon).takeIfDefined()
                val widgetConfigButtonTintList = widgetInfo.buttonTintList
                val attachmentIconTint = widgetConfigButtonTintList.takeIf { isDefaultTheme }
                    ?: getColorStateList(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIconTint)
                    ?: widgetConfigButtonTintList
                    ?: Defaults.iconTint
                val attachmentBackgroundDrawable = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentBackgroundDrawable, 0).takeIfDefined()
                val attachmentBackgroundColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentBackgroundColor, 0).takeIfDefined()
                val sendIcon = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputSendIcon, R.drawable.ib_chat_send_btn_icon).takeIfDefined()
                val sendIconTint = widgetConfigButtonTintList.takeIf { isDefaultTheme }
                    ?: getColorStateList(R.styleable.InAppChatInputViewStyleable_ibChatInputSendIconTint)
                    ?: widgetConfigButtonTintList
                    ?: Defaults.iconTint
                val sendBackgroundDrawable = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputSendBackgroundDrawable, 0).takeIfDefined()
                val sendBackgroundColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputSendBackgroundColor, 0).takeIfDefined()
                val separatorLineColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputSeparatorLineColor, Defaults.separatorLineColor)
                val isSeparatorLineVisible = getBoolean(R.styleable.InAppChatInputViewStyleable_ibChatInputSeparatorLineVisible, Defaults.isSeparatorLineVisible)
                val cursorColor = widgetInfo?.colorPrimary?.takeIf { isDefaultTheme }
                    ?: getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputCursorColor, textColor)

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
                    cursorColor = cursorColor
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