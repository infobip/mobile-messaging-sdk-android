package org.infobip.mobile.messaging.chat.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.annotation.StringRes
import org.infobip.mobile.messaging.chat.InAppChat
import java.util.Locale

class LocalizationUtils private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private var resources: Resources

    init {
        resources = context.resources
    }

    companion object {

        private var INSTANCE: LocalizationUtils? = null

        @JvmStatic
        fun getInstance(context: Context): LocalizationUtils {
            return INSTANCE ?: synchronized(this) {
                LocalizationUtils(context).also { INSTANCE = it }
            }
        }
    }

    fun getString(@StringRes stringRes: Int, vararg args: Any?): String {
        return resources.getString(stringRes, *args)
    }

    fun setLanguage(locale: Locale) {
        resources = appContext.applyLocale(locale).resources
    }

}

@Suppress("DEPRECATION")
internal fun Context.applyLocale(locale: Locale): Context {
    val resources = this.resources
    val currentConfig = resources.configuration
    val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        currentConfig.locales.get(0)
    } else {
        currentConfig.locale
    }
    return if (currentLocale.language != locale.language) {
        Locale.setDefault(locale)
        val newConfig = Configuration(currentConfig)
        newConfig.setLayoutDirection(locale)
        newConfig.setLocale(locale)
        return this.createConfigurationContext(newConfig)
    } else {
        this
    }
}

internal fun Context.applyInAppChatLanguage(): Context {
    return this.applyLocale(InAppChat.getInstance(this).language.locale)
}