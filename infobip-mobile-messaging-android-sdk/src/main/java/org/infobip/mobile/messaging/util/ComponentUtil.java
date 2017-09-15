package org.infobip.mobile.messaging.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.infobip.mobile.messaging.ConfigurationException;

/**
 * Utility class for component state managmenet
 *
 * @author sslavin
 * @since 14/09/2017.
 */

public class ComponentUtil {

    /**
     * Enables or disables component
     *
     * @param context context object
     * @param enabled desired state
     * @param componentClass class of the component
     *
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
}
