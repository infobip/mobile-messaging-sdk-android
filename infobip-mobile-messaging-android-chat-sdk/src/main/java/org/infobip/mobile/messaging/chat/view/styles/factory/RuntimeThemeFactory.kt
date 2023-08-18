package org.infobip.mobile.messaging.chat.view.styles.factory

import org.infobip.mobile.messaging.chat.view.styles.InAppChatInputViewStyle
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle
import org.infobip.mobile.messaging.chat.view.styles.InAppChatTheme
import org.infobip.mobile.messaging.chat.view.styles.InAppChatToolbarStyle

class RuntimeThemeFactory(private val inAppChatTheme: InAppChatTheme) : StyleFactory {
    override fun chatToolbarStyle(): InAppChatToolbarStyle = inAppChatTheme.chatToolbarStyle

    override fun attachmentToolbarStyle(): InAppChatToolbarStyle = inAppChatTheme.attachmentToolbarStyle

    override fun chatStyle(): InAppChatStyle = inAppChatTheme.chatStyle

    override fun chatInputViewStyle(): InAppChatInputViewStyle = inAppChatTheme.chatInputViewStyle
}