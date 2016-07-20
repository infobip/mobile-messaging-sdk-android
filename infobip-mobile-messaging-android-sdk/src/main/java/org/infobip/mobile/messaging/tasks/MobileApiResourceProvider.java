package org.infobip.mobile.messaging.tasks;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.deliveryreports.MobileApiDeliveryReport;
import org.infobip.mobile.messaging.api.registration.MobileApiRegistration;
import org.infobip.mobile.messaging.api.seenstatus.MobileApiSeenStatusReport;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.userdata.MobileApiUserDataSync;
import org.infobip.mobile.messaging.util.DeviceInformation;
import org.infobip.mobile.messaging.util.MobileNetworkInformation;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.SystemInformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author mstipanov
 * @since 21.03.2016.
 */
public enum MobileApiResourceProvider {
    INSTANCE;

    private Generator generator;
    private MobileApiRegistration mobileApiRegistration;
    private MobileApiDeliveryReport mobileApiDeliveryReport;
    private MobileApiSeenStatusReport mobileApiSeenStatusReport;
    private MobileApiUserDataSync mobileApiUserDataSync;

    public MobileApiRegistration getMobileApiRegistration(Context context) {
        if (null != mobileApiRegistration) {
            return mobileApiRegistration;
        }

        mobileApiRegistration = getGenerator(context).create(MobileApiRegistration.class);

        return mobileApiRegistration;
    }

    public MobileApiDeliveryReport getMobileApiDeliveryReport(Context context) {
        if (null != mobileApiDeliveryReport) {
            return mobileApiDeliveryReport;
        }

        mobileApiDeliveryReport = getGenerator(context).create(MobileApiDeliveryReport.class);

        return mobileApiDeliveryReport;
    }

    public MobileApiSeenStatusReport getMobileApiSeenStatusReport(Context context) {
        if (null != mobileApiSeenStatusReport) {
            return mobileApiSeenStatusReport;
        }

        mobileApiSeenStatusReport = getGenerator(context).create(MobileApiSeenStatusReport.class);

        return mobileApiSeenStatusReport;
    }

    public MobileApiUserDataSync getMobileApiUserDataSync(Context context) {
        if (null != mobileApiUserDataSync) {
            return mobileApiUserDataSync;
        }

        mobileApiUserDataSync = getGenerator(context).create(MobileApiUserDataSync.class);

        return mobileApiUserDataSync;
    }

    String[] getUserAgentAdditions(Context context) {
        List<String> userAgentAdditions = new ArrayList<String>();
        if (PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO)) {
            userAgentAdditions.add(SystemInformation.getAndroidSystemName());
            userAgentAdditions.add(SystemInformation.getAndroidSystemVersion());
            userAgentAdditions.add(SystemInformation.getAndroidSystemABI());
            userAgentAdditions.add(DeviceInformation.getDeviceModel());
            userAgentAdditions.add(DeviceInformation.getDeviceManufacturer());
            userAgentAdditions.add(SoftwareInformation.getAppName(context));
            userAgentAdditions.add(SoftwareInformation.getAppVersion(context));
        } else {
            String emptySystemInfo[] = {"","","","","","",""};
            userAgentAdditions.addAll(Arrays.asList(emptySystemInfo));
        }
        if (PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_CARRIER_INFO)) {
            userAgentAdditions.add(MobileNetworkInformation.getMobileCarrierName(context));
            userAgentAdditions.add(MobileNetworkInformation.getMobileNetworkCode(context));
            userAgentAdditions.add(MobileNetworkInformation.getMobileCoutryCode(context));
            userAgentAdditions.add(MobileNetworkInformation.getSIMCarrierName(context));
            userAgentAdditions.add(MobileNetworkInformation.getSIMNetworkCode(context));
            userAgentAdditions.add(MobileNetworkInformation.getSIMCoutryCode(context));
        } else {
            String emptyCarrierInfoAdditions[] = {"", "", "", "", "", ""};
            userAgentAdditions.addAll(Arrays.asList(emptyCarrierInfoAdditions));
        }
        return userAgentAdditions.toArray(new String[userAgentAdditions.size()]);
    }

    private Generator getGenerator(Context context) {
        if (null != generator) {
            return generator;
        }

        Properties properties = new Properties();
        properties.putAll(System.getProperties());
        properties.put("api.key", MobileMessagingCore.getApplicationCode(context));
        properties.put("library.version", SoftwareInformation.getLibraryVersion());

        generator = new Generator.Builder().
                withBaseUrl(MobileMessagingCore.getInstance(context).getApiUri()).
                withProperties(properties).
                withUserAgentAdditions(getUserAgentAdditions(context)).
                build();
        return generator;
    }

    public void resetMobileApi() {
        generator = null;
        mobileApiRegistration = null;
        mobileApiSeenStatusReport = null;
        mobileApiDeliveryReport = null;
        mobileApiUserDataSync = null;
    }
}
