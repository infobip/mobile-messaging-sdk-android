/*
 * MobileNetworkInformation.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

/**
 * Created by sslavin on 22/04/16.
 */
public class MobileNetworkInformation {
    private MobileNetworkInformation() {
    }


    public static String getMobileCarrierName(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            try {
                return telephonyManager.getNetworkOperatorName();
            } catch (SecurityException ex) {
                return "unknown";
            }
        }
        return "unknown";
    }

    public static String getMobileCountryCode(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            try {
                return extractMCC(telephonyManager.getNetworkOperator());
            } catch (SecurityException ex) {
                return "unknown";
            }
        }
        return "unknown";
    }

    public static String getMobileNetworkCode(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            try {
                return extractMNC(telephonyManager.getNetworkOperator());
            } catch (SecurityException ex) {
                return "unknown";
            }
        }
        return "unknown";
    }

    public static String getSIMCarrierName(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            try {
                return telephonyManager.getSimOperatorName();
            } catch (SecurityException ex) {
                return "unknown";
            }
        }
        return "unknown";
    }

    public static String getSIMCountryCode(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            try {
                return extractMCC(telephonyManager.getSimOperator());
            } catch (SecurityException ex) {
                return "unknown";
            }
        }
        return "unknown";
    }

    public static String getSIMNetworkCode(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            try {
                return extractMNC(telephonyManager.getSimOperator());
            } catch (SecurityException ex) {
                return "unknown";
            }
        }
        return "unknown";
    }

    private static String extractMCC(String operator) {
        if (StringUtils.isNotBlank(operator) && operator.length() >= 3) {
            return operator.substring(0, 3);
        }
        return "unknown";
    }

    private static String extractMNC(String operator) {
        if (StringUtils.isNotBlank(operator) && operator.length() >= 3) {
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
