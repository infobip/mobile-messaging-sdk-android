package org.infobip.mobile.messaging.chat.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.StringRes;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.Locale;

public class LocalizationUtils {
    private static LocalizationUtils INSTANCE = null;

    private final Context appContext;
    private Resources resources;

    private LocalizationUtils(Context context) {
        appContext = context.getApplicationContext();
        resources = context.getResources();
    }

    public static LocalizationUtils getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LocalizationUtils(context.getApplicationContext());
        }

        return INSTANCE;
    }

    public Context updateContext() {
        Configuration conf = resources.getConfiguration();
        Locale locale = conf.locale;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            conf.setLayoutDirection(locale);
        }
        Context newContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newContext = appContext.createConfigurationContext(conf);
        } else {
            resources.updateConfiguration(conf, resources.getDisplayMetrics());
            newContext = appContext;
        }

        return newContext;
    }

    public String getString(@StringRes int stringRes, Object... args) {
        return resources.getString(stringRes, args);
    }

    public void setLanguage(Locale locale) {
        Locale.setDefault(locale);
        Configuration conf = appContext.getResources().getConfiguration();
        conf.locale = locale;
        resources = new Resources(appContext.getAssets(), appContext.getResources().getDisplayMetrics(), conf);
    }

    public Locale localeFromString(String language) {
        try {
            if (StringUtils.isBlank(language)) {
                MobileMessagingLogger.d("Language is empty, using device default locale.");
                return getApplicationLocale();
            }

            if (language.contains("-")) {
                return parseLocaleWithDelimiter(language, "-");
            } else if (language.contains("_")) {
                return parseLocaleWithDelimiter(language, "_");
            } else {
                return new Locale(language);
            }
        } catch (Throwable ignored) {
            MobileMessagingLogger.e("Could not parse language, using device default locale.", ignored);
            return getApplicationLocale();
        }
    }

    private Locale parseLocaleWithDelimiter(String language, String delimiter) {
        String[] parts = language.split(delimiter);
        if (parts.length >= 3) {
            return new Locale(parts[0], parts[1], parts[2]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else if (parts.length == 1) {
            return new Locale(parts[0]);
        } else {
            return new Locale(language);
        }
    }

    private Locale getApplicationLocale() {
        return ConfigurationCompat.getLocales(appContext.getResources().getConfiguration()).get(0);
    }

}
