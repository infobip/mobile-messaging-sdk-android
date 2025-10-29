/*
 * MobileMessagingStats.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.stats;

import android.content.Context;

import org.infobip.mobile.messaging.util.PreferenceHelper;

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
        return PreferenceHelper
                .getDefaultMMSharedPreferences(context)
                .getLong(getKey(mobileMessagingStatsError), 0);
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
        PreferenceHelper
                .getDefaultMMSharedPreferences(context).edit()
                .putLong(getKey(mobileMessagingStatsError), value).apply();
    }
}
