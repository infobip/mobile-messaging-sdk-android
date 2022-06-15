package org.infobip.mobile.messaging.chat.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.StringRes;

import java.util.Locale;

public class LocalizationUtils {
    private static LocalizationUtils INSTANCE = null;

    private final Context appContext;
    private Resources resources;

    private LocalizationUtils(Context context) {
        this.appContext = context.getApplicationContext();
        resources = context.getResources();
    }

    public static LocalizationUtils getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LocalizationUtils(context.getApplicationContext());
        }

        return INSTANCE;
    }

    public static Locale localeFromString(String locale) {
        try {
            String language;
            if (locale.startsWith("zh") || !locale.contains("-")) {
                language = locale;
            } else {
                language = locale.substring(0, locale.indexOf('-'));
            }

            return new Locale(language);
        } catch (Throwable ignored) {
            return Locale.getDefault();
        }
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

}
