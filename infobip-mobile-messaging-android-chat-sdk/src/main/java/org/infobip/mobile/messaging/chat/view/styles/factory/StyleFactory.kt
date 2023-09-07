package org.infobip.mobile.messaging.chat.view.styles.factory

import android.content.Context
import android.util.AttributeSet
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.InAppChatImpl
import org.infobip.mobile.messaging.chat.view.styles.InAppChatInputViewStyle
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle
import org.infobip.mobile.messaging.chat.view.styles.InAppChatToolbarStyle

/**
 * Custom styles are stored here, there is hierarchy of style sources
 *
 * 1. Runtime theme attributes
 * 2. 8.0+ android theme attributes
 * 3. Pre 8.0 android theme attributes
 * 4. LiveChat widget theme
 * 5. In-app chat default theme
 *
 */
interface StyleFactory {

    companion object {
        fun create(context: Context, attributeSet: AttributeSet? = null, widgetInfo: WidgetInfo? = null): StyleFactory {
            return InAppChatImpl.getInstance(context).theme?.let {
                RuntimeThemeFactory(it)
            } ?: XMLThemeFactory(context, attributeSet, widgetInfo)
        }
    }

    fun chatToolbarStyle(): InAppChatToolbarStyle
    fun attachmentToolbarStyle(): InAppChatToolbarStyle
    fun chatStyle(): InAppChatStyle
    fun chatInputViewStyle(): InAppChatInputViewStyle
}

