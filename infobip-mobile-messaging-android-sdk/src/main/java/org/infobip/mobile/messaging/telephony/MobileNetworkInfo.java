package org.infobip.mobile.messaging.telephony;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.util.MobileNetworkInformation;
import org.infobip.mobile.messaging.util.PreferenceHelper;

/**
 * Created by sslavin on 22/04/16.
 */
public class MobileNetworkInfo {

    private Context context;
    private String carrierName = "";
    private String mcc = "";
    private String mnc = "";

    private MobileNetworkInfo() {
    }

    private MobileNetworkInfo(Context context) {
        this.context = context;
    }

    public static MobileNetworkInfo fromProperties(Context context) {
        MobileNetworkInfo mobileNetworkInfo = new MobileNetworkInfo();
        mobileNetworkInfo.carrierName = PreferenceHelper.findString(context, MobileMessagingProperty.MOBILE_CARRIER_NAME);
        mobileNetworkInfo.mcc = PreferenceHelper.findString(context, MobileMessagingProperty.MOBILE_COUNTRY_CODE);
        mobileNetworkInfo.mnc = PreferenceHelper.findString(context, MobileMessagingProperty.MOBILE_NETWORK_CODE);
        return mobileNetworkInfo;
    }

    public static MobileNetworkInfo fromSystem(Context context) {
        MobileNetworkInfo mobileNetworkInfo = new MobileNetworkInfo(context);
        mobileNetworkInfo.carrierName = MobileNetworkInformation.getMobileCarrierName(context);
        mobileNetworkInfo.mcc = MobileNetworkInformation.getMobileCoutryCode(context);
        mobileNetworkInfo.mnc = MobileNetworkInformation.getMobileNetworkCode(context);
        return mobileNetworkInfo;
    }

    public void save() {
        PreferenceHelper.saveString(context, MobileMessagingProperty.MOBILE_CARRIER_NAME, carrierName);
        PreferenceHelper.saveString(context, MobileMessagingProperty.MOBILE_COUNTRY_CODE, mcc);
        PreferenceHelper.saveString(context, MobileMessagingProperty.MOBILE_NETWORK_CODE, mnc);
    }

    public boolean isEqual(MobileNetworkInfo mobileNetworkInfo) {
        return (mobileNetworkInfo != null &&
                this.mnc.equals(mobileNetworkInfo.mnc) &&
                this.mcc.equals(mobileNetworkInfo.mcc) &&
                this.carrierName.equals(mobileNetworkInfo.carrierName));
    }

    public String getCarrierName() {
        return carrierName;
    }

    public String getMCC() {
        return mcc;
    }

    public String getMNC() {
        return mnc;
    }
}
