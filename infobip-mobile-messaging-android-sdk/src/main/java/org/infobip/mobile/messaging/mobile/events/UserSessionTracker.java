package org.infobip.mobile.messaging.mobile.events;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.util.PreferenceHelper;

public class UserSessionTracker {

    public static final String SESSION_BOUNDS_DELIMITER = "___";
    private static final int sessionTimeoutMillis = 1000 * 30;
    private static final int sessionAlarmIntervalMillis = 1000 * 5;
    private static Runnable sessionTrackingRunnable;
    // Handler object is created on the main thread by default
    private static final Handler handler = new Handler();

    public static void startSessionTracking(Context context) {
        sessionTrackingRunnable = getSessionTrackingRunnable(context);

        if (ActivityLifecycleMonitor.isForeground()) {
            handler.post(sessionTrackingRunnable);
        } else {
            handler.removeCallbacks(sessionTrackingRunnable);
        }
    }

    public static void stopSessionTracking(Context context) {
        if (sessionTrackingRunnable != null) {
            handler.removeCallbacks(sessionTrackingRunnable);
            sessionTrackingRunnable = null;
        }
        saveActiveSessionEndTime(context, Time.now());
    }

    @NonNull
    private static Runnable getSessionTrackingRunnable(final Context context) {
        if (sessionTrackingRunnable != null) return sessionTrackingRunnable;
        else return new Runnable() {
            @Override
            public void run() {
                if (ActivityLifecycleMonitor.isBackground()) {
                    saveActiveSessionEndTime(context, Time.now());
                    handler.removeCallbacks(this);
                } else {
                    trackCurrentSession(context.getApplicationContext());
                    handler.postDelayed(this, UserSessionTracker.sessionAlarmIntervalMillis);
                }
            }
        };
    }

    private static void trackCurrentSession(Context context) {
        if (ActivityLifecycleMonitor.isBackground())
            return;

        final MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        long activeSessionEndTimeMillis = mobileMessagingCore.getActiveSessionEndTime();
        long activeSessionStartTimeMillis = mobileMessagingCore.getActiveSessionStartTime();
        long now = Time.now();

        if (shouldBeNewSession(activeSessionStartTimeMillis, activeSessionEndTimeMillis, now)) {
            saveActiveSessionStartTime(context, now);

            if (sessionExistedBefore(activeSessionStartTimeMillis, activeSessionEndTimeMillis)) {
                mobileMessagingCore.saveSessionBounds(context, activeSessionStartTimeMillis, activeSessionEndTimeMillis);
                mobileMessagingCore.reportSessions();
            }
        }

        saveActiveSessionEndTime(context, now);
    }

    private static boolean sessionExistedBefore(long activeSessionStartTimeMillis, long activeSessionEndTimeMillis) {
        return activeSessionStartTimeMillis != 0 && activeSessionEndTimeMillis != 0;
    }

    // we have brand new session or we've tracked session at least once in 5 seconds but it's outdated
    private static boolean shouldBeNewSession(long activeSessionStartTimeMillis, long activeSessionEndTimeMillis, long now) {
        return activeSessionStartTimeMillis == 0 || now - activeSessionEndTimeMillis > sessionTimeoutMillis;
    }

    private static void saveActiveSessionStartTime(Context context, long sessionStartTime) {
        PreferenceHelper.saveLong(context, MobileMessagingProperty.ACTIVE_SESSION_START_TIME_MILLIS, sessionStartTime);
    }

    private static void saveActiveSessionEndTime(Context context, long sessionEndTime) {
        PreferenceHelper.saveLong(context, MobileMessagingProperty.ACTIVE_SESSION_END_TIME_MILLIS, sessionEndTime);
    }
}
