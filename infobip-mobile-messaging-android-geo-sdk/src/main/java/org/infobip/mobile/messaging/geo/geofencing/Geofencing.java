package org.infobip.mobile.messaging.geo.geofencing;


import android.content.Context;
import android.content.pm.PackageManager;

import org.infobip.mobile.messaging.ConfigurationException;

public abstract class Geofencing {

    /**
     * Gets an instance of Geofencing after it is initialized.
     * </p>
     * If the app was killed and there is no instance available, it will return a temporary instance based on current context.
     *
     * @param context android context object.
     * @return instance of Geofencing.
     */
    public synchronized static Geofencing getInstance(Context context) {
        return GeofencingImpl.getInstance(context);
    }

    /**
     * Sets component enabled parameter to enabled or disabled for every required geo component
     *
     * @param context                android context object
     * @param componentsStateEnabled state of the component from {@link PackageManager}
     * @throws ConfigurationException if component is missing in Manifest
     */
    public abstract void setGeoComponentsEnabledSettings(Context context, boolean componentsStateEnabled);

    public abstract void startGeoMonitoring();

    public abstract void stopGeoMonitoring();

    public abstract void cleanup();

    public abstract void logoutUser();
}
