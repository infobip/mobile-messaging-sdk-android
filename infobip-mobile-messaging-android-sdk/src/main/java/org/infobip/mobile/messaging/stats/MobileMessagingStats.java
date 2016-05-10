package org.infobip.mobile.messaging.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * @author mstipanov
 * @since 01.04.2016.
 */
public class MobileMessagingStats {
    public static final String STATS_KEY_BASE = "org.infobip.mobile.messaging.stats.";
    private final Context context;

    public MobileMessagingStats(Context context) {
        this.context = context;
    }

    public static String getKey(MobileMessagingError mobileMessagingError) {
        return STATS_KEY_BASE + mobileMessagingError.name();
    }

    public long getErrorCount(MobileMessagingError mobileMessagingError) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(getKey(mobileMessagingError), 0);
    }

    public synchronized void reportError(MobileMessagingError mobileMessagingError) {
        saveLong(mobileMessagingError, getErrorCount(mobileMessagingError) + 1);
    }

    public void resetErrors() {
        for (MobileMessagingError e : MobileMessagingError.values()) {
            resetError(e);
        }
    }

    public synchronized void resetError(MobileMessagingError mobileMessagingError) {
        saveLong(mobileMessagingError, getErrorCount(mobileMessagingError));
    }

    private void saveLong(MobileMessagingError mobileMessagingError, long value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putLong(getKey(mobileMessagingError), value).apply();
    }
}
