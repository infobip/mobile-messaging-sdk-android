package org.infobip.mobile.messaging.chat.view.styles

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.ColorInt
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.colorBackground
import org.infobip.mobile.messaging.chat.utils.isAttributePresent
import org.infobip.mobile.messaging.chat.utils.isIbDefaultTheme

data class InAppChatStyle(
    @ColorInt val backgroundColor: Int,
    val isIbDefaultTheme: Boolean
) {
    companion object {

        internal operator fun invoke(context: Context): InAppChatStyle {
            val theme = context.theme

            var backgroundColor = Color.WHITE

            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.ibChatStyle, typedValue, true)
            if (typedValue.data != 0) {
                val typedArray: TypedArray = theme.obtainStyledAttributes(typedValue.data, R.styleable.InAppChatViewStyleable)
                backgroundColor = typedArray.getColor(R.styleable.InAppChatViewStyleable_ibChatBackgroundColor, Color.WHITE)
                typedArray.recycle()
            }

            return InAppChatStyle(
                backgroundColor = backgroundColor,
                isIbDefaultTheme = context.theme.isIbDefaultTheme()
            )
        }

        private fun prepareStyle(context: Context, widgetInfo: WidgetInfo?): InAppChatStyle {
            //theme config
            var style = invoke(context)

            @ColorInt
            val backgroundColor = widgetInfo?.colorBackground

            if (style.isIbDefaultTheme) { //if it is IB default theme apply widget color automatically to all components
                if (backgroundColor != null) {
                    style = style.copy(backgroundColor = backgroundColor)
                }
            } else { //if it is theme provided by integrator apply widget color only to components which are not defined by integrator
                val backgroundColorDefined = context.theme.isAttributePresent(
                    R.styleable.InAppChatViewStyleable_ibChatBackgroundColor,
                    R.attr.ibChatStyle,
                    R.styleable.InAppChatViewStyleable
                )
                if (!backgroundColorDefined && backgroundColor != null) {
                    style = style.copy(backgroundColor = backgroundColor)
                }
            }

            return style
        }

        /**
         * Creates [InAppChatStyle] from android theme "IB_AppTheme.Chat" defined by integrator and [WidgetInfo] livechat widget configuration.
         * If "IB_AppTheme.Chat" does not exists in application, then online fetched [WidgetInfo] is used.
         * If [WidgetInfo] could not be fetched, then default IB theme "IB_ChatDefaultTheme.Styled" as last option.
         * Priority: IB_AppTheme.Chat > [WidgetInfo] > IB_ChatDefaultTheme
         */
        @JvmStatic
        fun create(context: Context, widgetInfo: WidgetInfo?): InAppChatStyle = prepareStyle(context, widgetInfo)

    }
}