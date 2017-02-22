package org.infobip.mobile.messaging;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelperImpl;
import org.infobip.mobile.messaging.gcm.MobileMessagingGcmIntentService;
import org.infobip.mobile.messaging.gcm.PlayServicesSupport;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.geo.GeoSQLiteMessageStore;
import org.infobip.mobile.messaging.geo.Geofencing;
import org.infobip.mobile.messaging.mobile.data.SystemDataReporter;
import org.infobip.mobile.messaging.mobile.data.UserDataSynchronizer;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.mobile.messages.MessageSender;
import org.infobip.mobile.messaging.mobile.messages.MessagesSynchronizer;
import org.infobip.mobile.messaging.mobile.registration.RegistrationSynchronizer;
import org.infobip.mobile.messaging.mobile.seen.SeenStatusReporter;
import org.infobip.mobile.messaging.mobile.version.VersionChecker;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.telephony.MobileNetworkStateListener;
import org.infobip.mobile.messaging.util.DeviceInformation;
import org.infobip.mobile.messaging.util.ExceptionUtils;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.StringUtils;
import org.infobip.mobile.messaging.util.SystemInformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 28.04.2016.
 */
public class MobileMessagingCore {

    private static final int MESSAGE_ID_PARAMETER_LIMIT = 100;
    private static final long MESSAGE_EXPIRY_TIME = TimeUnit.DAYS.toMillis(7);

    private static MobileMessagingCore instance;
    private static DatabaseHelper databaseHelper;
    private final RegistrationSynchronizer registrationSynchronizer = new RegistrationSynchronizer();
    private final MessagesSynchronizer messagesSynchronizer = new MessagesSynchronizer();
    private final SeenStatusReporter seenStatusReporter = new SeenStatusReporter();
    private final UserDataSynchronizer userDataSynchronizer = new UserDataSynchronizer();
    private final MessageSender messageSender = new MessageSender();
    private final SystemDataReporter systemDataReporter = new SystemDataReporter();
    private final VersionChecker versionChecker = new VersionChecker();
    private final GeoReporter geoReporter = new GeoReporter();
    private final MobileMessagingStats stats;
    private final Executor registrationAlignedExecutor = Executors.newSingleThreadExecutor();
    private ActivityLifecycleMonitor activityLifecycleMonitor;
    private MobileNetworkStateListener mobileNetworkStateListener;
    private PlayServicesSupport playServicesSupport;
    private NotificationSettings notificationSettings;
    private MessageStore messageStore;
    private GeoSQLiteMessageStore internalStoreForGeo;
    private Context context;
    private Geofencing geofencing;

    protected MobileMessagingCore(Context context) {
        this.context = context;
        this.stats = new MobileMessagingStats(context);
    }

    /**
     * Gets an instance of MobileMessagingCore after it is initialized via {@link MobileMessagingCore.Builder}.
     * </p>
     * If the app was killed and there is no instance available, it will return a temporary instance based on current context.
     * Only the Builder can set static instance, because there it is initialized from Application object.
     * It is needed in order to not hold possible references to Activity(Context) and to avoid memory leaks.
     *
     * @param context android context object.
     * @return instance of MobileMessagingCore.
     * @see MobileMessagingCore.Builder
     */
    public static MobileMessagingCore getInstance(Context context) {
        if (null != instance) {
            return instance;
        }

        return new MobileMessagingCore(context);
    }

    public static DatabaseHelper getDatabaseHelper(Context context) {
        if (null == databaseHelper) {
            databaseHelper = new DatabaseHelperImpl(context);
        }
        return databaseHelper;
    }

    public void sync() {

        registrationSynchronizer.synchronize(context, getDeviceApplicationInstanceId(), getRegistrationId(), isRegistrationIdReported(), getStats(), registrationAlignedExecutor);
        userDataSynchronizer.sync(context, getStats(), registrationAlignedExecutor, null);
        versionChecker.check(context);

        reportSystemData();

        seenStatusReporter.report(context, getUnreportedSeenMessageIds(), getStats(), registrationAlignedExecutor);

        if (isPushRegistrationEnabled()) {
            messagesSynchronizer.synchronize(context, getStats(), registrationAlignedExecutor);
            geoReporter.report(context, getStats());
        }
    }

