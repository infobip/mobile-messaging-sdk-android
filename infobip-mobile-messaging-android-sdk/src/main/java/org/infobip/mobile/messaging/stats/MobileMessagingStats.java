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

    public static String getKey(MobileMessagingStatsError mobileMessagingStatsError) {
        return STATS_KEY_BASE + mobileMessagingStatsError.name();
    }

    public long getErrorCount(MobileMessagingStatsError mobileMessagingStatsError) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(getKey(mobileMessagingStatsError), 0);
    }

    public synchronized void reportError(MobileMessagingStatsError mobileMessagingStatsError) {
        saveLong(mobileMessagingStatsError, getErrorCount(mobileMessagingStatsError) + 1);
    }

    public void resetErrors() {
        for (MobileMessagingStatsError e : MobileMessagingStatsError.values()) {
            resetError(e);
        }
    }

    public synchronized void resetError(MobileMessagingStatsError mobileMessagingStatsError) {
        saveLong(mobileMessagingStatsError, getErrorCount(mobileMessagingStatsError));
    }

    private void saveLong(MobileMessagingStatsError mobileMessagingStatsError, long value) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putLong(getKey(mobileMessagingStatsError), value).apply();
    }
}
