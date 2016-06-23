package org.infobip.mobile.messaging.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * @author sslavin
 * @since 22/06/16.
 */
public class ActivityLifecycleMonitor implements Application.ActivityLifecycleCallbacks {
    private static boolean foreground = false;
    private final Application application;

    public ActivityLifecycleMonitor(Application application) {
        this.application = application;
        this.application.registerActivityLifecycleCallbacks(this);
    }

    public static synchronized boolean isForeground() {
        return foreground;
    }

    private static synchronized void setForeground(boolean foreground) {
        ActivityLifecycleMonitor.foreground = foreground;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        setForeground(true);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        setForeground(false);
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
