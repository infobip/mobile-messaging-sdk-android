package org.infobip.mobile.messaging.chat.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.applyInAppChatLanguage
import org.infobip.mobile.messaging.chat.view.InAppChatThemeResolver.getChatViewTheme

class InAppChatActivity: AppCompatActivity(), InAppChatFragment.InAppChatActionBarProvider {

    companion object {

        @JvmStatic
        internal fun startIntent(context: Context): Intent {
            return Intent(context, InAppChatActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.applyInAppChatLanguage())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getChatViewTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ib_activity_chat)
    }

    override val originalSupportActionBar: ActionBar?
        get() = supportActionBar

    override fun onInAppChatBackPressed() {
        onBackPressedDispatcher.onBackPressed()
    }

}