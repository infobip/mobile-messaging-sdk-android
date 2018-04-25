package org.infobip.mobile.messaging.app;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;

import java.util.Collection;

/**
 * @author sslavin
 * @since 22/06/16.
 */
public class ActivityLifecycleMonitor implements Application.ActivityLifecycleCallbacks, ForegroundStateMonitor {
    private static volatile boolean foreground = false;
    private volatile Activity foregroundActivity = null;

    public ActivityLifecycleMonitor(Context context) {
        Application application = getApplication(context);
        if (application != null) {
            application.registerActivityLifecycleCallbacks(this);
        }
    }

    public static synchronized boolean isForeground() {
        return foreground;
    }

    @Override
    @NonNull
    public ForegroundState isInForeground() {
        if (isForeground()) {
            return ForegroundState.foreground(foregroundActivity);
        }

        return ForegroundState.background();
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

    private static Application getApplication(Context context) {
        if (context instanceof Activity) {
            return ((Activity) context).getApplication();
        } else if (context instanceof Service) {
            return ((Service) context).getApplication();
        } else if (context.getApplicationContext() instanceof Application) {
            return (Application) context.getApplicationContext();
        }
        return null;
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
        setForeground(activity, false);
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
