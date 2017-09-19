package org.infobip.mobile.messaging.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;

/**
 * Created by sslavin on 22/04/16.
 */
public class MobileNetworkInformation {
    private MobileNetworkInformation() {
    }


    public static String getMobileCarrierName(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            return telephonyManager.getNetworkOperatorName();
        }
        return "unknown";
    }

    public static String getMobileCoutryCode(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            return extractMCC(telephonyManager.getNetworkOperator());
        }
        return "unknown";
    }

    public static String getMobileNetworkCode(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            return extractMNC(telephonyManager.getNetworkOperator());
        }
        return "unknown";
    }

    public static String getSIMCarrierName(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            return telephonyManager.getSimOperatorName();
        }
        return "unknown";
    }

    public static String getSIMCoutryCode(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            return extractMCC(telephonyManager.getSimOperator());
        }
        return "unknown";
    }

    public static String getSIMNetworkCode(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            return extractMNC(telephonyManager.getSimOperator());
        }
        return "unknown";
    }

    private static String extractMCC(String operator) {
        if (StringUtils.isNotBlank(operator)) {
            return operator.substring(0, 3);
        }
        return "unknown";
    }

    private static String extractMNC(String operator) {
        if (StringUtils.isNotBlank(operator)) {
            return operator.substring(3);
        }
        return "unknown";
    }

    public static boolean isNetworkAvailableSafely(Context context) {
        Boolean isNetworkAvailable = isNetworkAvailable(context);
        return isNetworkAvailable == null || isNetworkAvailable;
    }

    public static Boolean isNetworkAvailable(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                manager == null) {
            return null;
        }

        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED;
    }
}
