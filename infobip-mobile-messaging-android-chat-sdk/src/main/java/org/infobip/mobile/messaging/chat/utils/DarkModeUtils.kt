package org.infobip.mobile.messaging.chat.utils

import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import org.infobip.mobile.messaging.chat.view.styles.InAppChatDarkMode

object DarkModeUtils {

    @JvmStatic
    fun setActivityDarkMode(activity: AppCompatActivity, modeToSet: InAppChatDarkMode?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val currentNightMode: Int =
                activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (modeToSet === InAppChatDarkMode.DARK_MODE_YES && currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
                activity.delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
            } else if (modeToSet === InAppChatDarkMode.DARK_MODE_NO && currentNightMode != Configuration.UI_MODE_NIGHT_NO) {
                activity.delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
            } else if (modeToSet === InAppChatDarkMode.DARK_MODE_FOLLOW_SYSTEM) {
                activity.delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            } else if (modeToSet === null) {
                activity.delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
            }
        }
    }

}