package org.infobip.mobile.messaging.geo;


import android.Manifest;
import android.content.Context;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

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

    /**
     * Will automatically ask for permissions and show the dialog with information that not all required permissions are granted, before activating Geofencing.
     * @param shouldShowPermissionsNotGrantedDialogIfShownOnce should application show the dialog with information that not all required permissions are granted, after it was already shown once.
     * <pre>
     * Recommendations:
     * <br>
     * - If you are asking for permissions by button tap, better to return true, so user will be informed, why an action can't be done, if the user didn't grant the permissions.
     * <br>
     * - If you are asking for permissions on the application start, without any additional user actions, better to return false not to disturb the user constantly.
     * </pre>
     * If you want to customize text for dialog with information that not all required permissions are granted, change following strings in your resources:
     * <pre>
     * {@code
     *  <string name="geofencing_permissions_not_granted_title">custom title</string>
     *  <string name="geofencing_permissions_not_granted_message">custom message</string>
     * }
     * </pre>
     */
    public abstract void activateGeofencingWithAutomaticPermissionsRequest(boolean shouldShowPermissionsNotGrantedDialogIfShownOnce);

    /**
     * This method will call `registerForActivityResult()` for provided activity or fragment in order to be able to ask permissions automatically.
     * `registerForActivityResult()` is safe to call before your fragment or activity is created, so call `setContextForRequestingPermissions` before your fragment or activity is created, for example inside it's `onCreate` method.
     * @param activity activity which will request for permissions and show the dialog with information that not all required permissions are granted.
     */
    public abstract void setContextForRequestingPermissions(AppCompatActivity activity);

    /**
     * This method will call `registerForActivityResult()` for provided activity or fragment in order to be able to ask permissions automatically.
     * `registerForActivityResult()` is safe to call before your fragment or activity is created, so call `setContextForRequestingPermissions` before your fragment or activity is created, for example inside it's `onCreate` method.
     * @param fragment fragment which will request for permissions and show the dialog with information that not all required permissions are granted.
     */
    public abstract void setContextForRequestingPermissions(Fragment fragment);
}
