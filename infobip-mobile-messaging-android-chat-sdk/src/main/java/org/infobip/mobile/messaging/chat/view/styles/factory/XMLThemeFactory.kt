package org.infobip.mobile.messaging.chat.view.styles.factory

import android.content.Context
import android.util.AttributeSet
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.view.styles.InAppChatInputViewStyle
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle
import org.infobip.mobile.messaging.chat.view.styles.InAppChatToolbarStyle


class XMLThemeFactory(private val context: Context, private val attributeSet: AttributeSet?, private val widgetInfo: WidgetInfo?) : StyleFactory {

    override fun chatToolbarStyle(): InAppChatToolbarStyle = InAppChatToolbarStyle.createChatToolbarStyle(context, widgetInfo)

    override fun attachmentToolbarStyle(): InAppChatToolbarStyle = InAppChatToolbarStyle.createChatAttachmentStyle(context, widgetInfo)

    override fun chatStyle(): InAppChatStyle = InAppChatStyle.invoke(context, attributeSet, widgetInfo)

    override fun chatInputViewStyle(): InAppChatInputViewStyle = InAppChatInputViewStyle(context, attributeSet, widgetInfo)
}