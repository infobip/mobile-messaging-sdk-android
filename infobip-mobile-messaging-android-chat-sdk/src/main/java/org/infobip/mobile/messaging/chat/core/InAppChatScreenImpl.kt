package org.infobip.mobile.messaging.chat.core

import android.content.Context
import org.infobip.mobile.messaging.chat.InAppChatScreen
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.chat.view.InAppChatActivity
import org.infobip.mobile.messaging.chat.view.styles.InAppChatDarkMode

class InAppChatScreenImpl(val context: Context): InAppChatScreen {

    private val propertyHelper = PropertyHelper(context)
    var darkMode: InAppChatDarkMode? = null

    override fun show() {
        propertyHelper.saveClasses(MobileMessagingChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES, *getTapActivityClasses())
        context.startActivity(InAppChatActivity.startIntent(context, darkMode))
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