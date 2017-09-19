package org.infobip.mobile.messaging;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelperImpl;
import org.infobip.mobile.messaging.dal.sqlite.SqliteDatabaseProvider;
import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.gcm.MobileMessagingGcmIntentService;
import org.infobip.mobile.messaging.gcm.PlayServicesSupport;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.common.DefaultRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.data.SystemDataReporter;
import org.infobip.mobile.messaging.mobile.data.UserDataReporter;
import org.infobip.mobile.messaging.mobile.messages.MessagesSynchronizer;
import org.infobip.mobile.messaging.mobile.messages.MoMessageSender;
import org.infobip.mobile.messaging.mobile.registration.RegistrationSynchronizer;
import org.infobip.mobile.messaging.mobile.seen.SeenStatusReporter;
import org.infobip.mobile.messaging.mobile.version.VersionChecker;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.notification.NotificationHandlerImpl;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.MobileMessagingJobService;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.MessageStoreWrapper;
import org.infobip.mobile.messaging.storage.MessageStoreWrapperImpl;
import org.infobip.mobile.messaging.telephony.MobileNetworkStateListener;
import org.infobip.mobile.messaging.util.ComponentUtil;
import org.infobip.mobile.messaging.util.DeviceInformation;
import org.infobip.mobile.messaging.util.ExceptionUtils;
import org.infobip.mobile.messaging.util.MobileNetworkInformation;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.StringUtils;
import org.infobip.mobile.messaging.util.SystemInformation;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 28.04.2016.
 */
public class MobileMessagingCore extends MobileMessaging {

    private static final int MESSAGE_ID_PARAMETER_LIMIT = 100;
    private static final long MESSAGE_EXPIRY_TIME = TimeUnit.DAYS.toMillis(7);
    private static final String INTERACTIVE_NOTIFICATION_HANDLER_CLASSNAME = "org.infobip.mobile.messaging.interactive.notification.NotificationInteractiveHandlerImpl";

    protected static MobileMessagingCore instance;
    protected static MobileApiResourceProvider mobileApiResourceProvider;
    static String applicationCode;
    static ApplicationCodeProvider applicationCodeProvider;
    private static DatabaseHelper databaseHelper;
    private static NotificationHandler notificationHandler;
    private final MobileMessagingStats stats;
    private final ExecutorService registrationAlignedExecutor;
    private final MRetryPolicy defaultRetryPolicy;
    private final Broadcaster broadcaster;
    private RegistrationSynchronizer registrationSynchronizer;
    private MessagesSynchronizer messagesSynchronizer;
    private UserDataReporter userDataReporter;
    private SystemDataReporter systemDataReporter;
    private MoMessageSender moMessageSender;
    private SeenStatusReporter seenStatusReporter;
    private VersionChecker versionChecker;
    private ActivityLifecycleMonitor activityLifecycleMonitor;
    private MobileNetworkStateListener mobileNetworkStateListener;
    private PlayServicesSupport playServicesSupport;
    private NotificationSettings notificationSettings;
    private MessageStore messageStore;
    private MessageStoreWrapper messageStoreWrapper;
    private Context context;
    private volatile boolean didSyncAtLeastOnce;

    protected MobileMessagingCore(Context context) {
        this(context, new AndroidBroadcaster(context), Executors.newSingleThreadExecutor());
    }

