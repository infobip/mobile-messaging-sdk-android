package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import org.infobip.mobile.messaging.gcm.MobileMessagingGcmIntentService;
import org.infobip.mobile.messaging.gcm.PlayServicesSupport;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.telephony.MobileNetworkStateListener;
import org.infobip.mobile.messaging.util.ExceptionUtils;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author sslavin
 * @since 28.04.2016.
 */
public class MobileMessagingCore {
    private static MobileMessagingCore instance;

    private final RegistrationSynchronizer registrationSynchronizer = new RegistrationSynchronizer();
    private final DeliveryReporter deliveryReporter = new DeliveryReporter();
    private final SeenStatusReporter seenStatusReporter = new SeenStatusReporter();
    private final MsisdnSynchronizer msisdnSynchronizer = new MsisdnSynchronizer();
    private final MobileNetworkStateListener mobileNetworkStateListener;
    private final MobileMessagingStats stats;
    private final PlayServicesSupport playServicesSupport = new PlayServicesSupport();
    private NotificationSettings notificationSettings;
    private MessageStore messageStore;
    private Context context;
    private OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (MobileMessagingProperty.MSISDN_TO_REPORT.getKey().equals(key) &&
                PreferenceHelper.contains(context, MobileMessagingProperty.MSISDN_TO_REPORT)) {
                syncMsisdn();
            }
        }
    };

    protected MobileMessagingCore(Context context) {
        this.context = context;
        this.stats = new MobileMessagingStats(context);
        this.mobileNetworkStateListener = new MobileNetworkStateListener(context);
        PreferenceHelper.registerOnSharedPreferenceChangeListener(context, onSharedPreferenceChangeListener);
    }

    public static MobileMessagingCore getInstance(Context context) {
        if (null != instance) {
            return instance;
        }

        instance = new MobileMessagingCore(context);
        return instance;
    }

    public void sync() {
        registrationSynchronizer.syncronize(context, getDeviceApplicationInstanceId(), getRegistrationId(), isRegistrationIdReported(), getStats());
        deliveryReporter.report(context, registrationSynchronizer, getDeviceApplicationInstanceId(), getRegistrationId(), isRegistrationIdReported(), getUnreportedMessageIds(), getStats());
        seenStatusReporter.report(context, registrationSynchronizer, getDeviceApplicationInstanceId(), getRegistrationId(), isRegistrationIdReported(), getUnreportedSeenMessageIds(), getStats());
    }

    public void syncMsisdn() {
        msisdnSynchronizer.syncronize(context, getDeviceApplicationInstanceId(), getUnreportedMsisdn(), isMsisdnReported(), getStats());
    }

    public long getMsisdn() {
        return PreferenceHelper.findLong(context, MobileMessagingProperty.MSISDN);
    }

    public long getUnreportedMsisdn() {
        return PreferenceHelper.findLong(context, MobileMessagingProperty.MSISDN_TO_REPORT);
    }

    protected void setMsisdn(long msisdn) {
        if (msisdn < 0) {
            throw new IllegalArgumentException("MSISDN can't be negative!");
        }
        long oldMsisdn = getMsisdn();
        if (oldMsisdn != 0 && oldMsisdn == msisdn) {
            setMsisdnReported(false);
            return;
        }
        PreferenceHelper.saveLong(context, MobileMessagingProperty.MSISDN_TO_REPORT, msisdn);
    }

    protected boolean isMsisdnReported() {
        return !PreferenceHelper.contains(context, MobileMessagingProperty.MSISDN_TO_REPORT);
    }

    public void setMsisdnReported(boolean success) {
        long reportedMsisdn = PreferenceHelper.findLong(context, MobileMessagingProperty.MSISDN_TO_REPORT);
        if (success && reportedMsisdn > 0) {
            PreferenceHelper.saveLong(context, MobileMessagingProperty.MSISDN, reportedMsisdn);
        }
        PreferenceHelper.remove(context, MobileMessagingProperty.MSISDN_TO_REPORT);
    }

    public String getRegistrationId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.GCM_REGISTRATION_ID);
    }

    public String getDeviceApplicationInstanceId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);
    }

    public String[] getUnreportedMessageIds() {
        return PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS);
    }

    protected void addUnreportedMessageIds(final String... messageIDs) {
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, messageIDs);
    }

    public void removeUnreportedMessageIds(final String... messageIDs) {
        PreferenceHelper.deleteFromStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, messageIDs);
    }

    public String[] getUnreportedSeenMessageIds() {
        return PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS);
    }

    protected void addUnreportedSeenMessageIds(final String... messageIDs) {
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS, messageIDs);
    }

    public void removeUnreportedSeenMessageIds(final String... messageIDs) {
        PreferenceHelper.deleteFromStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS, messageIDs);
    }

    public void setMessagesDelivered(String... messageIds) {
        addUnreportedMessageIds(messageIds);
        sync();
    }

    protected void setMessagesSeen(String... messageIds) {
        if (isMessageStoreEnabled()) {
            for (Message message : messageStore.findAllMatching(context, messageIds)) {
                message.setSeenTimestamp(System.currentTimeMillis());
                messageStore.save(context, message);
            }
        }
        addUnreportedSeenMessageIds(messageIds);
        sync();
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

    protected static void setGcmSenderId(Context context, String gcmSenderId) {
        if (StringUtils.isBlank(gcmSenderId)) {
            throw new IllegalArgumentException("gcmSenderId is mandatory! Get one here: https://developers.google.com/mobile/add?platform=android&cntapi=gcm");
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_SENDER_ID, gcmSenderId);
    }

    public String getGcmSenderId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.GCM_SENDER_ID);
    }

    public boolean isRegistrationIdReported() {
        return registrationSynchronizer.isRegistrationIdReported(context);
    }

    private void setRegistrationIdReported(boolean registrationIdReported) {
        registrationSynchronizer.setRegistrationIdReported(context, registrationIdReported);
    }

    public void setRegistrationId(String registrationId) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_REGISTRATION_ID, registrationId);
        setRegistrationIdReported(false);
    }

    protected static void setMessageStoreClass(Context context, Class<? extends MessageStore> messageStoreClass) {
        String value = null != messageStoreClass ? messageStoreClass.getName() : null;
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, value);
    }

    @SuppressWarnings({"unchecked", "WeakerAccess"})
    protected Class<? extends MessageStore> getMessageStoreClass() {
        return PreferenceHelper.findClass(context, MobileMessagingProperty.MESSAGE_STORE_CLASS);
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

    protected MobileMessagingStats getStats() {
        return stats;
    }

    public void setLastHttpException(Exception lastHttpException) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.LAST_HTTP_EXCEPTION, ExceptionUtils.stacktrace(lastHttpException));
    }

    @SuppressWarnings("unused")
    public String getLastHttpException() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.LAST_HTTP_EXCEPTION);
    }

    private void setApplicationCode(String applicationCode) {
        if (StringUtils.isBlank(applicationCode)) {
            throw new IllegalArgumentException("applicationCode is mandatory! Get one here: https://portal.infobip.com/push/applications");
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, applicationCode);
    }

    public static String getApplicationCode(Context context) {
        return PreferenceHelper.findString(context, MobileMessagingProperty.APPLICATION_CODE);
    }

    protected static void setApiUri(Context context, String apiUri) {
        if (StringUtils.isBlank(apiUri)) {
            throw new IllegalArgumentException("apiUri is mandatory! If in doubt, use " + MobileMessagingProperty.API_URI.getDefaultValue());
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, apiUri);
    }

    public String getApiUri() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.API_URI);
    }

    protected static void setReportCarrierInfo(Context context, boolean reportCarrierInfo) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_CARRIER_INFO, reportCarrierInfo);
    }

    protected static void setReportSystemInfo(Context context, boolean reportSystemInfo) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, reportSystemInfo);
    }

    private static void cleanup(Context context) {
        String gcmSenderID = PreferenceHelper.findString(context, MobileMessagingProperty.GCM_SENDER_ID);
        String gcmToken = PreferenceHelper.findString(context, MobileMessagingProperty.GCM_REGISTRATION_ID);

        Intent intent = new Intent(MobileMessagingGcmIntentService.ACTION_TOKEN_CLEANUP, null, context, MobileMessagingGcmIntentService.class);
        intent.putExtra(MobileMessagingGcmIntentService.EXTRA_GCM_SENDER_ID, gcmSenderID);
        intent.putExtra(MobileMessagingGcmIntentService.EXTRA_GCM_TOKEN, gcmToken);
        context.startService(intent);

        PreferenceHelper.remove(context, MobileMessagingProperty.GCM_REGISTRATION_ID);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED, false);

        PreferenceHelper.remove(context, MobileMessagingProperty.MSISDN_TO_REPORT);
        PreferenceHelper.remove(context, MobileMessagingProperty.MSISDN);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS);
    }

    /**
     * The {@link MobileMessagingCore} builder class.
     *
     * @author sslavin
     * @see MobileMessagingCore
     * @see NotificationSettings.Builder
     * @see NotificationSettings
     * @since 30.05.2016.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static final class Builder {

        private NotificationSettings notificationSettings = null;
        private String applicationCode = null;
        private final Context context;

        public Builder(Context context) {
            if (null == context) {
                throw new IllegalArgumentException("context is mandatory!");
            }
            this.context = context.getApplicationContext();
        }

        private void validateWithParam(Object o) {
            if (null != o) {
                return;
            }
            throw new IllegalArgumentException("Can't use 'with' method with null argument!");
        }

        /**
         * It will set the notification configuration which will be used to display the notification automatically.
         * <pre>
         * {@code new MobileMessagingCore.Builder(context)
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
            this.notificationSettings = notificationSettings;
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
         * Builds the <i>MobileMessagingCore</i> configuration. Registration token sync is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @return {@link MobileMessagingCore}
         */
        public MobileMessagingCore build() {
            if (!applicationCode.equals(MobileMessagingCore.getApplicationCode(context))) {
                MobileMessagingCore.cleanup(context);
            }
            MobileMessagingCore mobileMessagingCore = new MobileMessagingCore(context);
            mobileMessagingCore.setNotificationSettings(notificationSettings);
            mobileMessagingCore.setApplicationCode(applicationCode);
            MobileMessagingCore.instance = mobileMessagingCore;
            mobileMessagingCore.playServicesSupport.checkPlayServices(context);
            return mobileMessagingCore;
        }
    }
}
