package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.*

data class InAppChatInputViewStyle(
    @StyleRes val textAppearance: Int? = null,
    @ColorInt val backgroundColor: Int,
    val hintText: String? = null,
    @StringRes val hintTextRes: Int? = null,
    @ColorInt val hintTextColor: Int,
    @DrawableRes val attachmentIcon: Int? = null,
    val attachmentIconTint: ColorStateList? = null,
    @DrawableRes val sendIcon: Int? = null,
    val sendIconTint: ColorStateList? = null,
    @ColorInt val separatorLineColor: Int,
    val isSeparatorLineVisible: Boolean,
) {
    companion object {

        /**
         * Creates [InAppChatInputViewStyle] only from android style inside "IB_AppTheme.Chat" theme,
         * defined by "ibChatInputStyle" attribute provided by integrator.
         * If "ibChatInputStyle" attribute is not defined, default IB style "IB.Chat.Input" is used.
         * Priority: IB_AppTheme.Chat - ibChatInputStyle > IB.Chat.Input style
         */
        internal operator fun invoke(
            context: Context,
            attrs: AttributeSet?
        ): InAppChatInputViewStyle {
            context.obtainStyledAttributes(
                attrs,
                R.styleable.InAppChatInputViewStyleable,
                R.attr.ibChatInputStyle,
                R.style.IB_Chat_Input
            ).run {
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
                val backgroundColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputBackgroundColor, Color.WHITE)
                val hintTextNonResource = getNonResourceString(R.styleable.InAppChatInputViewStyleable_ibChatInputHintText)
                var hintText: String?
                var hintTextRes: Int? = null
                if (hintTextNonResource != null) {
                    hintText = hintTextNonResource
                } else {
                    hintText = getString(R.styleable.InAppChatInputViewStyleable_ibChatInputHintText)
                    hintTextRes = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputHintText, R.string.ib_chat_message_hint).takeIfDefined()
                }
                if (hintTextRes != null && hintText == null){
                    hintText = context.getString(hintTextRes)
                }
                val hintTextColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputHintTextColor, context.getColorCompat(R.color.ib_chat_hint_text_color))
                val attachmentIcon = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIcon, 0).takeIfDefined()
                val attachmentIconTint = getColorStateList(R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIconTint) ?: buttonDefaultTint
                val sendIcon = getResourceId(R.styleable.InAppChatInputViewStyleable_ibChatInputSendIcon, 0).takeIfDefined()
                val sendIconTint = getColorStateList(R.styleable.InAppChatInputViewStyleable_ibChatInputSendIconTint) ?: buttonDefaultTint
                val separatorLineColor = getColor(R.styleable.InAppChatInputViewStyleable_ibChatInputSeparatorLineColor, context.getColorCompat(R.color.ib_chat_separator_color))
                val isSeparatorLineVisible = getBoolean(R.styleable.InAppChatInputViewStyleable_ibChatInputSeparatorLineVisible, true)

                recycle()
                return InAppChatInputViewStyle(
                    textAppearance,
                    backgroundColor,
                    hintText,
                    hintTextRes,
                    hintTextColor,
                    attachmentIcon,
                    attachmentIconTint,
                    sendIcon,
                    sendIconTint,
                    separatorLineColor,
                    isSeparatorLineVisible
                )
            }
        }

        /**
         * Applies [WidgetInfo] livechat widget configuration into existing [InAppChatInputViewStyle] from android style inside "IB_AppTheme.Chat" theme,
         * defined by "ibChatInputStyle" attribute provided by integrator.
         * Priority: IB_AppTheme.Chat ibChatInputStyle > [WidgetInfo] > IB.Chat.Input style
         */
        internal fun InAppChatInputViewStyle.applyWidgetConfig(
            context: Context,
            widgetInfo: WidgetInfo
        ): InAppChatInputViewStyle {
            var style = this
            val theme = context.theme

            @ColorInt
            val colorPrimary = widgetInfo.colorPrimary
            @ColorInt
            val backgroundColor = widgetInfo.colorBackground

            val buttonTint = colorPrimary?.let {
                colorStateListOf(
                    intArrayOf(-android.R.attr.state_enabled) to context.getColorCompat(R.color.ib_chat_hint_text_color),
                    intArrayOf(android.R.attr.state_enabled) to it,
                )
            }

            if (theme.isIbDefaultTheme()) { //if it is IB default theme apply widget color automatically to all components
                if (buttonTint != null) {
                    style = style.copy(attachmentIconTint = buttonTint, sendIconTint = buttonTint)
                }
                if (backgroundColor != null) {
                    style = style.copy(backgroundColor = backgroundColor)
                }
            } else { //if it is theme provided by integrator apply widget color only to components which are not defined by integrator
                val sendButtonTintDefined = theme.isAttributePresent(
                    R.styleable.InAppChatInputViewStyleable_ibChatInputSendIconTint,
                    R.attr.ibChatInputStyle,
                    R.styleable.InAppChatInputViewStyleable
                )
                if (!sendButtonTintDefined && buttonTint != null) {
                    style = style.copy(sendIconTint = buttonTint)
                }
                val attachmentButtonTintDefined = theme.isAttributePresent(
                    R.styleable.InAppChatInputViewStyleable_ibChatInputAttachmentIconTint,
                    R.attr.ibChatInputStyle,
                    R.styleable.InAppChatInputViewStyleable
                )
                if (!attachmentButtonTintDefined && buttonTint != null) {
                    style = style.copy(attachmentIconTint = buttonTint)
                }
                val backgroundColorDefined = theme.isAttributePresent(
                    R.styleable.InAppChatInputViewStyleable_ibChatInputBackgroundColor,
                    R.attr.ibChatInputStyle,
                    R.styleable.InAppChatInputViewStyleable
                )
                if (!backgroundColorDefined && backgroundColor != null) {
                    style = style.copy(backgroundColor = backgroundColor)
                }
            }

            return style
        }
    }

}