    protected MobileMessagingCore(Context context, Broadcaster broadcaster, ExecutorService registrationAlignedExecutor) {
        MobileMessagingLogger.init(context);

        this.context = context;
        this.broadcaster = broadcaster;
        this.registrationAlignedExecutor = registrationAlignedExecutor;
        this.stats = new MobileMessagingStats(context);
        this.defaultRetryPolicy = DefaultRetryPolicy.create(context);
        MobileMessagingSynchronizationReceiver mobileMessagingSynchronizationReceiver = new MobileMessagingSynchronizationReceiver();

        LocalBroadcastManager.getInstance(context).registerReceiver(mobileMessagingSynchronizationReceiver,
                new IntentFilter(LocalEvent.APPLICATION_FOREGROUND.getKey()));

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            ComponentUtil.setState(context, true, MobileMessagingConnectivityReceiver.class);
        } else {
            ComponentUtil.setState(context, true, MobileMessagingJobService.class);
        }
    }

    /**
     * Gets an instance of MobileMessagingCore after it is initialized via {@link MobileMessagingCore.Builder}.
     * </p>
     * {@link MobileMessagingCore} is initialized here from application context to minimize possibility of memory leaks.
     *
     * @param context android context object.
     * @return instance of MobileMessagingCore.
     * @see MobileMessagingCore.Builder
     */
    public static MobileMessagingCore getInstance(Context context) {
        if (instance == null) {
            instance = new MobileMessagingCore(context.getApplicationContext());
        }
        return instance;
    }

    public static DatabaseHelper getDatabaseHelper(Context context) {
        if (null == databaseHelper) {
            databaseHelper = new DatabaseHelperImpl(context.getApplicationContext());
        }
        return databaseHelper;
    }

    public static SqliteDatabaseProvider getDatabaseProvider(Context context) {
        return (SqliteDatabaseProvider) getDatabaseHelper(context);
    }

    public void sync() {
        didSyncAtLeastOnce = true;
        if (!MobileNetworkInformation.isNetworkAvailableSafely(context)) {
            registerForNetworkAvailability();
            return;
        }

        registrationSynchronizer().sync();
        messagesSynchronizer().sync();
        moMessageSender().sync();
        seenStatusReporter().sync();
        userDataReporter().sync(null, getUnreportedUserData());
        versionChecker().sync();

        reportSystemData();
    }

    public void retrySync() {
        if (!didSyncAtLeastOnce) {
            return;
        }

        messagesSynchronizer().sync();
        moMessageSender().sync();
        seenStatusReporter().sync();
        userDataReporter().sync(null, getUnreportedUserData());
        reportSystemData();
    }

    public void registerForNetworkAvailability() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MobileMessagingJobService.registerJobForConnectivityUpdates(context);
        }
    }

    public static NotificationHandler resolveNotificationHandler(Context context) {
        if (null != notificationHandler) {
            return notificationHandler;
        }

        return setNotificationHandler(context);
    }

    private static NotificationHandler setNotificationHandler(Context context) {

        try {
            Class<?> c = Class.forName(INTERACTIVE_NOTIFICATION_HANDLER_CLASSNAME);
            Constructor<?> constructor = c.getConstructor(Context.class);
            Object resolvedInteractiveNotificationHandler = constructor.newInstance(context);
            notificationHandler = (NotificationHandler) resolvedInteractiveNotificationHandler;

        } catch (Exception e) {
            notificationHandler = new NotificationHandlerImpl(context.getApplicationContext());
        }

        return notificationHandler;
    }

    @Override
    public void enablePushRegistration() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, true);
        registrationSynchronizer().updateStatus(true);
    }

    @Override
    public void disablePushRegistration() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, false);
        registrationSynchronizer().updateStatus(false);
    }

    @Override
    public String getPushRegistrationId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);
    }

    public boolean isPushRegistrationEnabled() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED);
    }

    public String getCloudToken() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.GCM_REGISTRATION_ID);
    }

    public void setRegistrationId(String registrationId) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_REGISTRATION_ID, registrationId);
        setRegistrationIdReported(false);
    }

    public String[] getAndRemoveUnreportedMessageIds() {
        return PreferenceHelper.findAndRemoveStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS);
    }

    public void addUnreportedMessageIds(String... messageIDs) {
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, messageIDs);
    }

    public void addSyncMessagesIds(String... messageIDs) {
        String[] timestampMessageIdPair = concatTimestampToMessageId(messageIDs);
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS, timestampMessageIdPair);
    }

    public String[] getSyncMessagesIds() {
        String[] messageIds = PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS);
        ArrayList<String> messageIdsArrayList = new ArrayList<>(Arrays.asList(messageIds));
        Set<String> messageIdsToSync = new HashSet<>(messageIdsArrayList.size() <= MESSAGE_ID_PARAMETER_LIMIT ? messageIdsArrayList.size() : MESSAGE_ID_PARAMETER_LIMIT);
        boolean shouldUpdateMessageIds = false;

        for (int i = 0; i < messageIdsArrayList.size(); i++) {
            String syncMessage = messageIdsArrayList.get(i);
            String[] messageIdWithTimestamp = syncMessage.split(StringUtils.COMMA_WITH_SPACE);

            String strTimeMessageReceived = messageIdWithTimestamp[1];

            long timeMessageReceived = Long.valueOf(strTimeMessageReceived);
            long timeInterval = Time.now() - timeMessageReceived;

            if (timeInterval > MESSAGE_EXPIRY_TIME || i >= MESSAGE_ID_PARAMETER_LIMIT) {
                messageIdsArrayList.remove(i);
                shouldUpdateMessageIds = true;
            } else {
                messageIdsToSync.add(messageIdWithTimestamp[0]);
            }
        }

        if (shouldUpdateMessageIds) {
            String[] messageIdsToUpdate = new String[messageIdsArrayList.size()];
            messageIdsToUpdate = messageIdsArrayList.toArray(messageIdsToUpdate);
            PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS);
            PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS, messageIdsToUpdate);
        }

        return messageIdsToSync.toArray(new String[messageIdsToSync.size()]);
    }

    public String[] getUnreportedSeenMessageIds() {
        String[] ids = PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS);
        return filterOutGeneratedMessageIds(ids);
    }

    /**
     * Method to update unreported seen ids
     *
     * @param messageIdMap map that contains old id as key and new id as value
     */
    public void updateUnreportedSeenMessageIds(final Map<String, String> messageIdMap) {
        if (messageIdMap == null || messageIdMap.isEmpty()) {
            return;
        }

        PreferenceHelper.runTransaction(new PreferenceHelper.Transaction<Void>() {
            @Override
            public Void run() {
                String[] reports = PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS);
                if (reports.length == 0) {
                    return null;
                }

                for (int i = 0; i < reports.length; i++) {
                    String messageIdAndTimestamp[] = reports[i].split(StringUtils.COMMA_WITH_SPACE);
                    String newMessageId = messageIdMap.get(messageIdAndTimestamp[0]);
                    if (newMessageId != null) {
                        reports[i] = StringUtils.concat(newMessageId, messageIdAndTimestamp[1], StringUtils.COMMA_WITH_SPACE);
                    }
                }

                PreferenceHelper.saveStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS, reports);
                return null;
            }
        });
    }

    public void updatedGeneratedMessageIDs(final Map<String, String> messageIdMap) {
        if (messageIdMap == null || messageIdMap.isEmpty()) {
            return;
        }

        PreferenceHelper.runTransaction(new PreferenceHelper.Transaction<Void>() {
            @Override
            public Void run() {
                String[] generatedMessageIds = getGeneratedMessageIds();
                for (String messageId : generatedMessageIds) {
                    if (messageIdMap.get(messageId) != null) {
                        removeGeneratedMessageIds(messageId);
                    }
                }
                return null;
            }
        });
    }

    public void addGeneratedMessageIds(final String... messageIDs) {
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_GENERATED_MESSAGE_IDS, messageIDs);
    }

    private String[] getGeneratedMessageIds() {
        return PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_GENERATED_MESSAGE_IDS);
    }

    private void removeGeneratedMessageIds(final String... messageIDs) {
        PreferenceHelper.deleteFromStringArray(context, MobileMessagingProperty.INFOBIP_GENERATED_MESSAGE_IDS, messageIDs);
    }

    private void addUnreportedSeenMessageIds(final String... messageIDs) {
        String[] seenMessages = concatTimestampToMessageId(messageIDs);
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS, seenMessages);
    }

    private String[] concatTimestampToMessageId(String[] messageIDs) {
        List<String> syncMessages = new ArrayList<>(messageIDs.length);
        if (messageIDs.length > 0) {
            for (String messageId : messageIDs) {
                if (messageId == null) {
                    continue;
                }
                String seenTimestamp = String.valueOf(Time.now());
                syncMessages.add(StringUtils.concat(messageId, seenTimestamp, StringUtils.COMMA_WITH_SPACE));
            }
        }

        return syncMessages.toArray(new String[syncMessages.size()]);
    }

    private String[] filterOutGeneratedMessageIds(String[] messageIDs) {
        String generatedMessageIDs[] = getGeneratedMessageIds();
        if (generatedMessageIDs.length == 0) {
            return messageIDs;
        }

        List<String> seenIds = getSeenMessageIdsFromReports(messageIDs);
        List<String> filteredSeenReports = new ArrayList<>(Arrays.asList(messageIDs));
        for (String generatedMessageId : generatedMessageIDs) {
            int idIndex = seenIds.indexOf(generatedMessageId);
            if (idIndex >= 0) {
                filteredSeenReports.remove(idIndex);
                seenIds.remove(idIndex);
            }
        }
        return filteredSeenReports.toArray(new String[filteredSeenReports.size()]);
    }

    /**
     * Returns list of messageId and seenTimestamp
     *
     * @param reports concatenated message id and timestamp
     * @return reports
     */
    private List<String> getSeenMessageIdsFromReports(String[] reports) {
        List<String> ids = new ArrayList<>();
        for (String report : reports) {
            ids.add(getSeenMessageIdFromReport(report));
        }
        return ids;
    }

    /**
     * Returns message id from seen report string
     *
     * @param report concatenated message id and timestamp
     * @return message id
     */
    private String getSeenMessageIdFromReport(String report) {
        String[] reportContents = report.split(StringUtils.COMMA_WITH_SPACE);
        return reportContents.length > 0 ? reportContents[0] : null;
    }

    public void removeUnreportedSeenMessageIds(final String... messageIDs) {
        PreferenceHelper.deleteFromStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS, messageIDs);
    }

    public void setMessagesDelivered(String... messageIds) {
        addUnreportedMessageIds(messageIds);
        addSyncMessagesIds(messageIds);
        sync();
    }

    public void setMessagesSeen(String... messageIds) {
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
                m.setSeenTimestamp(Time.now());
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

    public static String getGcmSenderId(Context context) {
        return PreferenceHelper.findString(context, MobileMessagingProperty.GCM_SENDER_ID);
    }

    public boolean isRegistrationIdReported() {
        return registrationSynchronizer().isRegistrationIdReported();
    }

    private void setRegistrationIdReported(boolean registrationIdReported) {
        registrationSynchronizer().setRegistrationIdReported(registrationIdReported);
    }

    public static void setMessageStoreClass(Context context, Class<? extends MessageStore> messageStoreClass) {
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

    public MessageStoreWrapper getMessageStoreWrapper() {
        if (messageStoreWrapper == null) {
            messageStoreWrapper = new MessageStoreWrapperImpl(context, getMessageStore());
        }
        return messageStoreWrapper;
    }

    public MobileMessagingStats getStats() {
        return stats;
    }

    public void setLastHttpException(Throwable lastHttpException) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.LAST_HTTP_EXCEPTION, ExceptionUtils.stacktrace(lastHttpException));
    }

    @SuppressWarnings("unused")
    public String getLastHttpException() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.LAST_HTTP_EXCEPTION);
    }

    private void setApplicationCode(String applicationCode) {
        if (shouldSaveApplicationCode(context)) {
            if (StringUtils.isBlank(applicationCode)) {
                throw new IllegalArgumentException("applicationCode is mandatory! Get one here: https://portal.infobip.com/push/applications");
            }
            PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, applicationCode);
            return;
        }

        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "");
    }

    public static String getApplicationCode(Context context) {

        if (shouldSaveApplicationCode(context)) {
            applicationCode = PreferenceHelper.findString(context, MobileMessagingProperty.APPLICATION_CODE);
            return applicationCode;
        }

        if (applicationCode != null) {
            return applicationCode;
        }

        if (applicationCodeProvider != null) {
            applicationCode = applicationCodeProvider.resolve();
            return applicationCode;
        }

        String appCodeProviderCanonicalClassName = getApplicationCodeProviderClassName(context);

        try {
            Class<?> c = Class.forName(appCodeProviderCanonicalClassName);
            Object applicationCodeProvider = c.newInstance();
            Method resolve = ApplicationCodeProvider.class.getMethod("resolve");
            applicationCode = String.valueOf(resolve.invoke(applicationCodeProvider));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return applicationCode;
    }

    public String getApplicationCode() {
        return getApplicationCode(context);
    }

    static void setApiUri(Context context, String apiUri) {
        if (StringUtils.isBlank(apiUri)) {
            throw new IllegalArgumentException("apiUri is mandatory! If in doubt, use " + MobileMessagingProperty.API_URI.getDefaultValue());
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, apiUri);
    }

    public static String getApiUri(Context context) {
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

    public static void setShouldSaveUserData(Context context, boolean shouldSaveUserData) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.SAVE_USER_DATA_ON_DISK, shouldSaveUserData);
    }

    public boolean shouldSaveUserData() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.SAVE_USER_DATA_ON_DISK.getKey(), true);
    }

    public static void setShouldSaveAppCode(Context context, boolean shouldSaveAppCode) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.SAVE_APP_CODE_ON_DISK, shouldSaveAppCode);
    }

    public static boolean shouldSaveApplicationCode(Context context) {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.SAVE_APP_CODE_ON_DISK.getKey(), true);
    }

    @Override
    public void cleanup() {
        MobileMessagingCore.cleanup(context);
    }

    private static void cleanup(Context context) {
        applicationCode = null;
        resetMobileApi();

        String gcmSenderID = PreferenceHelper.findString(context, MobileMessagingProperty.GCM_SENDER_ID);

        Intent intent = new Intent(MobileMessagingGcmIntentService.ACTION_TOKEN_CLEANUP, null, context, MobileMessagingGcmIntentService.class);
        intent.putExtra(MobileMessagingGcmIntentService.EXTRA_GCM_SENDER_ID, gcmSenderID);
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

    public static void resetMobileApi() {
        mobileApiResourceProvider = null;
    }

    @Override
    public void syncUserData(UserData userData) {
        syncUserData(userData, null);
    }

    @Override
    public void syncUserData(UserData userData, MobileMessaging.ResultListener<UserData> listener) {

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

        userDataReporter().sync(listener, userDataToReport);
    }

    @Override
    public void fetchUserData() {
        fetchUserData(null);
    }

    @Override
    public void fetchUserData(MobileMessaging.ResultListener<UserData> listener) {
        syncUserData(null, listener);
    }

    @Nullable
    public UserData getUserData() {
        UserData existing = null;
        if (PreferenceHelper.contains(context, MobileMessagingProperty.USER_DATA)) {
            existing = new UserData(PreferenceHelper.findString(context, MobileMessagingProperty.USER_DATA));
        }

        return UserData.merge(existing, getUnreportedUserData());
    }

    @Nullable
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
        if (userData != null && shouldSaveUserData()) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.USER_DATA, userData.toString());
        }
        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_USER_DATA);
    }

    @Override
    public void sendMessages(Message... messages) {
        sendMessages(null, messages);
    }

    @Override
    public void sendMessages(MobileMessaging.ResultListener<Message[]> listener, Message... messages) {
        if (isMessageStoreEnabled()) {
            getMessageStore().save(context, messages);
        }
        moMessageSender().send(listener, messages);
    }

    public void sendMessagesWithRetry(Message... messages) {
        moMessageSender().sendWithRetry(messages);
    }

    public void reportSystemData() {

        boolean reportEnabled = PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO);

        SystemData data = new SystemData(SoftwareInformation.getSDKVersionWithPostfixForSystemData(context),
                reportEnabled ? SystemInformation.getAndroidSystemVersion() : "",
                reportEnabled ? DeviceInformation.getDeviceManufacturer() : "",
                reportEnabled ? DeviceInformation.getDeviceModel() : "",
                reportEnabled ? SoftwareInformation.getAppVersion(context) : "",
                isGeofencingActivated(),
                SoftwareInformation.areNotificationsEnabled(context),
                reportEnabled && DeviceInformation.isDeviceSecure(context));

        Integer hash = PreferenceHelper.findInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);
        if (hash != data.hashCode()) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA, data.toString());
        }

        systemDataReporter().synchronize();
    }

    boolean isGeofencingActivated() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED.getKey(), false);
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

    private void setApplicationCodeProviderClassName(ApplicationCodeProvider applicationCodeProvider) {
        MobileMessagingCore.applicationCodeProvider = applicationCodeProvider;
        if (applicationCodeProvider == null) return;
        PreferenceHelper.saveString(context, MobileMessagingProperty.APP_CODE_PROVIDER_CANONICAL_CLASS_NAME, applicationCodeProvider.getClass().getCanonicalName());
    }

    private static String getApplicationCodeProviderClassName(Context context) {
        return PreferenceHelper.findString(context, MobileMessagingProperty.APP_CODE_PROVIDER_CANONICAL_CLASS_NAME);
    }

    public static boolean hasGeo(Message message) {
        if (message == null || message.getInternalData() == null) {
            return false;
        }

        try {
            JSONObject geo = new JSONObject(message.getInternalData());
            return geo.getJSONArray("geo") != null && geo.getJSONArray("geo").length() > 0;
        } catch (JSONException e) {
            MobileMessagingLogger.e(e.getMessage());
            return false;
        }
    }

    public void saveUnreportedUserData(UserData userData) {
        if (shouldSaveUserData()) {
            PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_USER_DATA);
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_USER_DATA, userData.toString());
        }
    }

    @NonNull
    private MobileApiResourceProvider mobileApiResourceProvider() {
        if (mobileApiResourceProvider == null) {
            mobileApiResourceProvider = new MobileApiResourceProvider();
        }
        return mobileApiResourceProvider;
    }

    @NonNull
    private MoMessageSender moMessageSender() {
        if (moMessageSender == null) {
            moMessageSender = new MoMessageSender(context, this, broadcaster,
                    registrationAlignedExecutor, stats, defaultRetryPolicy, mobileApiResourceProvider().getMobileApiMessages(context), getMessageStoreWrapper());
        }
        return moMessageSender;
    }

    @NonNull
    private UserDataReporter userDataReporter() {
        if (userDataReporter == null) {
            userDataReporter = new UserDataReporter(this, registrationAlignedExecutor,
                    broadcaster, defaultRetryPolicy, stats, mobileApiResourceProvider().getMobileApiData(context));
        }
        return userDataReporter;
    }

    @NonNull
    private SystemDataReporter systemDataReporter() {
        if (systemDataReporter == null) {
            systemDataReporter = new SystemDataReporter(this, stats, defaultRetryPolicy, registrationAlignedExecutor,
                    broadcaster, mobileApiResourceProvider().getMobileApiData(context));
        }
        return systemDataReporter;
    }

    @NonNull
    private MessagesSynchronizer messagesSynchronizer() {
        if (messagesSynchronizer == null) {
            MobileMessageHandler mobileMessageHandler = new MobileMessageHandler(this, broadcaster, resolveNotificationHandler(context), getMessageStoreWrapper());
            messagesSynchronizer = new MessagesSynchronizer(this, stats, registrationAlignedExecutor,
                    broadcaster, defaultRetryPolicy, mobileMessageHandler, mobileApiResourceProvider().getMobileApiMessages(context));
        }
        return messagesSynchronizer;
    }

    @NonNull
    private RegistrationSynchronizer registrationSynchronizer() {
        if (registrationSynchronizer == null) {
            registrationSynchronizer = new RegistrationSynchronizer(context, this, stats,
                    registrationAlignedExecutor, broadcaster, defaultRetryPolicy, mobileApiResourceProvider().getMobileApiRegistration(context));
        }
        return registrationSynchronizer;
    }

    @NonNull
    private SeenStatusReporter seenStatusReporter() {
        if (seenStatusReporter == null) {
            seenStatusReporter = new SeenStatusReporter(this, stats, registrationAlignedExecutor, broadcaster,
                    mobileApiResourceProvider().getMobileApiMessages(context), new BatchReporter(PreferenceHelper.findLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY)));
        }
        return seenStatusReporter;
    }

    @NonNull
    VersionChecker versionChecker() {
        if (versionChecker == null) {
            versionChecker = new VersionChecker(context, this, stats, mobileApiResourceProvider().getMobileApiVersion(context));
        }
        return versionChecker;
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
        private ApplicationCodeProvider applicationCodeProvider;

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
         * When you want to take more care about privacy and don't want to store Application code in <i>infobip_application_code</i>
         * string resource nor in our persistent storage, but would like to use it only from memory. In this case, you should
         * provide it on demand. For example, you should implement <b>sync</b> API call to your server where you store required
         * Application code and provide it to {@link ApplicationCodeProvider#resolve()} method as a return type.
         * <p>
         * Sync (not async) API call is encouraged because we already handle your code in a background thread.
         *
         * @param applicationCodeProvider if you don't have application code, you should get one <a href="https://portal.infobip.com/push/applications">here</a>
         * @return {@link Builder}
         */
        public Builder withApplicationCode(ApplicationCodeProvider applicationCodeProvider) {
            validateWithParam(applicationCodeProvider);
            this.applicationCodeProvider = applicationCodeProvider;
            return this;
        }

        /**
         * Builds the <i>MobileMessagingCore</i> configuration. Registration token sync is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @return {@link MobileMessagingCore}
         */
        public MobileMessagingCore build() {
            if (shouldSaveApplicationCode(application.getApplicationContext())) {
                if (!applicationCode.equals(MobileMessagingCore.getApplicationCode(application.getApplicationContext()))) {
                    MobileMessagingCore.cleanup(application);
                }
            }

            MobileMessagingCore mobileMessagingCore = new MobileMessagingCore(application);
            MobileMessagingCore.notificationHandler = setNotificationHandler(application.getApplicationContext());
            mobileMessagingCore.setNotificationSettings(notificationSettings);
            mobileMessagingCore.setApplicationCode(applicationCode);
            mobileMessagingCore.setApplicationCodeProviderClassName(applicationCodeProvider);
            mobileMessagingCore.activityLifecycleMonitor = new ActivityLifecycleMonitor(application.getApplicationContext());
            mobileMessagingCore.mobileNetworkStateListener = new MobileNetworkStateListener(application);
            mobileMessagingCore.playServicesSupport = new PlayServicesSupport();
            mobileMessagingCore.playServicesSupport.checkPlayServices(application.getApplicationContext());
            MobileMessagingCore.instance = mobileMessagingCore;
            return mobileMessagingCore;
        }
    }
}
