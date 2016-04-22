package org.infobip.mobile.messaging.util;

import android.content.Context;
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
            String networkOperator = telephonyManager.getNetworkOperator();
            if (StringUtils.isNotBlank(networkOperator)) {
                return networkOperator.substring(0, 3);
            }
        }
        return "unknown";
    }

    public static String getMobileNetworkCode(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            String networkOperator = telephonyManager.getNetworkOperator();
            if (StringUtils.isNotBlank(networkOperator)) {
                return networkOperator.substring(3);
            }
        }
        return "unknown";
    }
}
