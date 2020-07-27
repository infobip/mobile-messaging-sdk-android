package org.infobip.mobile.messaging.mobileapi.apiavailability;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class ApiAvailability {

    /**
     * Chack ApiAvailability status and return ConnectionResult code
     * @param context
     * @return
     */
    public int checkServicesStatus(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        return apiAvailability.isGooglePlayServicesAvailable(context);
    }

    public boolean isServicesAvailable(Context context) {
        return checkServicesStatus(context) == ConnectionResult.SUCCESS;
    }

    public boolean isUserResolvableError(int errorCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        return apiAvailability.isUserResolvableError(errorCode);
    }

    public Dialog getErrorDialog(Activity activity, int errorCode, int platformError, DialogInterface.OnCancelListener listner) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        return apiAvailability.getErrorDialog(activity, errorCode, platformError, listner);
    }

}
