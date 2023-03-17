package org.infobip.mobile.messaging.chat.core

import android.content.Context
import android.content.Intent
import org.infobip.mobile.messaging.chat.InAppChatScreen
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.chat.view.InAppChatActivity

class InAppChatScreenImpl(val context: Context): InAppChatScreen {

    private val propertyHelper = PropertyHelper(context)

    override fun show() {
        propertyHelper.saveClasses(MobileMessagingChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES, *getTapActivityClasses())
        context.startActivity(Intent(context, InAppChatActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // region private methods
    private fun getTapActivityClasses(): Array<Class<*>> {
        return getLaunchActivityClass()?.let {
            arrayOf(it, InAppChatActivity::class.java)
        } ?: arrayOf(InAppChatActivity::class.java)
    }

    private fun getLaunchActivityClass(): Class<*>? {
        val className = context.packageManager.getLaunchIntentForPackage(context.packageName)?.component?.className?.takeIf { it.isNotBlank() }
        return runCatching {
            className?.let { Class.forName(it) }
        }.getOrNull()
    }

}