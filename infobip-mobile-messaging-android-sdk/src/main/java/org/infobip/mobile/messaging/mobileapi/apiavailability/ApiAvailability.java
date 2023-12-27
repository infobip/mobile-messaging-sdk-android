package org.infobip.mobile.messaging.mobileapi.apiavailability;

import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;

public class ApiAvailability {

    /**
     * Chack ApiAvailability status and return ConnectionResult code
     * @param context
     * @return
     */
    public int checkServicesStatus(Context context) {
        GoogleApiAvailabilityLight apiAvailability = GoogleApiAvailabilityLight.getInstance();
        return apiAvailability.isGooglePlayServicesAvailable(context);
    }

    public boolean isServicesAvailable(Context context) {
        return checkServicesStatus(context) == ConnectionResult.SUCCESS;
    }

    public boolean isUserResolvableError(int errorCode) {
        GoogleApiAvailabilityLight apiAvailability = GoogleApiAvailabilityLight.getInstance();
        return apiAvailability.isUserResolvableError(errorCode);
    }
}
