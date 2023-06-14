package org.infobip.mobile.messaging.chat.view

import android.content.Context
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.isAttributePresent
import org.infobip.mobile.messaging.util.ResourceLoader

object InAppChatThemeResolver {

    private const val RES_ID_CHAT_VIEW_THEME = "IB_AppTheme.Chat"
    private const val RES_ID_CHAT_ATTACH_THEME = "IB_AppTheme.ChatAttach"

    private var chatViewTheme = 0
    private var chatAttachPreviewTheme = 0

    @JvmStatic
    @StyleRes
    fun getChatViewTheme(context: Context): Int {
        if (chatViewTheme != 0) {
            return chatViewTheme
        }
        chatViewTheme = getThemeResourceByName(context, RES_ID_CHAT_VIEW_THEME, R.style.IB_ChatDefaultTheme_Styled)
        return chatViewTheme
    }

    @JvmStatic
    @StyleRes
    fun getChatAttachPreviewTheme(context: Context): Int {
        if (chatAttachPreviewTheme != 0) {
            return chatAttachPreviewTheme
        }

        val chatViewTheme = getChatViewTheme(context)
        val isNewAttachmentToolbarStyleDefined = if (chatViewTheme != R.style.IB_ChatDefaultTheme_Styled)
            ContextThemeWrapper(context, chatViewTheme).theme.isAttributePresent(R.attr.ibChatAttachmentToolbarStyle)
        else
            false

        chatAttachPreviewTheme = if (isNewAttachmentToolbarStyleDefined)
            chatViewTheme
        else
            getThemeResourceByName(context, RES_ID_CHAT_ATTACH_THEME, R.style.IB_ChatDefaultTheme_Styled)
        return chatAttachPreviewTheme
    }

    private fun getThemeResourceByName(context: Context, name: String, fallbackResourceId: Int): Int {
        var resourceId = ResourceLoader.loadResourceByName(context, "style", name)
        if (resourceId == 0) {
            resourceId = fallbackResourceId
        }
        return resourceId
    }
}