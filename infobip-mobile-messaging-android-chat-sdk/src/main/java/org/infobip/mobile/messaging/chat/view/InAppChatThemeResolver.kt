package org.infobip.mobile.messaging.chat.view

import android.content.Context
import androidx.annotation.StyleRes
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.util.ResourceLoader

object InAppChatThemeResolver {

    private const val RES_ID_CHAT_VIEW_THEME = "IB_AppTheme.Chat"

    private var chatViewTheme = 0

    @JvmStatic
    @StyleRes
    fun getChatViewTheme(context: Context): Int {
        if (chatViewTheme != 0) {
            return chatViewTheme
        }
        chatViewTheme = getThemeResourceByName(context, RES_ID_CHAT_VIEW_THEME, R.style.IB_ChatDefaultTheme_Styled)
        return chatViewTheme
    }

    private fun getThemeResourceByName(context: Context, name: String, fallbackResourceId: Int): Int {
        var resourceId = ResourceLoader.loadResourceByName(context, "style", name)
        if (resourceId == 0) {
            resourceId = fallbackResourceId
        }
        return resourceId
    }
}