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
    private String simCarrierName = "";
    private String simMcc = "";
    private String simMnc = "";

    private MobileNetworkInfo() {
    }

    private MobileNetworkInfo(Context context) {
        this.context = context;
    }

    static MobileNetworkInfo fromProperties(Context context) {
        MobileNetworkInfo mobileNetworkInfo = new MobileNetworkInfo();
        mobileNetworkInfo.carrierName = PreferenceHelper.findString(context, MobileMessagingProperty.MOBILE_CARRIER_NAME);
        mobileNetworkInfo.mcc = PreferenceHelper.findString(context, MobileMessagingProperty.MOBILE_COUNTRY_CODE);
        mobileNetworkInfo.mnc = PreferenceHelper.findString(context, MobileMessagingProperty.MOBILE_NETWORK_CODE);
        mobileNetworkInfo.simCarrierName = PreferenceHelper.findString(context, MobileMessagingProperty.SIM_CARRIER_NAME);
        mobileNetworkInfo.simMcc = PreferenceHelper.findString(context, MobileMessagingProperty.SIM_COUNTRY_CODE);
        mobileNetworkInfo.simMnc = PreferenceHelper.findString(context, MobileMessagingProperty.SIM_NETWORK_CODE);
        return mobileNetworkInfo;
    }

    static MobileNetworkInfo fromSystem(Context context) {
        MobileNetworkInfo mobileNetworkInfo = new MobileNetworkInfo(context);
        mobileNetworkInfo.carrierName = MobileNetworkInformation.getMobileCarrierName(context);
        mobileNetworkInfo.mcc = MobileNetworkInformation.getMobileCountryCode(context);
        mobileNetworkInfo.mnc = MobileNetworkInformation.getMobileNetworkCode(context);
        mobileNetworkInfo.simCarrierName = MobileNetworkInformation.getSIMCarrierName(context);
        mobileNetworkInfo.simMcc = MobileNetworkInformation.getSIMCountryCode(context);
        mobileNetworkInfo.simMnc = MobileNetworkInformation.getSIMNetworkCode(context);
        return mobileNetworkInfo;
    }

    void save() {
        PreferenceHelper.saveString(context, MobileMessagingProperty.MOBILE_CARRIER_NAME, carrierName);
        PreferenceHelper.saveString(context, MobileMessagingProperty.MOBILE_COUNTRY_CODE, mcc);
        PreferenceHelper.saveString(context, MobileMessagingProperty.MOBILE_NETWORK_CODE, mnc);
        PreferenceHelper.saveString(context, MobileMessagingProperty.SIM_CARRIER_NAME, simCarrierName);
        PreferenceHelper.saveString(context, MobileMessagingProperty.SIM_COUNTRY_CODE, simMcc);
        PreferenceHelper.saveString(context, MobileMessagingProperty.SIM_NETWORK_CODE, simMnc);
    }

    boolean isEqual(MobileNetworkInfo mobileNetworkInfo) {
        return (mobileNetworkInfo != null &&
                this.mnc.equals(mobileNetworkInfo.mnc) &&
                this.mcc.equals(mobileNetworkInfo.mcc) &&
                this.carrierName.equals(mobileNetworkInfo.carrierName)) &&
                this.simMnc.equals(mobileNetworkInfo.simMnc) &&
                this.simMcc.equals(mobileNetworkInfo.simMcc) &&
                this.simCarrierName.equals(mobileNetworkInfo.simCarrierName);
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

    public String getSimCarrierName() {
        return simCarrierName;
    }

    public String getSimMcc() {
        return simMcc;
    }

    public String getSimMnc() {
        return simMnc;
    }
}