    void enablePushRegistration() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, true);
        registrationSynchronizer.updatePushRegistrationStatus(context, getRegistrationId(), true, getStats(), registrationAlignedExecutor);
        if (isGeofencingActivated(context)) {
            Geofencing.getInstance(context).activate();
        }
    }

    void disablePushRegistration() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, false);
        registrationSynchronizer.updatePushRegistrationStatus(context, getRegistrationId(), false, getStats(), registrationAlignedExecutor);
        if (isGeofencingActivated(context)) {
            Geofencing.getInstance(context).deactivate();
        }
    }

    public boolean isPushRegistrationEnabled() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED);
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

    private void addUnreportedMessageIds(String... messageIDs) {
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, messageIDs);
    }

    public void addSyncMessagesIds(String... messageIDs) {
        String[] timestampMessageIdPair = concatTimestampToMessageId(messageIDs);
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS, timestampMessageIdPair);
    }

    public String[] getSyncMessagesIds() {
        String[] messageIds = PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS);
        ArrayList<String> messageIdsArrayList = new ArrayList<>(Arrays.asList(messageIds));
        String[] messageIdsToSync = new String[messageIdsArrayList.size() <= MESSAGE_ID_PARAMETER_LIMIT ? messageIdsArrayList.size() : MESSAGE_ID_PARAMETER_LIMIT];

        boolean shouldUpdateMessageIds = false;

        for (int i = 0; i < messageIdsArrayList.size(); i++) {
            String syncMessage = messageIdsArrayList.get(i);
            String[] messageIdWithTimestamp = syncMessage.split(StringUtils.COMMA_WITH_SPACE);

            String strTimeMessageReceived = messageIdWithTimestamp[1];

            long timeMessageReceived = Long.valueOf(strTimeMessageReceived);
            long timeInterval = System.currentTimeMillis() - timeMessageReceived;

            if (timeInterval > MESSAGE_EXPIRY_TIME || i >= MESSAGE_ID_PARAMETER_LIMIT) {
                messageIdsArrayList.remove(i);
                shouldUpdateMessageIds = true;
            } else {
                messageIdsToSync[i] = messageIdWithTimestamp[0];
            }
        }

        if (shouldUpdateMessageIds) {
            String[] messageIdsToUpdate = new String[messageIdsArrayList.size()];
            messageIdsToUpdate = messageIdsArrayList.toArray(messageIdsToUpdate);
            PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS);
            PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS, messageIdsToUpdate);
        }

        return messageIdsToSync;
    }

    public void removeUnreportedMessageIds(final String... messageIDs) {
        PreferenceHelper.deleteFromStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, messageIDs);
    }

    public String[] getUnreportedSeenMessageIds() {
        return PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS);
    }

    private void addUnreportedSeenMessageIds(final String... messageIDs) {
        String[] seenMessages = concatTimestampToMessageId(messageIDs);
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS, seenMessages);
    }

    private String[] concatTimestampToMessageId(String[] messageIDs) {
        String[] syncMessages = new String[messageIDs.length];

        if (messageIDs.length > 0) {
            for (int i = 0; i < messageIDs.length; i++) {
                String messageId = messageIDs[i];
                String seenTimestamp = String.valueOf(System.currentTimeMillis());
                syncMessages[i] = StringUtils.concat(messageId, seenTimestamp, StringUtils.COMMA_WITH_SPACE);
            }
        }

        return syncMessages;
    }

    public void removeUnreportedSeenMessageIds(final String... messageIDs) {
        PreferenceHelper.deleteFromStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS, messageIDs);
    }

    public void setMessagesDelivered(String... messageIds) {
        addUnreportedMessageIds(messageIds);
        addSyncMessagesIds(messageIds);
        sync();
    }

    void setMessagesSeen(String... messageIds) {
        addUnreportedSeenMessageIds(messageIds);
        updateStoredMessagesWithSeenStatus(messageIds);
        sync();
    }

    private void updateStoredMessagesWithSeenStatus(String[] messageIds) {
        if (!isMessageStoreEnabled()) {
            return;
        }

        MessageStore messageStore = getMessageStore();
        List<String> messageIdList = Arrays.asList(messageIds);
        for (Message m : new ArrayList<>(messageStore.findAll(context))) {
            if (messageIdList.contains(m.getMessageId())) {
                m.setSeenTimestamp(System.currentTimeMillis());
                messageStore.save(context, m);
            }
        }
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

    static void setGcmSenderId(Context context, String gcmSenderId) {
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

    static void setMessageStoreClass(Context context, Class<? extends MessageStore> messageStoreClass) {
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

    public MessageStore getMessageStoreForGeo() {
        if (internalStoreForGeo == null) {
            internalStoreForGeo = new GeoSQLiteMessageStore();
        }
        return internalStoreForGeo;
    }

    public MessageStore getMessageStoreForMessage(Message message) {
        if (hasGeo(message)) {
            return getMessageStoreForGeo();
        }
        return getMessageStore();
    }

    public static boolean hasGeo(Message message) {
        return message != null && message.getGeo() != null &&
                message.getGeo().getAreasList() != null &&
                !message.getGeo().getAreasList().isEmpty();
    }

    @SuppressWarnings({"unchecked", "WeakerAccess"})
    protected Class<? extends MessageStore> getMessageStoreClass() {
        return PreferenceHelper.findClass(context, MobileMessagingProperty.MESSAGE_STORE_CLASS);
    }

    public boolean isMessageStoreEnabled() {
        return null != getMessageStoreClass();
    }

    public MobileMessagingStats getStats() {
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

    static void setApiUri(Context context, String apiUri) {
        if (StringUtils.isBlank(apiUri)) {
            throw new IllegalArgumentException("apiUri is mandatory! If in doubt, use " + MobileMessagingProperty.API_URI.getDefaultValue());
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, apiUri);
    }

    public String getApiUri() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.API_URI);
    }

    static void setReportCarrierInfo(Context context, boolean reportCarrierInfo) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_CARRIER_INFO, reportCarrierInfo);
    }

    static void setReportSystemInfo(Context context, boolean reportSystemInfo) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, reportSystemInfo);
    }

    static void setDoMarkSeenOnNotificationTap(Context context, boolean doMarkSeenOnNotificationTap) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.MARK_SEEN_ON_NOTIFICATION_TAP, doMarkSeenOnNotificationTap);
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
        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);
    }

    public static void setGeofencingActivated(Context context, boolean activated) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, activated);
    }

    public static boolean isGeofencingActivated(Context context) {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED);
    }

    public void activateGeofencing() {
        activateGeofencing(geofencing);
    }

    void activateGeofencing(Geofencing geofencing) {
        this.geofencing = geofencing;
        setGeofencingActivated(context, geofencing != null);
        if (geofencing == null) return;
        geofencing.activate();
    }

    void deactivateGeofencing() {
        if (geofencing == null) return;
        setGeofencingActivated(context, false);
        geofencing.deactivate();
        this.geofencing = null;
    }

    void syncUserData(UserData userData, MobileMessaging.ResultListener<UserData> listener) {

        UserData userDataToReport = new UserData();
        if (userData != null) {
            UserData existingData = getUnreportedUserData();
            String userId = userData.getExternalUserId();
            String existingUserId = existingData != null ? existingData.getExternalUserId() : null;

            if (!StringUtils.isEqual(userId, existingUserId)) {
                PreferenceHelper.remove(context, MobileMessagingProperty.USER_DATA);
                existingData = null;
            }

            userDataToReport = UserData.merge(existingData, userData);
        } else {
            UserData existingUserData = getUserData();
            String externalUserId = existingUserData != null ? existingUserData.getExternalUserId() : null;
            if (externalUserId != null) {
                userDataToReport.setExternalUserId(externalUserId);
            }
        }

        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_USER_DATA);
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_USER_DATA, userDataToReport.toString());

        userDataSynchronizer.sync(context, getStats(), registrationAlignedExecutor, listener);
    }

    public UserData getUserData() {
        UserData existing = null;
        if (PreferenceHelper.contains(context, MobileMessagingProperty.USER_DATA)) {
            existing = new UserData(PreferenceHelper.findString(context, MobileMessagingProperty.USER_DATA));
        }

        return UserData.merge(existing, getUnreportedUserData());
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

    void sendMessages(MobileMessaging.ResultListener<Message[]> listener, Message... messages) {
        if (isMessageStoreEnabled()) {
            getMessageStore().save(context, messages);
        }
        messageSender.send(context, getStats(), registrationAlignedExecutor, listener, messages);
    }

    public void reportSystemData() {

        boolean reportEnabled = PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO);

        SystemData data = new SystemData(SoftwareInformation.getLibraryVersion(),
                reportEnabled ? SystemInformation.getAndroidSystemVersion() : "",
                reportEnabled ? DeviceInformation.getDeviceManufacturer() : "",
                reportEnabled ? DeviceInformation.getDeviceModel() : "",
                reportEnabled ? SoftwareInformation.getAppVersion(context) : "",
                isGeofencingActivated(context),
                SoftwareInformation.areNotificationsEnabled(context));

        Integer hash = PreferenceHelper.findInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);
        if (hash != data.hashCode()) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA, data.toString());
        }

        systemDataReporter.report(context, getStats(), registrationAlignedExecutor);
    }

    public SystemData getUnreportedSystemData() {
        if (PreferenceHelper.contains(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA)) {
            return SystemData.fromJson(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA));
        }
        return null;
    }

    public void setSystemDataReported() {
        SystemData systemData = getUnreportedSystemData();
        if (systemData == null) {
            return;
        }

        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA);
        PreferenceHelper.saveInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH, systemData.hashCode());
    }

    public ArrayList<GeoReport> removeUnreportedGeoEvents(final Context context) {
        return PreferenceHelper.runTransaction(new PreferenceHelper.Transaction<ArrayList<GeoReport>>() {
            @Override
            public ArrayList<GeoReport> run() {
                JsonSerializer deserializer = new JsonSerializer();
                String unreportedGeoEventsJsons[] = PreferenceHelper.findStringArray(context, MobileMessagingProperty.UNREPORTED_GEO_EVENTS);
                ArrayList<GeoReport> reports = new ArrayList<>();
                for (String unreportedGeoEventJson : unreportedGeoEventsJsons) {
                    reports.add(deserializer.deserialize(unreportedGeoEventJson, GeoReport.class));
                }
                PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_GEO_EVENTS);
                return reports;
            }
        });
    }

    public void addUnreportedGeoEvents(final List<GeoReport> reports) {
        PreferenceHelper.runTransaction(new PreferenceHelper.Transaction<Void>() {
            @Override
            public Void run() {
                JsonSerializer serializer = new JsonSerializer();
                for (GeoReport report : reports) {
                    PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.UNREPORTED_GEO_EVENTS, serializer.serialize(report));
                }
                return null;
            }
        });
    }

    public void addCampaignStatus(final String[] finishedCampaignIds, final String[] suspendedCampaignIds) {
        PreferenceHelper.runTransaction(new PreferenceHelper.Transaction<Void>() {
            @Override
            public Void run() {
                if (finishedCampaignIds != null && finishedCampaignIds.length != 0) {
                    PreferenceHelper.saveStringArray(context, MobileMessagingProperty.FINISHED_CAMPAIGN_IDS, finishedCampaignIds);
                }
                if (suspendedCampaignIds != null && suspendedCampaignIds.length != 0) {
                    PreferenceHelper.saveStringArray(context, MobileMessagingProperty.SUSPENDED_CAMPAIGN_IDS, suspendedCampaignIds);
                }
                return null;
            }
        });
    }

    public String[] getFinishedCampaignIds() {
        return PreferenceHelper.findStringArray(context, MobileMessagingProperty.FINISHED_CAMPAIGN_IDS);
    }

    public String[] getSuspendedCampaignIds() {
        return PreferenceHelper.findStringArray(context, MobileMessagingProperty.SUSPENDED_CAMPAIGN_IDS);
    }

    static void handleBootCompleted(Context context) {
        Geofencing.scheduleRefresh(context);
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

        private final Application application;
        private NotificationSettings notificationSettings = null;
        private String applicationCode = null;
        private Geofencing geofencing;

        public Builder(Application application) {
            if (null == application) {
                throw new IllegalArgumentException("application is mandatory!");
            }
            this.application = application;
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

        /**
         * Builds the <i>MobileMessagingCore</i> configuration. Registration token sync is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @return {@link MobileMessagingCore}
         */
        public MobileMessagingCore build() {
            if (!applicationCode.equals(MobileMessagingCore.getApplicationCode(application.getApplicationContext()))) {
                MobileMessagingCore.cleanup(application);
            }

            MobileMessagingCore mobileMessagingCore = new MobileMessagingCore(application);
            MobileMessagingCore.instance = mobileMessagingCore;
            mobileMessagingCore.setNotificationSettings(notificationSettings);
            mobileMessagingCore.setApplicationCode(applicationCode);
            mobileMessagingCore.activityLifecycleMonitor = new ActivityLifecycleMonitor(application.getApplicationContext());
            mobileMessagingCore.mobileNetworkStateListener = new MobileNetworkStateListener(application);
            mobileMessagingCore.playServicesSupport = new PlayServicesSupport();
            mobileMessagingCore.playServicesSupport.checkPlayServices(application.getApplicationContext());
            mobileMessagingCore.activateGeofencing(geofencing);
            return mobileMessagingCore;
        }
    }
}
