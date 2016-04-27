package org.infobip.mobile.messaging.tasks;

import android.content.Context;
import android.os.Build;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.deliveryreports.MobileApiDeliveryReport;
import org.infobip.mobile.messaging.api.msisdn.MobileApiRegisterMsisdn;
import org.infobip.mobile.messaging.api.registration.MobileApiRegistration;
import org.infobip.mobile.messaging.api.seenstatus.MobileApiSeenStatusReport;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.telephony.MobileNetworkInfo;
import org.infobip.mobile.messaging.util.DeviceInformation;
import org.infobip.mobile.messaging.util.MobileNetworkInformation;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.SystemInformation;

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
    private MobileApiRegisterMsisdn mobileApiRegisterMsisdn;
    private MobileApiSeenStatusReport mobileApiSeenStatusReport;

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

    public MobileApiRegisterMsisdn getMobileApiRegisterMsisdn(Context context) {
        if (null != mobileApiRegisterMsisdn) {
            return mobileApiRegisterMsisdn;
        }

        mobileApiRegisterMsisdn = getGenerator(context).create(MobileApiRegisterMsisdn.class);

        return mobileApiRegisterMsisdn;
    }

    public MobileApiSeenStatusReport getMobileApiSeenStatusReport(Context context) {
        if (null != mobileApiSeenStatusReport) {
            return mobileApiSeenStatusReport;
        }

        mobileApiSeenStatusReport = getGenerator(context).create(MobileApiSeenStatusReport.class);

        return mobileApiSeenStatusReport;
    }

    private Generator getGenerator(Context context) {
        if (null != generator) {
            return generator;
        }

        Properties properties = new Properties();
        properties.putAll(System.getProperties());
        properties.put("api.key", MobileMessaging.getInstance(context).getApplicationCode());
        properties.put("library.version", SoftwareInformation.getLibraryVersion());

        String userAgentAdditions[] = {
                SystemInformation.getAndroidSystemName(),
                SystemInformation.getAndroidSystemVersion(),
                SystemInformation.getAndroidSystemABI(),
                DeviceInformation.getDeviceModel(),
                DeviceInformation.getDeviceManufacturer(),
                SoftwareInformation.getAppName(context),
                SoftwareInformation.getAppVersion(context),
                MobileNetworkInformation.getMobileCarrierName(context),
                MobileNetworkInformation.getMobileNetworkCode(context),
                MobileNetworkInformation.getMobileCoutryCode(context),
                MobileNetworkInformation.getSIMCarrierName(context),
                MobileNetworkInformation.getSIMNetworkCode(context),
                MobileNetworkInformation.getSIMCoutryCode(context)
            };

        generator = new Generator.Builder().
                withBaseUrl(MobileMessaging.getInstance(context).getApiUri()).
                withProperties(properties).
                withUserAgentAdditions(userAgentAdditions).
                build();
        return generator;
    }
}
