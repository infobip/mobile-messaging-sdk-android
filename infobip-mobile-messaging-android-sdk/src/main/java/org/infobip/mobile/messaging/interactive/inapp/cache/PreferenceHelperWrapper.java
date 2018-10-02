package org.infobip.mobile.messaging.interactive.inapp.cache;

import android.content.Context;
import android.support.annotation.Nullable;

import org.infobip.mobile.messaging.util.PreferenceHelper;

/**
 * Temporary wrapper for preference helper
 *
 * @author sslavin
 * @since 18/04/2018.
 */
public class PreferenceHelperWrapper {

    private final Context context;

    PreferenceHelperWrapper(Context context) {
        this.context = context;
    }

    @Nullable
    public String getAndRemove(String key) {
        String value = PreferenceHelper.findString(context, key, null);
        PreferenceHelper.remove(context, key);
        return value;
    }

    public boolean get(String key, boolean defaultValue) {
        return PreferenceHelper.findBoolean(context, key, defaultValue);
    }

    public void set(String key, String value) {
        PreferenceHelper.saveString(context, key, value);
    }

    public void set(String key, boolean value) {
        PreferenceHelper.saveBoolean(context, key, value);
    }

    public void remove(String key) {
        PreferenceHelper.remove(context, key);
    }
}
