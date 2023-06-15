package org.infobip.mobile.messaging.chat.core;

import androidx.annotation.Nullable;

enum Language {
    ENGLISH("en-US"),
    TURKISH("tr-TR"),
    KOREAN("ko-KR"),
    RUSSIAN("ru-RU"),
    CHINESE("zh-TW"),
    SPANISH("es-ES"),
    SPANISH_LA("es-LA"),
    PORTUGUESE("pt-PT"),
    PORTUGUESE_BR("pt-BR"),
    POLISH("pl-PL"),
    ROMANIAN("ro-RO"),
    ARABIC("ar-AE"),
    BOSNIAN("bs-BA"),
    CROATIAN("hr-HR"),
    GREEK("el-GR"),
    SWEDISH("sv-SE"),
    THAI("th-TH"),
    LITHUANIAN("lt-LT"),
    DANISH("da-DK"),
    LATVIAN("lv-LV"),
    HUNGARIAN("hu-HU"),
    ITALIAN("it-IT"),
    FRENCH("fr-FR"),
    SLOVENIAN("sl-SI"),
    UKRAINIAN("uk-UA"),
    JAPANESE("ja-JP"),
    GERMAN("de-DE"),
    ALBANIAN("sq-AL");

    private final String locale;

    Language(String locale) {
        this.locale = locale;
    }

    public String getLocale() {
        return locale;
    }

    @Nullable
    public static Language findLanguage(String locale) {
        if (locale.length() >= 2) {
            String lang = locale.substring(0, 2);
            for (Language l : values()) {
                if (l.getLocale().startsWith(lang)) {
                    return l;
                }
            }
        }
        return null;
    }
}
