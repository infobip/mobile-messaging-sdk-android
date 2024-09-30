package org.infobip.mobile.messaging.chat.view.styles

data class InAppChatTheme @JvmOverloads constructor(
        val chatToolbarStyle: InAppChatToolbarStyle = InAppChatToolbarStyle(),
        val attachmentToolbarStyle: InAppChatToolbarStyle = InAppChatToolbarStyle(),
        val chatStyle: InAppChatStyle = InAppChatStyle(),
        val chatInputViewStyle: InAppChatInputViewStyle = InAppChatInputViewStyle(),
)