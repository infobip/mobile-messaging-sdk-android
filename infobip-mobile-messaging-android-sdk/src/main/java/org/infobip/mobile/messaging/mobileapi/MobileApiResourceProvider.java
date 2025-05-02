package org.infobip.mobile.messaging.mobileapi;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.api.appinstance.MobileApiUserData;
import org.infobip.mobile.messaging.api.baseurl.MobileApiBaseUrl;
import org.infobip.mobile.messaging.api.chat.MobileApiChat;
import org.infobip.mobile.messaging.api.inbox.MobileApiInbox;
import org.infobip.mobile.messaging.api.messages.MobileApiMessages;
import org.infobip.mobile.messaging.api.rtc.MobileApiRtc;
import org.infobip.mobile.messaging.api.support.CustomApiHeaders;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.client.Logger;
import org.infobip.mobile.messaging.api.support.http.client.Request;
import org.infobip.mobile.messaging.api.support.http.client.RequestInterceptor;
import org.infobip.mobile.messaging.api.support.http.client.ResponsePreProcessor;
import org.infobip.mobile.messaging.api.version.MobileApiVersion;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.DeviceInformation;
import org.infobip.mobile.messaging.util.MobileNetworkInformation;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.StringUtils;
import org.infobip.mobile.messaging.util.SystemInformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author mstipanov
 * @since 21.03.2016.
 */
public class MobileApiResourceProvider {

    public class BaseUrlManager implements RequestInterceptor, ResponsePreProcessor {

        private final Context context;

        private boolean isPreviousFailed = false;
        private int badRequests = 0;

        public BaseUrlManager(Context context) {
            this.context = context;
        }

        @Override
        public Request intercept(Request request) {
            boolean foreground = ActivityLifecycleMonitor.isForeground();
            request.getHeaders().put(CustomApiHeaders.FOREGROUND.getValue(), Collections.<Object>singletonList(foreground));
            if (foreground) {
                String sessionIdHeader = MobileMessagingCore.getInstance(context).getSessionIdHeader();
                if (StringUtils.isNotBlank(sessionIdHeader)) {
                    request.getHeaders().put(CustomApiHeaders.SESSION_ID.getValue(), Collections.<Object>singletonList(sessionIdHeader));
                }
            }
            request.getHeaders().put(CustomApiHeaders.INSTALLATION_ID.getValue(), Collections.<Object>singletonList(MobileMessagingCore.getInstance(context).getUniversalInstallationId()));
            request.getHeaders().put(CustomApiHeaders.PUSH_REGISTRATION_ID.getValue(), Collections.<Object>singletonList(MobileMessagingCore.getInstance(context).getPushRegistrationId()));
            request.getHeaders().put(CustomApiHeaders.APPLICATION_CODE.getValue(), Collections.<Object>singletonList(MobileMessagingCore.getApplicationCodeHash(context)));

            String baseUrl = MobileMessagingCore.getApiUri(context);
            if (generator.getBaseUrl() == baseUrl) {
                return request;
            }

            String newUri = request.getUri().substring(generator.getBaseUrl().length());
            newUri = org.infobip.mobile.messaging.api.support.util.StringUtils.join("/", baseUrl, newUri);
            request.setUri(newUri);
            generator.setBaseUrl(baseUrl);

            return request;
        }

        @Override
        public void beforeResponse(int responseCode, Map<String, List<String>> headers) {
            if (PreferenceHelper.findBoolean(context, MobileMessagingProperty.API_URI_PROVIDED_BY_INTEGRATOR)) {
                return;
            }

            boolean isFailedNotThrottlingRequest = responseCode >= 400 && responseCode != 429 && responseCode != 404;

            if (isFailedNotThrottlingRequest) {
                if (isPreviousFailed) {
                    badRequests++;
                }
                if (badRequests >= 1) {
                    MobileMessagingCore.resetApiUri(context);
                    generator.setBaseUrl((String) MobileMessagingProperty.API_URI.getDefaultValue());
                    badRequests = 0;
                    isPreviousFailed = false;
                    return;
                }
            }

            isPreviousFailed = isFailedNotThrottlingRequest;

            List<String> values = headers.get(CustomApiHeaders.NEW_BASE_URL.getValue());
            if (values == null || values.isEmpty() || StringUtils.isBlank(values.get(0))) {
                return;
            }

            String url = values.get(0);
            MobileMessagingCore.setApiUri(context, url);
            generator.setBaseUrl(url);
        }

