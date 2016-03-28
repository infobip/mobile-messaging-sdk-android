package org.infobip.mobile.messaging.tasks;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.deliveryreports.MobileApiDeliveryReport;
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

    public MobileApiRegistration getMobileApiRegistration() {
        if (null != mobileApiRegistration) {
            return mobileApiRegistration;
        }

        mobileApiRegistration = getGenerator().create(MobileApiRegistration.class);

        return mobileApiRegistration;
    }

    public MobileApiDeliveryReport getMobileApiDeliveryReport() {
        if (null != mobileApiDeliveryReport) {
            return mobileApiDeliveryReport;
        }

        mobileApiDeliveryReport = getGenerator().create(MobileApiDeliveryReport.class);

        return mobileApiDeliveryReport;
    }

    private Generator getGenerator() {
        if (null != generator) {
            return generator;
        }

        Properties properties = System.getProperties();
        properties.put("api.key", MobileMessaging.getInstance().getApplicationCode());
        generator = new Generator.Builder().
                withBaseUrl(MobileMessaging.getInstance().getApiUri()).
                withProperties(properties).
                build();
        return generator;
    }
}
