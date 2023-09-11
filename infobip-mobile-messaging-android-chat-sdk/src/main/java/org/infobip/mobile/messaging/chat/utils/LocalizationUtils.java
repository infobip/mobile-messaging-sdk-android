package org.infobip.mobile.messaging.chat.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.StringRes;
import androidx.core.os.ConfigurationCompat;

import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
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

    public String getString(@StringRes int stringRes, Object... args) {
        return resources.getString(stringRes, args);
    }

    public void setLanguage(Locale locale) {
        resources = applyLocale(appContext, locale).getResources();
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
        } catch (Throwable throwable) {
            MobileMessagingLogger.e("Could not parse language, using device default locale.", throwable);
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
        Locale locale = ConfigurationCompat.getLocales(appContext.getResources().getConfiguration()).get(0);
        return locale != null ? locale : Locale.getDefault();
    }


    private static Context applyLocale(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration currentConfig = resources.getConfiguration();
        Locale currentLocale = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? currentConfig.getLocales().get(0) : currentConfig.locale;
        if (!currentLocale.getLanguage().equals(locale.getLanguage())) {
            Locale.setDefault(locale);
            Configuration newConfig = new Configuration(currentConfig);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                newConfig.setLayoutDirection(locale);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                newConfig.setLocale(locale);
                return context.createConfigurationContext(newConfig);
            } else {
                newConfig.locale = locale;
                resources.updateConfiguration(newConfig, resources.getDisplayMetrics());
                return context;
            }
        } else {
            return context;
        }
    }

    public static Context applyInAppChatLanguage(Context context) {
        String language = PropertyHelper.getDefaultMMSharedPreferences(context).getString(MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE.getKey(), null);
        if (language != null) {
            Locale locale = LocalizationUtils.getInstance(context).localeFromString(language);
            return applyLocale(context, locale);
        } else {
            return context;
        }
    }
}
