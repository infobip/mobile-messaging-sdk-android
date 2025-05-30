package org.infobip.mobile.messaging.chat.core

import android.content.Context
import org.infobip.mobile.messaging.chat.InAppChatScreen
import org.infobip.mobile.messaging.chat.view.InAppChatActivity

internal class InAppChatScreenImpl(val context: Context) : InAppChatScreen {

    override fun show() {
        context.startActivity(InAppChatActivity.startIntent(context))
    }

}