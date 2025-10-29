/*
 * ComponentUtil.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.util;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;

import org.infobip.mobile.messaging.ConfigurationException;
import org.infobip.mobile.messaging.LocalEvent;
import org.infobip.mobile.messaging.MobileMessagingConnectivityReceiver;
import org.infobip.mobile.messaging.MobileMessagingSynchronizationReceiver;
import org.infobip.mobile.messaging.cloud.firebase.MobileMessagingFirebaseService;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.MobileMessagingJobService;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Utility class for component state management
 *
 * @author sslavin
 * @since 14/09/2017.
 */

public class ComponentUtil {

    /**
     * Enables or disables component
     *
     * @param context        context object
     * @param enabled        desired state
     * @param componentClass class of the component
     * @throws ConfigurationException if desired component is not registered in manifest
     */
    public static void setState(Context context, boolean enabled, Class componentClass) {
        int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        ComponentName componentName = new ComponentName(context, componentClass);
        try {
            context.getPackageManager().setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            throw new ConfigurationException(ConfigurationException.Reason.MISSING_REQUIRED_COMPONENT, componentClass.getCanonicalName());
        }
    }

    public static void setSynchronizationReceiverStateEnabled(Context context, MobileMessagingSynchronizationReceiver syncReceiver, boolean enabled) {
        if (enabled) {
            LocalBroadcastManager.getInstance(context).registerReceiver(syncReceiver, new IntentFilter(LocalEvent.APPLICATION_FOREGROUND.getKey()));
        } else {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(syncReceiver);
        }
    }

    public static void setConnectivityComponentsStateEnabled(Context context, boolean enabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            ComponentUtil.setState(context, enabled, MobileMessagingConnectivityReceiver.class);
        } else {
            ComponentUtil.setState(context, enabled, MobileMessagingJobService.class);
        }
    }

    /**
     * Verifies that manifest contains all components needed for normal operation of push.
     *
     * @param context context object
     * @throws ConfigurationException if any of desired components is not registered in manifest
     */
    public static void verifyManifestComponentsForPush(Context context) {
        verifyManifestService(context, MobileMessagingFirebaseService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            verifyManifestService(context, MobileMessagingJobService.class);
        } else {
            verifyManifestReceiver(context, MobileMessagingConnectivityReceiver.class);
        }
    }

    public static void enableComponent(Context context, Class componentClass) {
        try {
            ComponentUtil.setState(context, true, componentClass);
            MobileMessagingLogger.d("Enabled " + componentClass.getName() + " for compatibility reasons");
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot enable " + componentClass.getName() + ": ", e);
        }
    }

    public static void enableComponent(Context context, String fullClassName) {
        try {
            enableComponent(context, Class.forName(fullClassName));
        } catch (ClassNotFoundException ignored) {
        }
    }

    public static void verifyManifestService(Context context, Class<? extends Service> cls) {
        try {
            context.getPackageManager().getServiceInfo(new ComponentName(context, cls), PackageManager.GET_DISABLED_COMPONENTS);
        } catch (Exception ignored) {
            reportMissingComponent(context, cls);
        }
    }

    public static void verifyManifestReceiver(Context context, Class<? extends BroadcastReceiver> cls) {
        try {
            context.getPackageManager().getReceiverInfo(new ComponentName(context, cls), PackageManager.GET_DISABLED_COMPONENTS);
        } catch (Exception ignored) {
            reportMissingComponent(context, cls);
        }
    }

    private static void reportMissingComponent(Context context, Class cls) {
        ConfigurationException exception = new ConfigurationException(ConfigurationException.Reason.MISSING_REQUIRED_COMPONENT, cls.getCanonicalName());
        if (SoftwareInformation.isDebuggableApplicationBuild(context)) {
            throw exception;
        } else {
            MobileMessagingLogger.e(exception.getMessage(), exception);
        }
    }
}