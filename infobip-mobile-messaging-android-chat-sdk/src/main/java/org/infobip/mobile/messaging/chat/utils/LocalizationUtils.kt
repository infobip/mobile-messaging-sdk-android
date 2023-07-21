package org.infobip.mobile.messaging.chat.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.os.ConfigurationCompat
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.util.StringUtils
import java.util.Locale

class LocalizationUtils private constructor(context: Context) {

    private val appContext: Context
    private var resources: Resources

    init {
        appContext = context.applicationContext
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

    fun localeFromString(language: String): Locale {
        return try {
            if (StringUtils.isBlank(language)) {
                MobileMessagingLogger.d("Language is empty, using device default locale.")
                return applicationLocale
            }
            if (language.contains("-")) {
                parseLocaleWithDelimiter(language, "-")
            } else if (language.contains("_")) {
                parseLocaleWithDelimiter(language, "_")
            } else {
                Locale(language)
            }
        } catch (ignored: Throwable) {
            MobileMessagingLogger.e(
                "Could not parse language, using device default locale.",
                ignored
            )
            applicationLocale
        }
    }

    private fun parseLocaleWithDelimiter(language: String, delimiter: String): Locale {
        val parts = language.split(delimiter)
        return if (parts.size >= 3) {
            Locale(parts[0], parts[1], parts[2])
        } else if (parts.size == 2) {
            Locale(parts[0], parts[1])
        } else if (parts.size == 1) {
            Locale(parts[0])
        } else {
            Locale(language)
        }
    }

    private val applicationLocale: Locale
        get() = ConfigurationCompat.getLocales(appContext.resources.configuration)[0]
            ?: Locale.getDefault()

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            newConfig.setLayoutDirection(locale)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            newConfig.setLocale(locale)
            this.createConfigurationContext(newConfig)
        } else {
            newConfig.locale = locale
            resources.updateConfiguration(newConfig, resources.displayMetrics)
            this
        }
    } else {
        this
    }
}

internal fun Context.applyInAppChatLanguage(): Context {
    val language = PropertyHelper.getDefaultMMSharedPreferences(this)
        .getString(MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE.key, null)
    return if (language != null) {
        val locale = LocalizationUtils.getInstance(this).localeFromString(language)
        this.applyLocale(locale)
    } else {
        this
    }
}