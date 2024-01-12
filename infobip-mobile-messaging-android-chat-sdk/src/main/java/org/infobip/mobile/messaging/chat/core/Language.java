package org.infobip.mobile.messaging.chat.core;

import androidx.annotation.Nullable;

import java.util.Locale;

enum Language {
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

    private final String locale;

    Language(String locale) {
        this.locale = locale;
    }

    public String getLocale() {
        return locale;
    }

    @Nullable
    public static Language findLanguage(Locale locale) {
        String language = locale.getLanguage();
        if ("zh".equalsIgnoreCase(language)) {
            String country = locale.getCountry();
            if ("Hant".equalsIgnoreCase(country)
                    || "TW".equalsIgnoreCase(country)
                    || "HK".equalsIgnoreCase(country)) {
                return CHINESE_TRADITIONAL;
            }
            if ("Hans".equalsIgnoreCase(country)
                    || "CN".equalsIgnoreCase(country)
                    || "SG".equalsIgnoreCase(country)) {
                return CHINESE_SIMPLIFIED;
            }
        } else if (language.length() >= 2) {
            String lang = language.substring(0, 2);
            for (Language l : values()) {
                if (l.getLocale().startsWith(lang)) {
                    return l;
                }
            }
        }
        return null;
    }
}
