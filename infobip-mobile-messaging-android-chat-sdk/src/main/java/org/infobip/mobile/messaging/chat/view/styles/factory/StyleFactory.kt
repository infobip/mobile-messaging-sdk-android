package org.infobip.mobile.messaging.chat.view.styles.factory

import android.content.Context
import android.util.AttributeSet
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.InAppChat
import org.infobip.mobile.messaging.chat.view.styles.InAppChatInputViewStyle
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle
import org.infobip.mobile.messaging.chat.view.styles.InAppChatToolbarStyle

/**
 * Custom styles are stored here, there is hierarchy of style sources
 *
 * 1. Runtime theme attributes - [InAppChatTheme] by [RuntimeThemeFactory]
 * 2. Android theme attributes - [InAppChatTheme] by [XMLThemeFactory]
 * 3. LiveChat widget theme - [WidgetInfo]
 * 4. In-app chat default theme - [IB_ChatDefaultTheme.Styled]
 */
interface StyleFactory {

    companion object {
        fun create(context: Context, attributeSet: AttributeSet? = null, widgetInfo: WidgetInfo? = null): StyleFactory {
            return InAppChat.getInstance(context).theme?.let {
                RuntimeThemeFactory(it)
            } ?: XMLThemeFactory(context, attributeSet, widgetInfo)
        }
    }

    fun chatToolbarStyle(): InAppChatToolbarStyle
    fun attachmentToolbarStyle(): InAppChatToolbarStyle
    fun chatStyle(): InAppChatStyle
    fun chatInputViewStyle(): InAppChatInputViewStyle
}

