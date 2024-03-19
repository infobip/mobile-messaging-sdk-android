package org.infobip.mobile.messaging.chat.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.utils.DarkModeUtils
import org.infobip.mobile.messaging.chat.utils.applyInAppChatLanguage
import org.infobip.mobile.messaging.chat.view.InAppChatThemeResolver.getChatViewTheme
import org.infobip.mobile.messaging.chat.view.styles.InAppChatDarkMode
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

class InAppChatActivity: AppCompatActivity(), InAppChatFragment.InAppChatActionBarProvider {

    companion object {
        private const val EXTRA_DARK_MODE = "org.infobip.mobile.messaging.chat.view.InAppChatActivity.EXTRA_DARK_MODE"

        @JvmStatic
        internal fun startIntent(context: Context, darkMode: InAppChatDarkMode? = null): Intent {
            return Intent(context, InAppChatActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                darkMode?.let {
                    putExtra(EXTRA_DARK_MODE, it.name)
                }
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
        applyDarkMode()
    }

    private fun applyDarkMode() {
        intent.extras?.getString(EXTRA_DARK_MODE)
            ?.let { runCatching { InAppChatDarkMode.valueOf(it) }.getOrNull() }
            ?.let {
                MobileMessagingLogger.d("Applied dark mode: $it")
                DarkModeUtils.setActivityDarkMode(this, it)
            }
    }

    override val originalSupportActionBar: ActionBar?
        get() = supportActionBar

    override fun onInAppChatBackPressed() {
        onBackPressedDispatcher.onBackPressed()
    }

}