package org.infobip.mobile.messaging.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.ConfigurationException;
import org.infobip.mobile.messaging.LocalEvent;
import org.infobip.mobile.messaging.MobileMessagingConnectivityReceiver;
import org.infobip.mobile.messaging.MobileMessagingSynchronizationReceiver;
import org.infobip.mobile.messaging.platform.MobileMessagingJobService;

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

    public static void setSyncronizationReceiverStateEnabled(Context context, MobileMessagingSynchronizationReceiver syncReceiver, boolean enabled) {
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
}
