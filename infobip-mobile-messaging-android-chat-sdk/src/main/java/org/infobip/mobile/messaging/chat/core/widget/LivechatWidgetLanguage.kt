/*
 * LivechatWidgetLanguage.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core.widget

import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.util.StringUtils
import java.util.Locale

/**
 * Supported livechat widget languages.
 */
enum class LivechatWidgetLanguage(val widgetCode: String) {
    ALBANIAN("sq-AL"),
    ARABIC("ar-AE"),
    BOSNIAN("bs-BA"),
    CHINESE_TRADITIONAL("zh-Hant"),
    CHINESE_SIMPLIFIED("zh-Hans"),
    CROATIAN("hr-HR"),
    DANISH("da-DK"),
    ENGLISH("en-US"),
    FRENCH("fr-FR"),
    GERMAN("de-DE"),
    GREEK("el-GR"),
    HUNGARIAN("hu-HU"),
    ITALIAN("it-IT"),
    JAPANESE("ja-JP"),
    KOREAN("ko-KR"),
    LATVIAN("lv-LV"),
    LITHUANIAN("lt-LT"),
    POLISH("pl-PL"),
    PORTUGUESE("pt-PT"),
    PORTUGUESE_BR("pt-BR"),
    ROMANIAN("ro-RO"),
    RUSSIAN("ru-RU"),
    SERBIAN("sr-Latn"),
    SLOVENIAN("sl-SI"),
    SPANISH("es-ES"),
    SPANISH_LA("es-LA"),
    SWEDISH("sv-SE"),
    THAI("th-TH"),
    TURKISH("tr-TR"),
    UKRAINIAN("uk-UA");

    val locale: Locale
        get() = localeFromString(widgetCode)

    companion object {

        /**
         * Finds livechat widget language by widget locale.
         */
        @JvmStatic
        fun findLanguage(widgetLocale: String?): LivechatWidgetLanguage? {
            if (widgetLocale.isNullOrBlank())
                return null

            for (l in entries) {
                if (l.widgetCode == widgetLocale.replace("_", "-")) {
                    return l
                }
            }
            return null
        }

        /**
         * Finds livechat widget language by widget locale or returns default language.
         */
        @JvmStatic
        @JvmOverloads
        fun findLanguageOrDefault(widgetLocale: String?, default: LivechatWidgetLanguage = ENGLISH): LivechatWidgetLanguage {
            return findLanguage(widgetLocale) ?: default
        }

        /**
         * Finds livechat widget language by locale.
         */
        @JvmStatic
        fun findLanguage(locale: Locale): LivechatWidgetLanguage? {
            val findByLocaleLanguage: (String) -> Set<LivechatWidgetLanguage> = { localeLanguage ->
                entries.filter { it.widgetCode.startsWith(localeLanguage, ignoreCase = true) }.toSet()
            }

            val findByLocaleCountry: (Set<LivechatWidgetLanguage>, String) -> LivechatWidgetLanguage? = { entries, country ->
                val localeCountry = when (country.lowercase()) {
                    "tw", "hk", "hant" -> "Hant"
                    "cn", "sg", "hans" -> "Hans"
                    else -> country
                }
                entries.firstOrNull { lcLang ->
                    val lcLangCountry = lcLang.widgetCode.split("-").getOrNull(1)
                    localeCountry.equals(lcLangCountry, ignoreCase = true)
                }
            }

            val langMatch: Set<LivechatWidgetLanguage> = findByLocaleLanguage(locale.language)
            val langAndCountryMatch = findByLocaleCountry(langMatch, locale.country)

            return langAndCountryMatch ?: langMatch.firstOrNull()
        }

        /**
         * Finds livechat widget language by locale or returns default language.
         *
         * @param locale locale contains country and language
         * @param default default language
         */
        @JvmStatic
        @JvmOverloads
        fun findLanguageOrDefault(locale: Locale, default: LivechatWidgetLanguage = ENGLISH): LivechatWidgetLanguage {
            return findLanguage(locale) ?: default
        }

        /**
         * Parses locale from string.
         */
        internal fun localeFromString(language: String, default: Locale = Locale.ENGLISH): Locale {
            return try {
                if (StringUtils.isBlank(language)) {
                    return default
                }
                if (language.contains("-")) {
                    parseLocaleWithDelimiter(language, "-")
                } else if (language.contains("_")) {
                    parseLocaleWithDelimiter(language, "_")
                } else {
                    Locale(language)
                }
            } catch (ignored: Throwable) {
                MobileMessagingLogger.e("Could not parse language, using device default ENGLISH locale.", ignored)
                return default
            }
        }

        private fun parseLocaleWithDelimiter(language: String, delimiter: String): Locale {
            val parts = language.split(delimiter)
            return when {
                parts.size >= 3 -> Locale(parts[0], parts[1], parts[2])
                parts.size == 2 -> Locale(parts[0], parts[1])
                parts.size == 1 -> Locale(parts[0])
                else -> Locale(language)
            }
        }

    }

}