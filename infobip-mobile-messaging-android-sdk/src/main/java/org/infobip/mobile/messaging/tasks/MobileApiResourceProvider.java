package org.infobip.mobile.messaging.tasks;

import android.content.Context;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.deliveryreports.MobileApiDeliveryReport;
import org.infobip.mobile.messaging.api.msisdn.MobileApiRegisterMsisdn;
import org.infobip.mobile.messaging.api.registration.MobileApiRegistration;
import org.infobip.mobile.messaging.api.support.Generator;

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

        Properties properties = System.getProperties();
        properties.put("api.key", MobileMessaging.getInstance(context).getApplicationCode());
        generator = new Generator.Builder().
                withBaseUrl(MobileMessaging.getInstance(context).getApiUri()).
                withProperties(properties).
                build();
        return generator;
    }
}
