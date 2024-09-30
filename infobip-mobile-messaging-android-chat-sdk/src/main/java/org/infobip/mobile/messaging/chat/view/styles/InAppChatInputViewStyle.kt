package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.colorBackground
import org.infobip.mobile.messaging.chat.utils.colorPrimary
import org.infobip.mobile.messaging.chat.utils.colorStateListOf
import org.infobip.mobile.messaging.chat.utils.getColorCompat
import org.infobip.mobile.messaging.chat.utils.getDrawableCompat
import org.infobip.mobile.messaging.chat.utils.isIbDefaultTheme
import org.infobip.mobile.messaging.chat.utils.resolveStringWithResId
import org.infobip.mobile.messaging.chat.utils.takeIfDefined

data class InAppChatInputViewStyle(
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
        @StyleRes val textAppearance: Int = R.style.IB_Chat_Input_TextAppearance
        @ColorInt val textColor: Int = Color.parseColor("#242424")
        @ColorInt val backgroundColor: Int = Color.WHITE
        @StringRes val hintTextRes: Int = R.string.ib_chat_message_hint
        @ColorInt val hintTextColor: Int = Color.parseColor("#808080")
        val iconTint: ColorStateList = colorStateListOf(
            intArrayOf(-android.R.attr.state_enabled) to Color.parseColor("#808080"),
            intArrayOf(android.R.attr.state_enabled) to Color.BLACK,
        )
        @ColorInt val separatorLineColor: Int = Color.parseColor("#19000000")
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
         * Creates [InAppChatInputViewStyle] only from android style inside "IB_AppTheme.Chat" theme,
         * defined by "ibChatInputStyle" attribute provided by integrator.
         * If "ibChatInputStyle" attribute is not defined, default IB style "IB.Chat.Input" is used.
         * Applies [WidgetInfo] livechat widget configuration into existing [InAppChatInputViewStyle] from android style inside "IB_AppTheme.Chat" theme,
         * defined by "ibChatInputStyle" attribute provided by integrator.
         * Priority: IB_AppTheme.Chat ibChatInputStyle > [WidgetInfo] > IB.Chat.Input style
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

                val buttonDefaultTint by lazy {
                    val colorPrimary = TypedValue().run {
                        context.theme.resolveAttribute(R.attr.colorPrimary, this, true)
                        this.data
                    }

                    colorStateListOf(
                        intArrayOf(-android.R.attr.state_enabled) to context.getColorCompat(R.color.ib_chat_hint_text_color),
                        intArrayOf(android.R.attr.state_enabled) to colorPrimary,
                    )
                }

                val textAppearance = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputTextAppearance, 0).takeIfDefined()
                val textColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputTextColor, context.getColorCompat(R.color.ib_chat_text_color))
                val backgroundColor = widgetInfo?.colorBackground.takeIf { isDefaultTheme }
                    ?: getColor(
                        R.styleable.InAppChatInputViewStyleable_ibChatInputBackgroundColor,
                        widgetInfo?.colorBackground ?: Color.WHITE
                    )

                val (hintTextRes, hintText) = resolveStringWithResId(context, R.styleable.InAppChatInputViewStyleable_ibChatInputHintText, R.string.ib_chat_message_hint)
                val hintTextColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputHintTextColor, context.getColorCompat(R.color.ib_chat_hint_text_color))
                val attachmentIcon = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIcon, 0).takeIfDefined()
                val widgetConfigButtonTintList = context.createButtonTintList(widgetInfo)
                val attachmentIconTint = widgetConfigButtonTintList.takeIf { isDefaultTheme }
                    ?: getColorStateList(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIconTint)
                    ?: widgetConfigButtonTintList
                    ?: buttonDefaultTint
                val attachmentBackgroundDrawable = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentBackgroundDrawable, 0).takeIfDefined()
                val attachmentBackgroundColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentBackgroundColor, 0).takeIfDefined()
                val sendIcon = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputSendIcon, 0).takeIfDefined()
                val sendIconTint = widgetConfigButtonTintList.takeIf { isDefaultTheme }
                    ?: getColorStateList(R.styleable.InAppChatInputViewStyleable_ibChatInputSendIconTint)
                    ?: widgetConfigButtonTintList
                    ?: buttonDefaultTint
                val sendBackgroundDrawable = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputSendBackgroundDrawable, 0).takeIfDefined()
                val sendBackgroundColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputSendBackgroundColor, 0).takeIfDefined()
                val separatorLineColor =
                    getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputSeparatorLineColor, context.getColorCompat(R.color.ib_chat_separator_color))
                val isSeparatorLineVisible = getBoolean(R.styleable.InAppChatInputViewStyleable_ibChatInputSeparatorLineVisible, true)
                val cursorColor = widgetInfo?.colorPrimary?.takeIf { isDefaultTheme }
                    ?: getColor(
                        R.styleable.InAppChatInputViewStyleable_ibChatInputCursorColor, widgetInfo?.colorPrimary
                            ?: textColor
                    )

                recycle()
                return InAppChatInputViewStyle(
                    textAppearance = textAppearance,
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    hintText = hintText,
                    hintTextRes = hintTextRes,
                    hintTextColor = hintTextColor,
                    attachmentIcon = attachmentIcon?.let(context::getDrawableCompat),
                    attachmentIconTint = attachmentIconTint,
                    attachmentBackgroundDrawable = attachmentBackgroundDrawable?.let(context::getDrawableCompat),
                    attachmentBackgroundColor = attachmentBackgroundColor,
                    sendIcon = sendIcon?.let(context::getDrawableCompat),
                    sendIconTint = sendIconTint,
                    sendBackgroundDrawable = sendBackgroundDrawable?.let(context::getDrawableCompat),
                    sendBackgroundColor = sendBackgroundColor,
                    separatorLineColor = separatorLineColor,
                    isSeparatorLineVisible = isSeparatorLineVisible,
                    cursorColor = cursorColor
                )
            }
        }

        private fun Context.createButtonTintList(widgetInfo: WidgetInfo?): ColorStateList? {
            return widgetInfo?.colorPrimary?.let { colorPrimary ->
                colorStateListOf(
                    intArrayOf(-android.R.attr.state_enabled) to getColorCompat(R.color.ib_chat_hint_text_color),
                    intArrayOf(android.R.attr.state_enabled) to colorPrimary,
                )
            }
        }
    }

}