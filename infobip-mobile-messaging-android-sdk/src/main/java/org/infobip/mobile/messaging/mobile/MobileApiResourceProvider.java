package org.infobip.mobile.messaging.mobile;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.data.MobileApiData;
import org.infobip.mobile.messaging.api.geo.MobileApiGeo;
import org.infobip.mobile.messaging.api.messages.MobileApiMessages;
import org.infobip.mobile.messaging.api.registration.MobileApiRegistration;
import org.infobip.mobile.messaging.api.support.CustomApiHeaders;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.version.MobileApiVersion;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.util.DeviceInformation;
import org.infobip.mobile.messaging.util.MobileNetworkInformation;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.SystemInformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author mstipanov
 * @since 21.03.2016.
 */
public class MobileApiResourceProvider {

    public static class MobileMessagingHeaderProvider implements Generator.CommonHeaderProvider {

        private final Context context;

        public MobileMessagingHeaderProvider(Context context) {
            this.context = context;
        }

        @Override
        public Map<String, Collection<Object>> get() {
            return new HashMap<String, Collection<Object>>() {{
                put(CustomApiHeaders.FOREGROUND.getValue(), Collections.<Object>singletonList(ActivityLifecycleMonitor.isForeground()));
                put(CustomApiHeaders.PUSH_REGISTRATION_ID.getValue(), Collections.<Object>singletonList(MobileMessagingCore.getInstance(context).getPushRegistrationId()));
            }};
        }
    }

    private MobileMessagingHeaderProvider mobileMessagingDynamicHeaderProvider;
    private Generator generator;
    private MobileApiRegistration mobileApiRegistration;
    private MobileApiMessages mobileApiMessages;
    private MobileApiData mobileApiData;
    private MobileApiVersion mobileApiVersion;
    private MobileApiGeo mobileApiGeo;

    public MobileApiRegistration getMobileApiRegistration(Context context) {
        if (null != mobileApiRegistration) {
            return mobileApiRegistration;
        }

        mobileApiRegistration = getGenerator(context).create(MobileApiRegistration.class);

        return mobileApiRegistration;
    }

    public MobileApiMessages getMobileApiMessages(Context context) {
        if (null != mobileApiMessages) {
            return mobileApiMessages;
        }

        mobileApiMessages = getGenerator(context).create(MobileApiMessages.class);

        return mobileApiMessages;
    }

    public MobileApiData getMobileApiData(Context context) {
        if (null != mobileApiData) {
            return mobileApiData;
        }

        mobileApiData = getGenerator(context).create(MobileApiData.class);

        return mobileApiData;
    }

    public MobileApiVersion getMobileApiVersion(Context context) {
        if (null != mobileApiVersion) {
            return mobileApiVersion;
        }

        mobileApiVersion = getGenerator(context).create(MobileApiVersion.class);

        return mobileApiVersion;
    }

    public MobileApiGeo getMobileApiGeo(Context context) {
        if (null != mobileApiGeo) {
            return mobileApiGeo;
        }

        mobileApiGeo = getGenerator(context).create(MobileApiGeo.class);

        return mobileApiGeo;
    }

    String[] getUserAgentAdditions(Context context) {
        List<String> userAgentAdditions = new ArrayList<>();
        if (PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO)) {
            userAgentAdditions.add(SystemInformation.getAndroidSystemName());
            userAgentAdditions.add(SystemInformation.getAndroidSystemVersion());
            userAgentAdditions.add(SystemInformation.getAndroidSystemABI());
            userAgentAdditions.add(DeviceInformation.getDeviceModel());
            userAgentAdditions.add(DeviceInformation.getDeviceManufacturer());
            userAgentAdditions.add(SoftwareInformation.getAppName(context));
            userAgentAdditions.add(SoftwareInformation.getAppVersion(context));
        } else {
            String emptySystemInfo[] = {"", "", "", "", "", "", ""};
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
        properties.put("library.version", SoftwareInformation.getSDKVersionWithPostfixForUserAgent(context));

        generator = new Generator.Builder().
                withBaseUrl(MobileMessagingCore.getApiUri(context)).
                withProperties(properties).
                withUserAgentAdditions(getUserAgentAdditions(context)).
                withDynamicHeaderProvider(mobileMessagingDynamicHeaderProvider(context)).
                build();

        return generator;
    }

    private MobileMessagingHeaderProvider mobileMessagingDynamicHeaderProvider(Context context) {
        if (mobileMessagingDynamicHeaderProvider == null) {
            mobileMessagingDynamicHeaderProvider = new MobileMessagingHeaderProvider(context);
        }
        return mobileMessagingDynamicHeaderProvider;
    }
}
