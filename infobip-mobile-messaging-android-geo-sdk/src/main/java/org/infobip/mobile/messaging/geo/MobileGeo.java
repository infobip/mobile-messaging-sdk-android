package org.infobip.mobile.messaging.geo;


import android.Manifest;
import android.content.Context;
import android.support.annotation.RequiresPermission;

public abstract class MobileGeo {

    /**
     * Gets an instance of MobileGeo after it is initialized.
     * <br>
     * If the app was killed and there is no instance available, it will return a temporary instance based on current context.
     *
     * @param context android context object.
     * @return instance of MobileGeo.
     */
    public synchronized static MobileGeo getInstance(Context context) {
        return MobileGeoImpl.getInstance(context);
    }

    /**
     * Starts tracking geofence areas.
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public abstract void activateGeofencing();

    /**
     * Stops tracking geofence areas.
     */
    public abstract void deactivateGeofencing();

    /**
     * Checks if geo area tracking is activated.
     *
     * @return Current geofencing status.
     */
    public abstract boolean isGeofencingActivated();

    /**
     * Cleans up MobileGeo installation and removes all geofences and settings.
     */
    public abstract void cleanup();
}