        @Override
        public void beforeResponse(Exception error) {
        }
    }

    private BaseUrlManager mobileMessagingRequestInterceptor;
    private Generator generator;
    private MobileApiMessages mobileApiMessages;
    private MobileApiVersion mobileApiVersion;
    private MobileApiAppInstance mobileApiAppInstance;
    private MobileApiUserData mobileApiUserData;
    private MobileApiChat mobileApiChat;
    private MobileApiBaseUrl mobileApiBaseUrl;
    private MobileApiInbox mobileApiInbox;
    private MobileApiRtc mobileApiRtc;

    public MobileApiMessages getMobileApiMessages(Context context) {
        if (null != mobileApiMessages) {
            return mobileApiMessages;
        }

        mobileApiMessages = getGenerator(context).create(MobileApiMessages.class);

        return mobileApiMessages;
    }

    public MobileApiVersion getMobileApiVersion(Context context) {
        if (null != mobileApiVersion) {
            return mobileApiVersion;
        }

        mobileApiVersion = getGenerator(context).create(MobileApiVersion.class);

        return mobileApiVersion;
    }

    public MobileApiAppInstance getMobileApiAppInstance(Context context) {
        if (null != mobileApiAppInstance) {
            return mobileApiAppInstance;
        }

        mobileApiAppInstance = getGenerator(context).create(MobileApiAppInstance.class);

        return mobileApiAppInstance;
    }

    public MobileApiUserData getMobileApiUserData(Context context) {
        if (null != mobileApiUserData) {
            return mobileApiUserData;
        }

        mobileApiUserData = getGenerator(context).create(MobileApiUserData.class);

        return mobileApiUserData;
    }

    public MobileApiChat getMobileApiChat(Context context) {
        if (null != mobileApiChat) {
            return mobileApiChat;
        }

        mobileApiChat = getGenerator(context).create(MobileApiChat.class);

        return mobileApiChat;
    }

    public MobileApiBaseUrl getMobileApiBaseUrl(Context context) {
        if (null != mobileApiBaseUrl) {
            return mobileApiBaseUrl;
        }

        mobileApiBaseUrl = getGenerator(context).create(MobileApiBaseUrl.class);

        return mobileApiBaseUrl;
    }

    public MobileApiInbox getMobileApiInbox(Context context) {
        if (null != mobileApiInbox) {
            return mobileApiInbox;
        }

        mobileApiInbox = getGenerator(context).create(MobileApiInbox.class);

        return mobileApiInbox;
    }

    public MobileApiRtc getMobileApiRtc(Context context) {
        if (mobileApiRtc == null) {
            mobileApiRtc = getGenerator(context).create(MobileApiRtc.class);
        }
        return mobileApiRtc;
    }

