/*
 * ForegroundStateMonitorImpl.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.foreground;

import android.app.Activity;
import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

/**
 * @author sslavin
 * @since 07/05/2018.
 */
public class ForegroundStateMonitorImpl implements ForegroundStateMonitor {

    private final Context context;

    public ForegroundStateMonitorImpl(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ForegroundState isInForeground() {
        return isInForeground(MobileMessagingCore.getInstance(context));
    }

    @VisibleForTesting
    ForegroundState isInForeground(MobileMessagingCore mobileMessagingCore) {
        ActivityLifecycleMonitor activityLifecycleMonitor = mobileMessagingCore.getActivityLifecycleMonitor();
        if (activityLifecycleMonitor == null) {
            return ForegroundState.background();
        }

        Activity activity = activityLifecycleMonitor.getForegroundActivity();
        if (activity == null) {
            return ForegroundState.background();
        }

        return ForegroundState.foreground(activity);
    }
}
