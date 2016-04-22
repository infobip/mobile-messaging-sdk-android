package org.infobip.mobile.messaging.tasks;

import android.content.Context;
import android.os.Build;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.deliveryreports.MobileApiDeliveryReport;
import org.infobip.mobile.messaging.api.msisdn.MobileApiRegisterMsisdn;
import org.infobip.mobile.messaging.api.registration.MobileApiRegistration;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.util.SoftwareInformationUtils;
import org.infobip.mobile.messaging.util.SystemInformationUtils;

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

    private Generator getGenerator(Context context) {
        if (null != generator) {
            return generator;
        }

        Properties properties = new Properties();
        properties.putAll(System.getProperties());
        properties.put("os.name", SystemInformationUtils.getAndroidSystemName());
        properties.put("os.version", Build.VERSION.RELEASE);
        properties.put("os.arch", SystemInformationUtils.getAndroidSystemABI());
        properties.put("api.key", MobileMessaging.getInstance(context).getApplicationCode());
        properties.put("library.version", SoftwareInformationUtils.getLibraryVersion());
        properties.put("app.version", SoftwareInformationUtils.getAppVersion(context));
        properties.put("platform.type", "GCM");
        properties.put("device.model", Build.MODEL);
        properties.put("device.vendor", Build.MANUFACTURER);
        generator = new Generator.Builder().
                withBaseUrl(MobileMessaging.getInstance(context).getApiUri()).
                withProperties(properties).
                build();
        return generator;
    }
}
