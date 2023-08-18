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
import org.infobip.mobile.messaging.chat.utils.takeIfDefined

data class InAppChatInputViewStyle(
        @StyleRes val textAppearance: Int? = null,
        @ColorInt val textColor: Int,
        @ColorInt val backgroundColor: Int,
        val hintText: String? = null,
        @StringRes val hintTextRes: Int? = null,
        @ColorInt val hintTextColor: Int,
        val attachmentIcon: Drawable? = null,
        val attachmentIconTint: ColorStateList? = null,
        val sendIcon: Drawable? = null,
        val sendIconTint: ColorStateList? = null,
        @ColorInt val separatorLineColor: Int,
        val isSeparatorLineVisible: Boolean,
        @ColorInt val cursorColor: Int,
) {
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
                val hintTextNonResource = getNonResourceString(R.styleable.InAppChatInputViewStyleable_ibChatInputHintText)
                var hintText: String?
                var hintTextRes: Int? = null
                if (hintTextNonResource != null) {
                    hintText = hintTextNonResource
                } else {
                    hintText = getString(R.styleable.InAppChatInputViewStyleable_ibChatInputHintText)
                    hintTextRes = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputHintText, R.string.ib_chat_message_hint).takeIfDefined()
                }
                if (hintTextRes != null && hintText == null) {
                    hintText = context.getString(hintTextRes)
                }
                val hintTextColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputHintTextColor, context.getColorCompat(R.color.ib_chat_hint_text_color))
                val attachmentIcon = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIcon, 0).takeIfDefined()
                val widgetConfigButtonTintList = context.createButtonTintList(widgetInfo)
                val attachmentIconTint = widgetConfigButtonTintList.takeIf { isDefaultTheme }
                        ?: getColorStateList(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIconTint)
                        ?: widgetConfigButtonTintList
                        ?: buttonDefaultTint
                val sendIcon = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputSendIcon, 0).takeIfDefined()
                val sendIconTint = widgetConfigButtonTintList.takeIf { isDefaultTheme }
                        ?: getColorStateList(R.styleable.InAppChatInputViewStyleable_ibChatInputSendIconTint)
                        ?: widgetConfigButtonTintList
                        ?: buttonDefaultTint
                val separatorLineColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputSeparatorLineColor, context.getColorCompat(R.color.ib_chat_separator_color))
                val isSeparatorLineVisible = getBoolean(R.styleable.InAppChatInputViewStyleable_ibChatInputSeparatorLineVisible, true)
                val cursorColor = widgetInfo?.colorPrimary?.takeIf { isDefaultTheme }
                        ?: getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputCursorColor, widgetInfo?.colorPrimary
                                ?: textColor)

                recycle()
                return InAppChatInputViewStyle(
                        textAppearance,
                        textColor,
                        backgroundColor,
                        hintText,
                        hintTextRes,
                        hintTextColor,
                        attachmentIcon?.let(context::getDrawableCompat),
                        attachmentIconTint,
                        sendIcon?.let(context::getDrawableCompat),
                        sendIconTint,
                        separatorLineColor,
                        isSeparatorLineVisible,
                        cursorColor
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