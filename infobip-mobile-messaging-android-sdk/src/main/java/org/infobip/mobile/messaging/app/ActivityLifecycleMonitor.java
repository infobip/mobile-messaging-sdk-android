package org.infobip.mobile.messaging.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;

import java.util.Collection;

/**
 * @author sslavin
 * @since 22/06/16.
 */
public class ActivityLifecycleMonitor implements Application.ActivityLifecycleCallbacks {
    private static volatile boolean foreground = false;
    private volatile Activity foregroundActivity = null;

    public ActivityLifecycleMonitor(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }

    public static synchronized boolean isForeground() {
        return foreground;
    }

    @Nullable
    public Activity getForegroundActivity() {
        return foregroundActivity;
    }

    private static synchronized void setForeground(Context context, boolean foreground) {
        boolean foregroundBefore = ActivityLifecycleMonitor.foreground;
        ActivityLifecycleMonitor.foreground = foreground;
        if (!foregroundBefore && foreground) {
            dispatchAppInForegroundEvent(context);
        }
    }

    private static void dispatchAppInForegroundEvent(Context context) {
        Collection<MessageHandlerModule> messageHandlerModules = MobileMessagingCore.getInstance(context).getMessageHandlerModules();
        if (messageHandlerModules == null) {
            return;
        }

        for (MessageHandlerModule module : messageHandlerModules) {
            if (module != null && MobileMessagingCore.getApplicationCode(context) != null) {
                module.applicationInForeground();
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        foregroundActivity = activity;
        setForeground(activity, true);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        foregroundActivity = null;
        setForeground(null, false);
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
