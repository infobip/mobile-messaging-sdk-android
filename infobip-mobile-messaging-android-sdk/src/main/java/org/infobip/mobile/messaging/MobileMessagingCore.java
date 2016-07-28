package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import org.infobip.mobile.messaging.MobileMessaging.OnReplyClickListener;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;

import org.infobip.mobile.messaging.gcm.MobileMessagingGcmIntentService;
import org.infobip.mobile.messaging.gcm.PlayServicesSupport;
import org.infobip.mobile.messaging.reporters.DeliveryReporter;
import org.infobip.mobile.messaging.reporters.MessageReporter;
import org.infobip.mobile.messaging.reporters.RegistrationSynchronizer;
import org.infobip.mobile.messaging.reporters.SeenStatusReporter;
import org.infobip.mobile.messaging.reporters.UserDataSynchronizer;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.telephony.MobileNetworkStateListener;
import org.infobip.mobile.messaging.util.ExceptionUtils;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author sslavin
 * @since 28.04.2016.
 */
public class MobileMessagingCore {
    private static MobileMessagingCore instance;
    private final RegistrationSynchronizer registrationSynchronizer = new RegistrationSynchronizer();
    private final DeliveryReporter deliveryReporter = new DeliveryReporter();
    private final SeenStatusReporter seenStatusReporter = new SeenStatusReporter();
    private final UserDataSynchronizer userDataSynchronizer = new UserDataSynchronizer();
    private final MessageReporter messageReporter = new MessageReporter();
    private final MobileNetworkStateListener mobileNetworkStateListener;
    private final MobileMessagingStats stats;
    private final PlayServicesSupport playServicesSupport = new PlayServicesSupport();
    private final Executor taskExecutor = Executors.newSingleThreadExecutor();
    private ActivityLifecycleMonitor activityLifecycleMonitor;
    private NotificationSettings notificationSettings;
    private MessageStore messageStore;
    private Context context;
    private OnReplyClickListener replyClickListener;
    private Geofencing geofencing;

    private OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (MobileMessagingProperty.UNREPORTED_USER_DATA.getKey().equals(key) &&
                    PreferenceHelper.contains(context, MobileMessagingProperty.UNREPORTED_USER_DATA)) {
                sync();
            }
        }
    };


    protected MobileMessagingCore(Context context) {
        this.context = context;
        this.stats = new MobileMessagingStats(context);
        this.mobileNetworkStateListener = new MobileNetworkStateListener(context);
        this.activityLifecycleMonitor = null;
        this.geofencing = Geofencing.getInstance(context);

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
        registrationSynchronizer.syncronize(context, getDeviceApplicationInstanceId(), getRegistrationId(), isRegistrationIdReported(), getStats(), taskExecutor);
        deliveryReporter.report(context, getUnreportedMessageIds(), getStats(), taskExecutor);
        seenStatusReporter.report(context, getUnreportedSeenMessageIds(), getStats(), taskExecutor);
        userDataSynchronizer.sync(context, getStats(), taskExecutor);
    }

    public String getRegistrationId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.GCM_REGISTRATION_ID);
    }

    public void setRegistrationId(String registrationId) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_REGISTRATION_ID, registrationId);
        setRegistrationIdReported(false);
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

    public void setMessagesSeen(String... messageIds) {
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

    protected static void setMessageStoreClass(Context context, Class<? extends MessageStore> messageStoreClass) {
        String value = null != messageStoreClass ? messageStoreClass.getName() : null;
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, value);
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

    @SuppressWarnings({"unchecked", "WeakerAccess"})
    protected Class<? extends MessageStore> getMessageStoreClass() {
        return PreferenceHelper.findClass(context, MobileMessagingProperty.MESSAGE_STORE_CLASS);
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

        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_USER_DATA);
        PreferenceHelper.remove(context, MobileMessagingProperty.USER_DATA);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS);
    }

    public OnReplyClickListener getOnReplyClickListener() {
        return this.replyClickListener;
    }

    public void setOnReplyClickListener(OnReplyClickListener replyActionClickListener) {
        this.replyClickListener = replyActionClickListener;
    }

    static void setGeofencingActivated(Context context, boolean activated) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, activated);
    }

    public static boolean isGeofencingActivated(Context context) {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED);
    }

    void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (geofencing == null) return;
        geofencing.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void activateGeofencing() {
        if (geofencing == null) return;
        setGeofencingActivated(context, true);
        geofencing.activate();
    }

    public void deactivateGeofencing() {
        if (geofencing == null) return;
        setGeofencingActivated(context, false);
        geofencing.deactivate();
    }

    protected void setUserData(UserData userData) {

        UserData existingData = getUnreportedUserData();
        if (existingData == null) {
            existingData = getUserData();
        }

        String userId = userData != null ? userData.getExternalUserId() : null;
        String existingUserId = existingData != null ? existingData.getExternalUserId() : null;

        if (!StringUtils.isEqual(userId, existingUserId)) {
            PreferenceHelper.remove(context, MobileMessagingProperty.USER_DATA);
            existingData = null;
        }

        UserData merged = UserData.merge(existingData, userData);
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_USER_DATA, merged.toString());
    }

    protected UserData getUserData() {
        if (PreferenceHelper.contains(context, MobileMessagingProperty.USER_DATA)) {
            return new UserData(PreferenceHelper.findString(context, MobileMessagingProperty.USER_DATA));
        }
        return null;
    }

    public UserData getUnreportedUserData() {
        if (PreferenceHelper.contains(context, MobileMessagingProperty.UNREPORTED_USER_DATA)) {
            return new UserData(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_USER_DATA));
        }
        return null;
    }

    public void setUserDataReportedWithError() {
        setUserDataReported(null);
    }

    public void setUserDataReported(UserData userData) {
        if (userData != null) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.USER_DATA, userData.toString());
        }
        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_USER_DATA);
    }

    public void sendMessages(MoMessage... messages) {
        messageReporter.send(context, getStats(), taskExecutor, messages);
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

        private final Context context;
        private NotificationSettings notificationSettings = null;
        private String applicationCode = null;
        private OnReplyClickListener replyActionClickListener;
        private Geofencing geofencing;

        public Builder(Context context) {
            if (null == context) {
                throw new IllegalArgumentException("context is mandatory!");
            }
            this.context = context;
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
         * It will activate monitoring geo areas and notify when area is entered.
         *
         * @param geofencing - handles monitored geo areas
         * @return {@link Builder}
         */
        public Builder withGeofencing(Geofencing geofencing) {
            this.geofencing = geofencing;
            return this;
        }

        public Builder withOnReplyClickListener(OnReplyClickListener replyActionClickListener) {
            this.replyActionClickListener = replyActionClickListener;
            return this;
        }

        private void activateGeofencing() {
            if (geofencing != null) {
                geofencing.activate();
            }
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
            mobileMessagingCore.activityLifecycleMonitor = new ActivityLifecycleMonitor(context);
            mobileMessagingCore.playServicesSupport.checkPlayServices(context);
            mobileMessagingCore.setOnReplyClickListener(replyActionClickListener);
            activateGeofencing();
            return mobileMessagingCore;
        }
    }
}
