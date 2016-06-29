package org.infobip.mobile.messaging.app;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

/**
 * @author sslavin
 * @since 22/06/16.
 */
public class ActivityLifecycleMonitor implements Application.ActivityLifecycleCallbacks {
    private static boolean foreground = false;

    public ActivityLifecycleMonitor(Context context) {
        Application application = null;
        if (context instanceof Activity) {
            application = ((Activity) context).getApplication();
        } else if (context.getApplicationContext() instanceof Application) {
            application = (Application) context.getApplicationContext();
        }

        if (application != null) {
            application.registerActivityLifecycleCallbacks(this);
        }
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