    private String[] getUserAgentAdditions(Context context) {
        List<String> userAgentAdditions = new ArrayList<>();
        if (PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO)) {
            userAgentAdditions.add(SystemInformation.getAndroidSystemName());
            userAgentAdditions.add(SystemInformation.getAndroidSystemVersion());
            userAgentAdditions.add(SystemInformation.getAndroidSystemABI());
            userAgentAdditions.add(DeviceInformation.getDeviceModel());
            userAgentAdditions.add(DeviceInformation.getDeviceManufacturer());
            userAgentAdditions.add(SoftwareInformation.getAppName(context));
            userAgentAdditions.add(SoftwareInformation.getAppVersion(context));
            userAgentAdditions.add(SystemInformation.getAndroidDeviceName(context));
        } else {
            String[] emptySystemInfo = {"", "", "", "", "", "", "", ""};
            userAgentAdditions.addAll(Arrays.asList(emptySystemInfo));
        }
        if (PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_CARRIER_INFO)) {
            userAgentAdditions.add(MobileNetworkInformation.getMobileCarrierName(context));
            userAgentAdditions.add(MobileNetworkInformation.getMobileNetworkCode(context));
            userAgentAdditions.add(MobileNetworkInformation.getMobileCountryCode(context));
            userAgentAdditions.add(MobileNetworkInformation.getSIMCarrierName(context));
            userAgentAdditions.add(MobileNetworkInformation.getSIMNetworkCode(context));
            userAgentAdditions.add(MobileNetworkInformation.getSIMCountryCode(context));
        } else {
            String[] emptyCarrierInfoAdditions = {"", "", "", "", "", ""};
            userAgentAdditions.addAll(Arrays.asList(emptyCarrierInfoAdditions));
        }
        List<String> userAgentAdditionsCleaned = new ArrayList<>();
        for (String addition : userAgentAdditions) {
            userAgentAdditionsCleaned.add(removeNotSupportedChars(addition));
        }
        return userAgentAdditionsCleaned.toArray(new String[0]);
    }

    private boolean shouldAllowUntrustedSSLOnError(Context context) {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.ALLOW_UNTRUSTED_SSL_ON_ERROR);
    }

    private Generator getGenerator(Context context) {
        if (null != generator) {
            return generator;
        }

        Properties properties = new Properties();
        try {
            properties.putAll(System.getProperties());
        } catch (Exception e) {
            MobileMessagingLogger.e("System properties not included in Http client headers.", e);
        }

        String appCode = MobileMessagingCore.getApplicationCode(context);
        if (StringUtils.isNotBlank(appCode))
            properties.put("api.key", MobileMessagingCore.getApplicationCode(context));
        else
            MobileMessagingLogger.e("Missing api.key value, application code is not provided to MobileMessaging library.");

        String sdkVersion = SoftwareInformation.getSDKVersionWithPostfixForUserAgent(context);
        if (StringUtils.isNotBlank(sdkVersion))
            properties.put("library.version", sdkVersion);
        else
            MobileMessagingLogger.e("Missing library.version value, sdk version is not available.");


        generator = new Generator.Builder()
                .withBaseUrl(MobileMessagingCore.getApiUri(context))
                .withProperties(properties)
                .withUserAgentAdditions(getUserAgentAdditions(context))
                .withRequestInterceptors(baseUrlManager(context))
                .withResponseHeaderInterceptors(baseUrlManager(context))
                .withLogger(new AndroidHTTPLogger())
                .withAllowUntrustedSSLOnError(shouldAllowUntrustedSSLOnError(context))
                .build();

        return generator;
    }

    private BaseUrlManager baseUrlManager(Context context) {
        if (mobileMessagingRequestInterceptor == null) {
            mobileMessagingRequestInterceptor = new BaseUrlManager(context);
        }
        return mobileMessagingRequestInterceptor;
    }

    static class AndroidHTTPLogger extends Logger {

        private static final String TAG = "MMHTTP";

        @Override
        public void e(String message) {
            MobileMessagingLogger.e(TAG, message);
        }

        @Override
        public void w(String message) {
            MobileMessagingLogger.w(TAG, message);
        }

        @Override
        public void i(String message) {
            MobileMessagingLogger.i(TAG, message);
        }

        @Override
        public void d(String message) {
            MobileMessagingLogger.d(TAG, message);
        }
    }

    /**
     * Related to bug: Caused by: java.lang.IllegalArgumentException: Unexpected char 0x11 at 100 in header value:
     *
     * @param str
     * @return
     */
    public String removeNotSupportedChars(String str) {
        if (str == null) {
            return null;
        }
        String result = str.replaceAll("[^\\x20-\\x7F]", "");
        if (!str.equals(result)) {
            MobileMessagingLogger.i("SPEC_CHAR", "Removed special char at string: " + str);
        }
        return result;
    }
}
