package org.infobip.mobile.messaging;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import org.infobip.mobile.messaging.gcm.PlayServicesSupport;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.ExceptionUtils;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * The main configuration class. It is used to configure and start the Mobile Messaging System.
 * <p>
 * It should is used in the Application entry point.
 * <pre>
 * {@code
 * public class MyActivity extends AppCompatActivity {
 *        protected void onCreate(Bundle savedInstanceState) {
 *            super.onCreate(savedInstanceState);
 *
 *            new MobileMessaging.Builder(this).build();
 *            .....
 *        }
 *    }}
 * </pre>
 *
 * @author mstipanov
 * @see Builder
 * @see Builder#withGcmSenderId(String)
 * @see Builder#withoutDisplayNotification()
 * @see Builder#withMessageStore(Class)
 * @see Builder#withApiUri(String)
 * @see Builder#withApplicationCode(String)
 * @see Builder#withDisplayNotification(NotificationSettings)
 * @see Builder#withoutMessageStore()
 * @since 29.02.2016.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MobileMessaging implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "MobileMessaging";

    private static MobileMessaging instance;
    private final Context context;
    private final RegistrationSynchronizer registrationSynchronizer = new RegistrationSynchronizer();
    private final DeliveryReporter deliveryReporter = new DeliveryReporter();
    private final MsisdnSynchronizer msisdnSynchronizer = new MsisdnSynchronizer();
    private final PlayServicesSupport playServicesSupport = new PlayServicesSupport();
    private final MobileMessagingStats stats;
    private MessageStore messageStore;
    private NotificationSettings notificationSettings;

    private MobileMessaging(Context context) {
        this.context = context;
        stats = new MobileMessagingStats(context);
        PreferenceHelper.registerOnSharedPreferenceChangeListener(context, this);
    }

    public synchronized static MobileMessaging getInstance(Context context) {
        if (null != instance) {
            return instance;
        }

        instance = new MobileMessaging(context);
        return instance;
    }

    public long getMsisdn() {
        return PreferenceHelper.findLong(context, MobileMessagingProperty.MSISDN);
    }

    public void setMsisdn(long msisdn) {
        if (msisdn < 0) {
            throw new IllegalArgumentException("MSISDN can't be negative!");
        }
        long oldMsisdn = getMsisdn();
        if (oldMsisdn != 0 && oldMsisdn == msisdn) {
            return;
        }
        PreferenceHelper.saveLong(context, MobileMessagingProperty.MSISDN, msisdn);
    }

    public String getRegistrationId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.GCM_REGISTRATION_ID);
    }

    public void setRegistrationId(String registrationId) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_REGISTRATION_ID, registrationId);
    }

    public String getDeviceApplicationInstanceId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);
    }

    public String getGcmSenderId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.GCM_SENDER_ID);
    }

    private void setGcmSenderId(String gcmSenderId) {
        if (StringUtils.isBlank(gcmSenderId)) {
            throw new IllegalArgumentException("gcmSenderId is mandatory! Get one here: https://developers.google.com/mobile/add?platform=android&cntapi=gcm");
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_SENDER_ID, gcmSenderId);
    }

    public String getApplicationCode() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.APPLICATION_CODE);
    }

    private void setApplicationCode(String applicationCode) {
        if (StringUtils.isBlank(applicationCode)) {
            throw new IllegalArgumentException("applicationCode is mandatory! Get one here: https://portal.infobip.com/push/applications");
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, applicationCode);
    }

    public String getApiUri() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.API_URI);
    }

    private void setApiUri(String apiUri) {
        if (StringUtils.isBlank(apiUri)) {
            throw new IllegalArgumentException("apiUri is mandatory! If in doubt, use " + MobileMessagingProperty.API_URI.getDefaultValue());
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, apiUri);
    }

    @SuppressWarnings("unused")
    public String getLastHttpException() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.LAST_HTTP_EXCEPTION);
    }

    public void setLastHttpException(Exception lastHttpException) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.LAST_HTTP_EXCEPTION, ExceptionUtils.stacktrace(lastHttpException));
    }

    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public Class<? extends MessageStore> getMessageStoreClass() {
        return PreferenceHelper.findClass(context, MobileMessagingProperty.MESSAGE_STORE_CLASS);
    }

    private void setMessageStoreClass(Class<? extends MessageStore> messageStoreClass) {
        String value = null != messageStoreClass ? messageStoreClass.getName() : null;
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, value);
    }

    public String[] getUnreportedMessageIds() {
        return PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS);

    }

    public void addUnreportedMessageIds(final String... messageIDs) {
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, messageIDs);
    }

    public void removeUnreportedMessageIds(final String... messageIDs) {
        PreferenceHelper.deleteFromStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, messageIDs);
    }

    public NotificationSettings getNotificationSettings() {
        if (!isDisplayNotificationEnabled()) {
            return null;
        }
        if (null != notificationSettings)
            return notificationSettings;

        notificationSettings = new NotificationSettings(context);
        return notificationSettings;
    }

    private void setNotificationSettings(NotificationSettings notificationSettings) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, null != notificationSettings);
        this.notificationSettings = notificationSettings;
    }

    private boolean isDisplayNotificationEnabled() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED);
    }
    //endregion

    public boolean isRegistrationIdSaved() {
        return registrationSynchronizer.isRegistrationIdSaved(context);
    }

    public void setRegistrationIdSaved(boolean registrationIdSaved) {
        registrationSynchronizer.setRegistrationIdSaved(context, registrationIdSaved);
    }

    public boolean isMsisdnSaved() {
        return msisdnSynchronizer.isMsisdnSaved(context);
    }

    public void setMsisdnSaved(boolean msisdnSaved) {
        msisdnSynchronizer.setMsisdnSaved(context, msisdnSaved);
    }

    public void reportUnreportedRegistration() {
        registrationSynchronizer.syncronize(context, getDeviceApplicationInstanceId(), getRegistrationId(), isRegistrationIdSaved(), getStats());
    }

    public void reportUnreportedMessageIds() {
        deliveryReporter.report(context, registrationSynchronizer, getDeviceApplicationInstanceId(), getRegistrationId(), isRegistrationIdSaved(), getUnreportedMessageIds(), getStats());
    }

    public void syncMsisdn() {
        msisdnSynchronizer.syncronize(context, getDeviceApplicationInstanceId(), getMsisdn(), isMsisdnSaved(), getStats());
    }

    public MessageStore getMessageStore() {
        if (!isMessageStoreEnabled()) {
            return null;
        }

        if (null != messageStore) {
            return messageStore;
        }

        Class<? extends MessageStore> messageStoreClass = null;
        try {
            messageStoreClass = getMessageStoreClass();
            messageStore = messageStoreClass.newInstance();
            return messageStore;
        } catch (Exception e) {
            throw new MessageStoreInstantiationException("Can't create message store of type: " + messageStoreClass, e);
        }
    }

    public boolean isMessageStoreEnabled() {
        return null != getMessageStoreClass();
    }

    private MobileMessagingStats getStats() {
        return stats;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (!MobileMessagingProperty.MSISDN.getKey().equals(key)) {
            return;
        }
        setMsisdnSaved(false);
        syncMsisdn();
    }

    /**
     * The {@link MobileMessaging} builder class.
     *
     * @author mstipanov
     * @see MobileMessaging
     * @see NotificationSettings.Builder
     * @see NotificationSettings
     * @see Builder#withGcmSenderId(String)
     * @see Builder#withoutDisplayNotification()
     * @see Builder#withMessageStore(Class)
     * @see Builder#withApiUri(String)
     * @see Builder#withApplicationCode(String)
     * @see Builder#withDisplayNotification(NotificationSettings)
     * @see Builder#withoutMessageStore()
     * @since 29.02.2016.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static final class Builder {
        private final Context context;
        private String gcmSenderId = (String) MobileMessagingProperty.GCM_SENDER_ID.getDefaultValue();
        private String applicationCode = (String) MobileMessagingProperty.APPLICATION_CODE.getDefaultValue();
        private String apiUri = (String) MobileMessagingProperty.API_URI.getDefaultValue();
        private NotificationSettings notificationSettings = null;

        @SuppressWarnings("unchecked")
        private Class<? extends MessageStore> messageStoreClass = (Class<? extends MessageStore>) MobileMessagingProperty.MESSAGE_STORE_CLASS.getDefaultValue();

        public Builder(Context context) {
            if (null == context) {
                throw new IllegalArgumentException("context is mandatory!");
            }
            this.context = context;

            loadDefaultApiUri(context);
            loadGcmSenderId(context);
            loadApplicationCode(context);
            loadNotificationSettings(context);
        }

        private void loadNotificationSettings(Context context) {
            boolean displayNotificationEnabled = (boolean) MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED.getDefaultValue();
            if (!displayNotificationEnabled) {
                return;
            }
            this.notificationSettings = new NotificationSettings.Builder(context).build();
        }

        private void loadDefaultApiUri(Context context) {
            int resource = ResourceLoader.loadResourceByName(context, "string", "infobip_api_uri");
            if (resource > 0) {
                String apiUri = context.getResources().getString(resource);
                if (StringUtils.isNotBlank(apiUri)) {
                    this.apiUri = apiUri;
                }
            }
        }

        private void loadGcmSenderId(Context context) {
            int resource = ResourceLoader.loadResourceByName(context, "string", "google_app_id");
            if (resource > 0) {
                String gcmSenderId = context.getResources().getString(resource);
                if (StringUtils.isNotBlank(gcmSenderId)) {
                    this.gcmSenderId = gcmSenderId;
                }
            }
        }

        private void loadApplicationCode(Context context) {
            int resource = ResourceLoader.loadResourceByName(context, "string", "infobip_application_code");
            if (resource > 0) {
                String applicationCode = context.getResources().getString(resource);
                if (StringUtils.isNotBlank(applicationCode)) {
                    this.applicationCode = applicationCode;
                }
            }
        }

        private void validateWithParam(Object o) {
            if (null != o) {
                return;
            }
            throw new IllegalArgumentException("Can't use 'with' method with null argument!");
        }

        /**
         * When you want to use a GCM sender that is not stored to <i>google_app_id</i> string resource
         * By default it will use <i>google_app_id</i> string resource
         *
         * @param gcmSenderId if you don't have one, you should get one <a href="https://developers.google.com/mobile/add?platform=android&cntapi=gcm">here</a>
         * @return {@link Builder}
         */
        public Builder withGcmSenderId(String gcmSenderId) {
            validateWithParam(gcmSenderId);
            this.gcmSenderId = gcmSenderId;
            return this;
        }

        /**
         * When you want to use the Application code that is not stored to <i>infobip_application_code</i> string resource
         * By default it will use <i>infobip_application_code</i> string resource
         *
         * @param applicationCode if you don't have one, you should get one <a href="https://portal.infobip.com/push/applications">here</a>
         * @return {@link Builder}
         */
        public Builder withApplicationCode(String applicationCode) {
            validateWithParam(applicationCode);
            this.applicationCode = applicationCode;
            return this;
        }

        /**
         * It will configure the system to use a custom API endpoint.
         * <pre>
         * {@code new MobileMessaging.Builder(context)
         *       .withApiUri("http://127.0.0.1")
         *       .build();
         * }
         * </pre>
         * <p>
         * The default us set to <a href="https://oneapi.infobip.com">https://oneapi.infobip.com</a>.
         * <p>
         * It fill fail if set to null or empty string.
         *
         * @param apiUri API endpoint
         * @return {@link Builder}
         */
        public Builder withApiUri(String apiUri) {
            validateWithParam(apiUri);
            this.apiUri = apiUri;
            return this;
        }

        /**
         * It will set the notification configuration which will be used to display the notification automatically.
         * <pre>
         * {@code new MobileMessaging.Builder(context)
         *       .withDisplayNotification(
         *           new NotificationSettings.Builder(this)
         *               .withDisplayNotification()
         *               .withCallbackActivity(MyActivity.class)
         *               .build()
         *       )
         *       .build();}
         * </pre>
         *
         * @return {@link Builder}
         */
        public Builder withDisplayNotification(NotificationSettings notificationSettings) {
            validateWithParam(notificationSettings);
            this.notificationSettings = notificationSettings;
            return this;
        }

        /**
         * It will configure the system not to display the notification automatically.
         * <pre>
         * {@code new MobileMessaging.Builder(context)
         *       .withoutDisplayNotification()
         *       .build();
         * }
         * </pre>
         * <p>
         *
         * @return {@link Builder}
         */
        public Builder withoutDisplayNotification() {
            this.notificationSettings = null;
            return this;
        }

        /**
         * It will set the <i>MessageStore</i> class which will be used to store the messages upon arrival.
         * <pre>
         * {@code new MobileMessaging.Builder(context)
         *       .withMessageStore(MyMessageStore.class)
         *       .build();}
         * </pre>
         *
         * @return {@link Builder}
         */
        public Builder withMessageStore(Class<? extends MessageStore> messageStoreClass) {
            validateWithParam(messageStoreClass);
            this.messageStoreClass = messageStoreClass;
            return this;
        }

        /**
         * It will not use <i>MessageStore</i> and will not store the messages upon arrival.
         * <pre>
         * {@code new MobileMessaging.Builder(context)
         *       .withoutMessageStore()
         *       .build();}
         * </pre>
         *
         * @return {@link Builder}
         */
        public Builder withoutMessageStore() {
            this.messageStoreClass = null;
            return this;
        }

        /**
         * Builds the <i>MobileMessaging</i> configuration. Registration token sync is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @return {@link MobileMessaging}
         */
        public MobileMessaging build() {
            MobileMessaging mobileMessaging = new MobileMessaging(context.getApplicationContext());

            mobileMessaging.setApiUri(apiUri);
            mobileMessaging.setGcmSenderId(gcmSenderId);
            mobileMessaging.setApplicationCode(applicationCode);
            mobileMessaging.setMessageStoreClass(messageStoreClass);
            mobileMessaging.setNotificationSettings(notificationSettings);

            MobileMessaging.instance = mobileMessaging;

            mobileMessaging.playServicesSupport.checkPlayServices(context);
            mobileMessaging.reportUnreportedRegistration();
            mobileMessaging.reportUnreportedMessageIds();
            mobileMessaging.syncMsisdn();

            return mobileMessaging;
        }
    }
}
