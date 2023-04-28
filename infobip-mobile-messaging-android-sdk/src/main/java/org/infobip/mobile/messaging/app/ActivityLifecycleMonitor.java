package org.infobip.mobile.messaging.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.LocalEvent;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.events.UserSessionTracker;

import java.util.Collection;

/**
 * @author sslavin
 * @since 22/06/16.
 */
public class ActivityLifecycleMonitor implements Application.ActivityLifecycleCallbacks {
    private static volatile boolean foreground = false;
    @SuppressLint("StaticFieldLeak")
    private static volatile Activity foregroundActivity = null;
    public ActivityLifecycleListener activityListener;

    public ActivityLifecycleMonitor(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(this);
    }

    public static synchronized boolean isForeground() {
        return foreground;
    }

    public static boolean isBackground() {
        return !isForeground();
    }

    @Nullable
    public Activity getForegroundActivity() {
        return foregroundActivity;
    }

    private static synchronized void setForeground(Context context, boolean foreground) {
        boolean foregroundBefore = ActivityLifecycleMonitor.foreground;
        ActivityLifecycleMonitor.foreground = foreground;
        if (!foregroundBefore && foreground && context != null) {
            dispatchEventToCore(context);
            dispatchEventToModules(context);
            UserSessionTracker.startSessionTracking(context);
        }
    }

    private static void dispatchEventToModules(Context context) {
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

    private static void dispatchEventToCore(Context context) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(LocalEvent.APPLICATION_FOREGROUND.getKey()));
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity instanceof ComponentActivity)
            MobileMessagingCore.getInstance(activity).getPostNotificationsPermissionRequester().onActivityCreated((ComponentActivity) activity);
        else
            MobileMessagingLogger.e("Activity not an instance of ComponentActivity. You'll need to manually require POST_NOTIFICATIONS permission.");
    }

    @Override
    public void onActivityStarted(Activity activity) {
        MobileMessagingCore.getInstance(activity).getPostNotificationsPermissionRequester().onActivityStarted();
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
        if (activity != null) UserSessionTracker.stopSessionTracking(activity);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (activityListener != null)
            activityListener.onActivityStopped(activity);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
