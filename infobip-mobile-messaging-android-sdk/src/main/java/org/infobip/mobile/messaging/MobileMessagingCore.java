package org.infobip.mobile.messaging;

import static org.infobip.mobile.messaging.UserMapper.filterOutDeletedData;
import static org.infobip.mobile.messaging.UserMapper.toJson;
import static org.infobip.mobile.messaging.mobileapi.events.UserSessionTracker.SESSION_BOUNDS_DELIMITER;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseOptions;

import org.infobip.mobile.messaging.api.appinstance.AppInstanceAtts;
import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.api.appinstance.UserCustomEventBody;
import org.infobip.mobile.messaging.api.support.ApiErrorCode;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.app.ContextHelper;
import org.infobip.mobile.messaging.cloud.MobileMessageHandler;
import org.infobip.mobile.messaging.cloud.MobileMessagingCloudHandler;
import org.infobip.mobile.messaging.cloud.MobileMessagingCloudService;
import org.infobip.mobile.messaging.cloud.PlayServicesSupport;
import org.infobip.mobile.messaging.cloud.firebase.FirebaseAppProvider;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.PushDatabaseHelperImpl;
import org.infobip.mobile.messaging.dal.sqlite.SqliteDatabaseProvider;
import org.infobip.mobile.messaging.interactive.MobileInteractiveImpl;
import org.infobip.mobile.messaging.interactive.notification.InteractiveNotificationHandler;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.BatchReporter;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.mobileapi.appinstance.InstallationSynchronizer;
import org.infobip.mobile.messaging.mobileapi.baseurl.BaseUrlChecker;
import org.infobip.mobile.messaging.mobileapi.common.MAsyncTask;
import org.infobip.mobile.messaging.mobileapi.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.mobileapi.events.UserEventsRequestMapper;
import org.infobip.mobile.messaging.mobileapi.events.UserEventsSynchronizer;
import org.infobip.mobile.messaging.mobileapi.messages.MessagesSynchronizer;
import org.infobip.mobile.messaging.mobileapi.messages.MoMessageSender;
import org.infobip.mobile.messaging.mobileapi.seen.SeenStatusReporter;
import org.infobip.mobile.messaging.mobileapi.user.DepersonalizeActionListener;
import org.infobip.mobile.messaging.mobileapi.user.DepersonalizeServerListener;
import org.infobip.mobile.messaging.mobileapi.user.PersonalizeSynchronizer;
import org.infobip.mobile.messaging.mobileapi.user.UserDataReporter;
import org.infobip.mobile.messaging.mobileapi.version.VersionChecker;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.permissions.PostNotificationsPermissionRequester;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.MobileMessagingJobService;
import org.infobip.mobile.messaging.platform.Platform;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.MessageStoreWrapper;
import org.infobip.mobile.messaging.storage.MessageStoreWrapperImpl;
import org.infobip.mobile.messaging.telephony.MobileNetworkStateListener;
import org.infobip.mobile.messaging.util.ComponentUtil;
import org.infobip.mobile.messaging.util.Cryptor;
import org.infobip.mobile.messaging.util.CryptorImpl;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.DeviceInformation;
import org.infobip.mobile.messaging.util.MobileNetworkInformation;
import org.infobip.mobile.messaging.util.ModuleLoader;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.SHA1;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.StringUtils;
import org.infobip.mobile.messaging.util.SystemInformation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author sslavin
 * @since 28.04.2016.
 */
public class MobileMessagingCore
        extends MobileMessaging
        implements DepersonalizeServerListener {

    private static final int MESSAGE_ID_PARAMETER_LIMIT = 100;
    private static final long MESSAGE_EXPIRY_TIME = TimeUnit.DAYS.toMillis(7);
    private static final long LAZY_SYNC_THROTTLE_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(5);
    private static final long FOREGROUND_SYNC_THROTTLE_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(10);
    private static final JsonSerializer nullSerializer = new JsonSerializer(true);
    public static final String MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID = "mm_default_channel_high_priority";
    public static final String MM_DEFAULT_CHANNEL_ID = "mm_default_channel";

    protected static MobileApiResourceProvider mobileApiResourceProvider;
    static String applicationCode;
    private static Map<String, String> applicationCodeHashMap;
    static ApplicationCodeProvider applicationCodeProvider;
    private static DatabaseHelper databaseHelper;
    private static MobileMessagingSynchronizationReceiver mobileMessagingSynchronizationReceiver;
    private final MobileMessagingStats stats;
    private final ExecutorService registrationAlignedExecutor;
    private final RetryPolicyProvider retryPolicyProvider;
    private final Broadcaster broadcaster;
    private final ModuleLoader moduleLoader;
    private final NotificationHandler notificationHandler;
    private String installationId;

    private MessagesSynchronizer messagesSynchronizer;
    private UserDataReporter userDataReporter;
    private InstallationSynchronizer installationSynchronizer;
    private PersonalizeSynchronizer personalizeSynchronizer;
    private UserEventsSynchronizer userEventsSynchronizer;

    private MoMessageSender moMessageSender;
    private SeenStatusReporter seenStatusReporter;
    private VersionChecker versionChecker;
    private BaseUrlChecker baseUrlChecker;
    private ActivityLifecycleMonitor activityLifecycleMonitor;
    @SuppressWarnings("unused")
    private MobileNetworkStateListener mobileNetworkStateListener;
    private PlayServicesSupport playServicesSupport;
    private NotificationSettings notificationSettings;
    private MessageStore messageStore;
    private MessageStoreWrapper messageStoreWrapper;
    private final Context context;
    private final Map<String, MessageHandlerModule> messageHandlerModules;
    private volatile boolean didSyncAtLeastOnce;
    private volatile Long lastSyncTimeMillis;
    private volatile Long lastForegroundSyncMillis;
    private FirebaseAppProvider firebaseAppProvider;
    private PostNotificationsPermissionRequester postNotificationsPermissionRequester;

    protected MobileMessagingCore(Context context) {
        this(context, new AndroidBroadcaster(context), Executors.newSingleThreadExecutor(), new ModuleLoader(context), new FirebaseAppProvider(context));
    }

    protected MobileMessagingCore(Context context, Broadcaster broadcaster, ExecutorService registrationAlignedExecutor, ModuleLoader moduleLoader, FirebaseAppProvider firebaseAppProvider) {
        MobileMessagingLogger.init(context);

        this.context = context;
        this.broadcaster = broadcaster;
        this.registrationAlignedExecutor = registrationAlignedExecutor;
        this.stats = new MobileMessagingStats(context);
        this.retryPolicyProvider = new RetryPolicyProvider(context);
        this.moduleLoader = moduleLoader;
        this.notificationHandler = new InteractiveNotificationHandler(context);
        this.messageHandlerModules = loadMessageHandlerModules();
        this.postNotificationsPermissionRequester = new PostNotificationsPermissionRequester(PreferenceHelper.findBoolean(context, MobileMessagingProperty.POST_NOTIFICATIONS_REQUEST_ENABLED));

        if (mobileMessagingSynchronizationReceiver == null) {
            mobileMessagingSynchronizationReceiver = new MobileMessagingSynchronizationReceiver();
        }

        Application application = new ContextHelper(context).getApplication();
        if (application != null) {
            this.activityLifecycleMonitor = new ActivityLifecycleMonitor(application);
        }

        ComponentUtil.setSynchronizationReceiverStateEnabled(context, mobileMessagingSynchronizationReceiver, true);
        ComponentUtil.setConnectivityComponentsStateEnabled(context, true);

        initDefaultChannels();
        migratePrefsIfNecessary(context);

        this.installationId = getUniversalInstallationId();
        this.firebaseAppProvider = firebaseAppProvider;
    }

    /**
     * There is no need to migrate system data fields - they'll be newly fetched/synced on the first call of patch method
     */
    private void migratePrefsIfNecessary(Context context) {
        if (PreferenceHelper.shouldMigrateToPrivatePrefs(context)) {
            PreferenceHelper.migrateToPrivatePrefs(context);
        }
        if (PreferenceHelper.contains(context, MobileMessagingProperty.PERFORMED_USER_DATA_MIGRATION)) {
            return;
        }

        migrateUserData(MobileMessagingProperty.USER_DATA);
        migrateUserData(MobileMessagingProperty.UNREPORTED_USER_DATA);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PERFORMED_USER_DATA_MIGRATION, true);
    }

    private void migrateUserData(MobileMessagingProperty userDataProperty) {
        if (!PreferenceHelper.contains(context, userDataProperty)) {
            return;
        }

        String unreportedUserData = PreferenceHelper.findString(context, userDataProperty);
        if (unreportedUserData == null) {
            return;
        }

        Pair<User, Map<String, CustomAttributeValue>> userDataWithCustomAtts = UserMapper.migrateToNewModels(unreportedUserData);
        if (userDataWithCustomAtts.first != null) {
            if (userDataProperty == MobileMessagingProperty.UNREPORTED_USER_DATA) {
                saveUnreportedUserData(userDataWithCustomAtts.first);
            } else if (userDataProperty == MobileMessagingProperty.USER_DATA) {
                storeAndMergeUnreportedUserLocally(userDataWithCustomAtts.first);
            }
        }

        Map<String, CustomAttributeValue> customAtts = userDataWithCustomAtts.second;
        if (customAtts != null) {
            saveCustomAttributes(customAtts);
            if (userDataProperty == MobileMessagingProperty.UNREPORTED_USER_DATA) {
                setUnreportedCustomAttributes(customAtts);
            } else if (userDataProperty == MobileMessagingProperty.USER_DATA) {
                saveCustomAttributes(customAtts);
            }
        }
    }

    private void initDefaultChannels() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        CharSequence channelName = SoftwareInformation.getAppName(context);

        NotificationChannel notificationChannel = new NotificationChannel(MM_DEFAULT_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationManager.createNotificationChannel(notificationChannel);

        NotificationSettings notificationSettings = getNotificationSettings();
        if (notificationSettings != null && notificationSettings.areHeadsUpNotificationsEnabled()) {
            NotificationChannel highPriorityNotificationChannel = new NotificationChannel(MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID, channelName + " High Priority", NotificationManager.IMPORTANCE_HIGH);
            highPriorityNotificationChannel.enableLights(true);
            highPriorityNotificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(highPriorityNotificationChannel);
        }
    }

    /**
     * Gets an instance of MobileMessagingCore after it is initialized via {@link MobileMessagingCore.Builder}.
     * <br>
     * {@link MobileMessagingCore} is initialized here from application context to minimize possibility of memory leaks.
     *
     * @param context android context object.
     * @return instance of MobileMessagingCore.
     * @see MobileMessagingCore.Builder
     */
    public static MobileMessagingCore getInstance(Context context) {
        return Platform.mobileMessagingCore.get(context);
    }

    public Collection<MessageHandlerModule> getMessageHandlerModules() {
        return messageHandlerModules.values();
    }

    public <T extends MessageHandlerModule> T getMessageHandlerModule(Class<? extends MessageHandlerModule> cls) {
        if (messageHandlerModules.containsKey(cls.getName())) {
            //noinspection unchecked
            return (T) messageHandlerModules.get(cls.getName());
        }

        //noinspection unchecked
        T module = (T) moduleLoader.createModule(cls);
        if (module != null) {
            module.init(context);
            messageHandlerModules.put(cls.getName(), module);
        }
        return module;
    }

    private Map<String, MessageHandlerModule> loadMessageHandlerModules() {
        Map<String, MessageHandlerModule> modules = moduleLoader.loadModulesFromManifest(MessageHandlerModule.class);
        modules.put(MobileInteractiveImpl.class.getName(), new MobileInteractiveImpl());
        for (MessageHandlerModule module : modules.values()) {
            module.init(context);
        }
        return modules;
    }

    public static DatabaseHelper getDatabaseHelper(Context context) {
        if (null == databaseHelper) {
            databaseHelper = new PushDatabaseHelperImpl(context.getApplicationContext());
        }
        return databaseHelper;
    }

    public static SqliteDatabaseProvider getDatabaseProvider(Context context) {
        return (SqliteDatabaseProvider) getDatabaseHelper(context);
    }

    @Nullable
    public ActivityLifecycleMonitor getActivityLifecycleMonitor() {
        return activityLifecycleMonitor;
    }

    public PostNotificationsPermissionRequester getPostNotificationsPermissionRequester() {
        return postNotificationsPermissionRequester;
    }

    public void sync() {
        sync(true, false);
    }

    public void lazySync() {
        sync(false, false);
    }

    public void foregroundSync() {
        sync(false, true);
    }

    private void sync(boolean required, boolean foreground) {
        didSyncAtLeastOnce = true;

        if (!required && (didSyncRecently() || (foreground && didSyncInForegroundRecently()))) {
            return;
        }

        if (!MobileNetworkInformation.isNetworkAvailableSafely(context)) {
            registerForNetworkAvailability();
            return;
        }

        if (TextUtils.isEmpty(MobileMessagingCore.getApplicationCode(context))) {
            MobileMessagingLogger.w("Application code is not found, check your MobileMessaging setup");
            return;
        }

        lastSyncTimeMillis = Time.now();
        baseUrlChecker().sync();
        if (foreground) {
            lastForegroundSyncMillis = lastSyncTimeMillis;
            userEventsSynchronizer().reportSessions();
            performSyncActions();
            versionChecker().sync();
            return;
        }

        performSyncActions();
    }

    private boolean didSyncRecently() {
        return lastSyncTimeMillis != null && Time.now() - lastSyncTimeMillis < LAZY_SYNC_THROTTLE_INTERVAL_MILLIS;
    }

    private boolean didSyncInForegroundRecently() {
        return lastForegroundSyncMillis != null && Time.now() - lastForegroundSyncMillis < FOREGROUND_SYNC_THROTTLE_INTERVAL_MILLIS;
    }

    // this method is invoked by successful connectivity invocation - it initiates the job for creating push reg ID and other sync actions
    public void retrySyncOnNetworkAvailable() {
        if (!didSyncAtLeastOnce) {
            return;
        }

        if (didSyncRecently()) {
            return;
        }

        MobileMessagingLogger.d(">>> Retry sync on network available");
        performSyncActions();
    }

    private void performSyncActions() {
        depersonalizeOnServerIfNeeded();
        syncInstallation();

        if (isDepersonalizeInProgress()) {
            return;
        }

        for (MessageHandlerModule module : messageHandlerModules.values()) {
            module.performSyncActions();
        }

        if (shouldRepersonalize()) {
            personalizeSynchronizer().repersonalize();
        } else {
            userDataReporter().patch(null, getUnreportedUserData());
        }
        messagesSynchronizer().sync();
        moMessageSender().sync();
        seenStatusReporter().sync();
    }

    protected void syncInstallation() {
        installationSynchronizer().sync();
    }

    public Boolean getUnreportedPrimarySetting() {
        return PreferenceHelper.runTransaction(new PreferenceHelper.Transaction<Boolean>() {
            @Override
            public Boolean run() {
                if (PreferenceHelper.contains(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED)) {
                    return PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED);
                }
                return null;
            }
        });
    }

    public void setUnreportedPrimarySetting() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED, true);
    }

    public boolean isRegistrationAvailable() {
        if (StringUtils.isBlank(getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration is not available yet");
            return false;
        }
        return true;
    }

    private void depersonalizeOnServerIfNeeded() {
        if (!isRegistrationAvailable()) {
            return;
        }

        if (!isDepersonalizeInProgress()) {
            return;
        }

        personalizeSynchronizer().depersonalize();
    }

    @Override
    public void onServerDepersonalizeStarted() {
        onDepersonalizeStarted(getPushRegistrationId());
    }

    @Override
    public void onServerDepersonalizeCompleted() {
        onDepersonalizeCompleted();
    }

    @Override
    public void onServerDepersonalizeFailed(Throwable error) {
        MobileMessagingLogger.w("Server depersonalize failed", error);
    }

    private void onDepersonalizeCompleted() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_DEPERSONALIZE_UNREPORTED, false);
        broadcaster.depersonalized();
        resetCloudToken(true);
    }

    private void onDepersonalizeStarted(String pushRegId) {
        if (pushRegId == null) {
            return;
        }

        if (pushRegId.equals(getPushRegistrationId())) {
            depersonalizeCurrentInstallation(false);
            return;
        }

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_DEPERSONALIZE_UNREPORTED, true);
    }

    private void depersonalizeCurrentInstallation(boolean forceDepersonalize) {
        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_USER_DATA);
        PreferenceHelper.remove(context, MobileMessagingProperty.USER_DATA);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS);
        PreferenceHelper.remove(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED);
        PreferenceHelper.remove(context, MobileMessagingProperty.UNSENT_MO_MESSAGES);
        PreferenceHelper.remove(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES);
        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES);
        PreferenceHelper.remove(context, MobileMessagingProperty.APP_USER_ID);
        PreferenceHelper.remove(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED);

        if (!forceDepersonalize) {
            PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_DEPERSONALIZE_UNREPORTED, true);
        }
        if (messageStore != null) {
            messageStore.deleteAll(context);
        }
        getNotificationHandler().cancelAllNotifications();
        for (MessageHandlerModule module : messageHandlerModules.values()) {
            module.depersonalize();
        }
    }

    public boolean isDepersonalizeInProgress() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_DEPERSONALIZE_UNREPORTED);
    }

    private void registerForNetworkAvailability() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MobileMessagingJobService.registerJobForConnectivityUpdates(context);
        }
    }

    public NotificationHandler getNotificationHandler() {
        return notificationHandler;
    }

    public String getPushRegistrationId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);
    }

    public void savePrimarySetting(boolean isPrimary) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_PRIMARY, isPrimary);
    }

    @Override
    public void setInstallationAsPrimary(@NonNull String pushRegistrationId, boolean isPrimary) {
        setInstallationAsPrimary(pushRegistrationId, isPrimary, null);
    }

    @Override
    public void setInstallationAsPrimary(@NonNull final String pushRegistrationId, final boolean isPrimary, final ResultListener<List<Installation>> listener) {
        installationSynchronizer().updatePrimaryStatus(pushRegistrationId, isPrimary, new ResultListener<Installation>() {
            @Override
            public void onResult(Result<Installation, MobileMessagingError> result) {
                if (!result.isSuccess()) {
                    if (listener != null) {
                        listener.onResult(new Result<List<Installation>, MobileMessagingError>(result.getError()));
                    }
                    return;
                }

                List<Installation> installationsToReturn = performLocalSettingOfPrimary(pushRegistrationId, isPrimary);
                if (listener != null) {
                    listener.onResult(new Result<>(installationsToReturn));
                }
            }
        });
    }

    private List<Installation> performLocalSettingOfPrimary(String pushRegId, boolean isPrimary) {
        User user = getUser();
        if (user == null) {
            return null;
        }

        List<Installation> installations = user.getInstallations();
        if (installations != null && !installations.isEmpty()) {
            List<Installation> installationsTemp = new ArrayList<>();
            for (Installation installation : installations) {
                if (pushRegId.equals(installation.getPushRegistrationId())) {
                    installation.setPrimaryDevice(isPrimary);
                } else if (installation.isPrimaryDevice()) {
                    installation.setPrimaryDevice(false);
                }
                installationsTemp.add(installation);
            }
            user.setInstallations(installationsTemp);
            saveUserDataToPrefs(user);
        }
        return user.getInstallations();
    }

    public void saveCustomAttributes(Map<String, CustomAttributeValue> customAttributes) {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES, nullSerializer.serialize(customAttributes));
    }

    public String getCustomAttributes() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES);
    }

    public void setUnreportedCustomAttributes(Map<String, CustomAttributeValue> customAttributes) {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES, nullSerializer.serialize(customAttributes));
    }

    private String getUnreportedCustomAttributes() {
        if (PreferenceHelper.contains(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES)) {
            return PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES);
        }
        return null;
    }

    public Map<String, CustomAttributeValue> getMergedUnreportedAndReportedCustomAtts() {
        String unreportedCustomAttributes = getUnreportedCustomAttributes();
        String reportedCustomAtts = getCustomAttributes();
        Map<String, CustomAttributeValue> unreportedCustomAttsMap = CustomAttributesMapper.customAttsFrom(unreportedCustomAttributes);
        Map<String, CustomAttributeValue> customAttsMap = CustomAttributesMapper.customAttsFrom(reportedCustomAtts);
        if (customAttsMap == null) {
            customAttsMap = new HashMap<>();
        }
        if (unreportedCustomAttsMap != null) {
            customAttsMap.putAll(unreportedCustomAttsMap);
        }
        return customAttsMap;
    }

    private boolean isPrimaryDevice() {
        return PreferenceHelper.runTransaction(new PreferenceHelper.Transaction<Boolean>() {
            @Override
            public Boolean run() {
                if (PreferenceHelper.contains(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED)) {
                    return PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED);
                }
                return PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_PRIMARY);
            }
        });
    }

    private void saveApplicationUserId(String applicationUserId) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.APP_USER_ID, applicationUserId);
    }

    public String getApplicationUserId() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.APP_USER_ID);
    }

    public void setApplicationUserIdReported(boolean reported) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED, !reported);
    }

    public Boolean isApplicationUserIdReported() {
        return !PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED);
    }

    public void setShouldRepersonalize(boolean shouldRepersonalize) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.SHOULD_REPERSONALIZE, shouldRepersonalize);
    }

    public Boolean shouldRepersonalize() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.SHOULD_REPERSONALIZE);
    }

    @SuppressWarnings("unchecked")
    private void reportErrorDepersonalizeInProgress(final ResultListener listener) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onResult(new Result<>(InternalSdkError.DEPERSONALIZATION_IN_PROGRESS.getError()));
                }
            }
        });
    }

    public void setPushRegistrationEnabled(Boolean pushRegistrationEnabled) {
        if (pushRegistrationEnabled != null) {
            PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, pushRegistrationEnabled);
        }
    }

    public boolean isPushRegistrationEnabled() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED);
    }

    public boolean isPushRegistrationEnabledUnreported() {
        return PreferenceHelper.contains(context, MobileMessagingProperty.UNREPORTED_PUSH_REGISTRATION_ENABLED) &&
                PreferenceHelper.findBoolean(context, MobileMessagingProperty.UNREPORTED_PUSH_REGISTRATION_ENABLED);
    }

    public void setPushRegistrationEnabledReported(boolean reported) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.UNREPORTED_PUSH_REGISTRATION_ENABLED, !reported);
    }

    public String getCloudToken() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.CLOUD_TOKEN);
    }

    public void setCloudToken(String registrationId) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.CLOUD_TOKEN, registrationId);
        setCloudTokenReported(false);
    }

    @NonNull
    public String[] getAndRemoveUnreportedMessageIds() {
        return PreferenceHelper.findAndRemoveStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS);
    }

    public void addUnreportedMessageIds(String... messageIDs) {
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS, messageIDs);
    }

    public void addSyncMessagesIds(String... messageIDs) {
        String[] timestampMessageIdPair = enrichMessageIdsWithTimestamp(messageIDs);
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS, timestampMessageIdPair);
    }

    public String[] getSyncMessagesIds() {
        String[] messageIds = PreferenceHelper.findStringArray(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS);
        ArrayList<String> messageIdsArrayList = new ArrayList<>(Arrays.asList(messageIds));
        Set<String> messageIdsToSync = new HashSet<>(Math.min(messageIdsArrayList.size(), MESSAGE_ID_PARAMETER_LIMIT));
        boolean shouldUpdateMessageIds = false;

        for (int i = 0; i < messageIdsArrayList.size(); i++) {
            String syncMessage = messageIdsArrayList.get(i);
            String[] messageIdWithTimestamp = syncMessage.split(StringUtils.COMMA_WITH_SPACE);

            String strTimeMessageReceived = messageIdWithTimestamp[1];

            long timeMessageReceived = Long.parseLong(strTimeMessageReceived);
            long timeInterval = Time.now() - timeMessageReceived;

            if (timeInterval > MESSAGE_EXPIRY_TIME || i >= MESSAGE_ID_PARAMETER_LIMIT) {
                messageIdsArrayList.remove(i);
                shouldUpdateMessageIds = true;
                i--;
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

        return messageIdsToSync.toArray(new String[0]);
    }

    public boolean isMessageAlreadyProcessed(String messageId) {
        return Arrays.asList(getSyncMessagesIds()).contains(messageId);
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
                    String[] messageIdAndTimestamp = reports[i].split(StringUtils.COMMA_WITH_SPACE);
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

    public void updateGeneratedMessageIds(final Map<String, String> messageIdMap) {
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
        String[] seenMessages = enrichMessageIdsWithTimestamp(messageIDs);
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS, seenMessages);
    }

    public String[] enrichMessageIdsWithTimestamp(String[] messageIDs) {
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

        return syncMessages.toArray(new String[0]);
    }

    public String[] filterOutGeneratedMessageIds(String[] messageIDs) {
        List<String> generatedMessageIDs = Arrays.asList(getGeneratedMessageIds());
        if (generatedMessageIDs.size() == 0 && messageIDs.length == 0) {
            return messageIDs;
        }

        List<String> seenIds = getSeenMessageIdsFromReports(messageIDs);
        List<String> seenIdsToRemove = new ArrayList<>();
        for (String seenMsgId : seenIds) {
            if (generatedMessageIDs.contains(seenMsgId) || isInUuidFormat(seenMsgId)) {
                seenIdsToRemove.add(seenMsgId);
            }
        }

        List<String> filteredSeenReports = new ArrayList<>(Arrays.asList(messageIDs));
        for (String seenReport : messageIDs) {
            String seenMessageIdFromReport = getSeenMessageIdFromReport(seenReport);
            if (seenMessageIdFromReport != null && seenIdsToRemove.contains(seenMessageIdFromReport)) {
                filteredSeenReports.remove(seenReport);
            }
        }
        return filteredSeenReports.toArray(new String[0]);
    }

    private boolean isInUuidFormat(String msgIdToReport) {
        Pattern p = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        return p.matcher(msgIdToReport).matches();
    }

    /**
     * Returns list of messageId and seenTimestamp
     *
     * @param reports concatenated message id and timestamp
     * @return reports
     */
    public List<String> getSeenMessageIdsFromReports(String[] reports) {
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
        if (messageIds != null) {
            addUnreportedMessageIds(messageIds);
            addSyncMessagesIds(messageIds);
            sync();
        }
    }

    public void setMessagesSeen(String... messageIds) {
        if (messageIds != null) {
            addUnreportedSeenMessageIds(messageIds);
            updateStoredMessagesWithSeenStatus(messageIds);
            lazySync();
        }
    }

    public void setMessagesSeenDontStore(String... messageIds) {
        if (messageIds != null) {
            addUnreportedSeenMessageIds(messageIds);
            lazySync();
        }
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

    @Nullable
    public NotificationSettings getNotificationSettings() {
        if (!isDisplayNotificationEnabled()) {
            return null;
        }
        if (null != notificationSettings)
            return notificationSettings;

        notificationSettings = new NotificationSettings(context);
        return notificationSettings;
    }

    public String getUniversalInstallationId() {
        if (this.installationId == null) {
            this.installationId = PreferenceHelper.findString(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID);
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID, installationId);
        }
        return this.installationId;
    }

    private void setNotificationSettings(NotificationSettings notificationSettings) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, null != notificationSettings);
        this.notificationSettings = notificationSettings;
    }

    private boolean isDisplayNotificationEnabled() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED);
    }

    public boolean isRegistrationIdReported() {
        return installationSynchronizer().isCloudTokenReported();
    }

    public boolean isPushServiceTypeChanged() {
        String reportedPushServiceType = getReportedPushServiceType();
        return StringUtils.isBlank(reportedPushServiceType) || !Platform.usedPushServiceType.name().equals(reportedPushServiceType);
    }

    private String getReportedPushServiceType() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.REPORTED_PUSH_SERVICE_TYPE);
    }

    public void setReportedPushServiceType() {
        PreferenceHelper.saveString(context, MobileMessagingProperty.REPORTED_PUSH_SERVICE_TYPE, Platform.usedPushServiceType.name());
    }

    public void setCloudTokenReported(boolean reported) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, reported);
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

    @Override
    public Installation getInstallation() {
        return getInstallation(false);
    }

    public Installation getInstallation(boolean restrictData) {
        boolean reportEnabled = false;
        if (restrictData) {
            reportEnabled = PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO);
        }
        Map<String, CustomAttributeValue> customAttsMap = new HashMap<>();
        String customAttributes = getCustomAttributes();
        if (customAttributes != null) {
            customAttsMap = CustomAttributesMapper.customAttsFrom(customAttributes);
        }
        return new Installation(
                getPushRegistrationId(),
                isPushRegistrationEnabled(),
                SoftwareInformation.areNotificationsEnabled(context),
                isGeofencingActivated(),
                SoftwareInformation.getSDKVersion(),
                reportEnabled ? SoftwareInformation.getAppVersion(context) : "",
                Platform.os,
                reportEnabled ? SystemInformation.getAndroidSystemVersion() : "",
                reportEnabled ? DeviceInformation.getDeviceManufacturer() : "",
                reportEnabled ? DeviceInformation.getDeviceModel() : "",
                reportEnabled && DeviceInformation.isDeviceSecure(context),
                reportEnabled ? SystemInformation.getAndroidSystemLanguage() : "",
                reportEnabled ? DeviceInformation.getDeviceTimeZoneOffset() : "",
                getApplicationUserId(),
                reportEnabled ? DeviceInformation.getDeviceModel() : "",
                isPrimaryDevice(),
                Platform.usedPushServiceType,
                getCloudToken(),
                customAttsMap);
    }

    @Override
    public void fetchInstallation(ResultListener<Installation> listener) {
        installationSynchronizer().fetchInstance(listener);
    }

    @Override
    public void saveInstallation(@NonNull Installation installation) {
        saveInstallation(installation, null);
    }

    @Override
    public void saveInstallation(@NonNull Installation installation, ResultListener<Installation> listener) {
        boolean isMyInstallation = isMyInstallation(installation);

        if (isMyInstallation) {
            if (installation.containsField(AppInstanceAtts.regEnabled)) {
                PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, installation.isPushRegistrationEnabled());
                PreferenceHelper.saveBoolean(context, MobileMessagingProperty.UNREPORTED_PUSH_REGISTRATION_ENABLED, true);
            }
            if (installation.containsField(AppInstanceAtts.customAttributes)) {
                setUnreportedCustomAttributes(installation.getCustomAttributes());
            }
            if (installation.containsField(AppInstanceAtts.isPrimary)) {
                PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_PRIMARY, installation.isPrimaryDevice());
                PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED, true);
            }
            if (installation.containsField(AppInstanceAtts.applicationUserId)) {
                setApplicationUserIdReported(false);
                saveApplicationUserId(installation.getApplicationUserId());
            }
        }

        if (isDepersonalizeInProgress()) {
            reportErrorDepersonalizeInProgress(listener);
            return;
        }

        installationSynchronizer().patch(installation, listener, isMyInstallation);
    }

    public boolean isMyInstallation(Installation installation) {
        String myPushRegId = getPushRegistrationId();
        return installation.getPushRegistrationId() == null || (myPushRegId != null && myPushRegId.equals(installation.getPushRegistrationId()));
    }

    @SuppressWarnings({"WeakerAccess"})
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

    static String getApplicationCodeFromResources(Context context) {
        int resource = ResourceLoader.loadResourceByName(context, "string", "infobip_application_code");
        if (resource > 0) {
            return context.getResources().getString(resource);
        }
        return null;
    }

    public static String getApplicationCode(Context context) {
        if (applicationCode != null) {
            return applicationCode;
        }

        // resolve from storage
        if (shouldSaveApplicationCode(context)) {
            applicationCode = getStoredApplicationCode(context);
            if (applicationCode != null) {
                return applicationCode;
            }
        }

        // resolve from app code provider
        applicationCode = resolveApplicationCodeFromAppCodeProvider(context, applicationCodeProvider);

        if (applicationCode == null) {
            // if still null fallback to check string resources if app code couldn't be resolved on storage or on app code provider
            applicationCode = getApplicationCodeFromResources(context);
        }

        return applicationCode;
    }

    @Nullable
    private static String resolveApplicationCodeFromAppCodeProvider(Context context, @Nullable ApplicationCodeProvider applicationCodeProvider) {
        if (applicationCodeProvider != null) {
            applicationCode = applicationCodeProvider.resolve();
            String applicationCodeHash = getApplicationCodeHash(context, applicationCode);
            PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE_HASH, applicationCodeHash);
            return applicationCode;
        }

        String appCodeProviderCanonicalClassName = getApplicationCodeProviderClassName(context);

        try {
            if (StringUtils.isNotBlank(appCodeProviderCanonicalClassName)) {
                Class<?> c = Class.forName(appCodeProviderCanonicalClassName);
                Object applicationCodeProviderInstance = c.newInstance();
                Method resolve = ApplicationCodeProvider.class.getMethod("resolve");
                applicationCode = String.valueOf(resolve.invoke(applicationCodeProviderInstance));
                String applicationCodeHash = getApplicationCodeHash(context, applicationCode);
                PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE_HASH, applicationCodeHash);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return applicationCode;
    }

    private static String getStoredApplicationCode(Context context) {
        return PreferenceHelper.findString(context, MobileMessagingProperty.APPLICATION_CODE);
    }

    private static String getStoredApplicationCodeHash(Context context) {
        return PreferenceHelper.findString(context, MobileMessagingProperty.APPLICATION_CODE_HASH);
    }

    @Nullable
    public String getApplicationCode() {
        return getApplicationCode(context);
    }

    public static String getApplicationCodeHash(Context context) {
        return getApplicationCodeHash(context, getApplicationCode(context));
    }

    public static String getApplicationCodeHash(Context context, String applicationCode) {
        if (StringUtils.isBlank(applicationCode)) {
            return null;
        }

        if (applicationCodeHashMap != null && applicationCodeHashMap.containsKey(applicationCode)) {
            return applicationCodeHashMap.get(applicationCode);
        }

        String appCodeHash = calculateAppCodeHash(applicationCode);
        applicationCodeHashMap = Collections.singletonMap(applicationCode, appCodeHash);
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE_HASH, appCodeHash);

        return appCodeHash;
    }

    @NonNull
    private static String calculateAppCodeHash(String applicationCode) {
        return SHA1.calc(applicationCode).substring(0, 10);
    }

    public static void setApiUri(Context context, String apiUri) {
        if (StringUtils.isBlank(apiUri)) {
            return;
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, apiUri);
    }

    public static void resetApiUri(Context context) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, (String) MobileMessagingProperty.API_URI.getDefaultValue());
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

    static void setRemoteNotificationsEnabled(Context context, boolean postNotificationPermissionRequest) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.POST_NOTIFICATIONS_REQUEST_ENABLED, postNotificationPermissionRequest);
    }

    public static void setShouldSaveUserData(Context context, boolean shouldSaveUserData) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.SAVE_USER_DATA_ON_DISK, shouldSaveUserData);
    }

    public boolean shouldSaveUserData() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.SAVE_USER_DATA_ON_DISK.getKey(), true);
    }

    public static void setShouldSaveAppCode(Context context, boolean shouldSaveAppCode) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.SAVE_APP_CODE_ON_DISK, shouldSaveAppCode);
        if (!shouldSaveAppCode)
            PreferenceHelper.remove(context, MobileMessagingProperty.APPLICATION_CODE);
    }

    public static void setAllowUntrustedSSLOnError(Context context, boolean allowUntrustedSSLOnError) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.ALLOW_UNTRUSTED_SSL_ON_ERROR, allowUntrustedSSLOnError);
    }

    public static void setSharedPrefsStorage(Context context, boolean usePrivateSharedPrefs) {
        PreferenceHelper.saveUsePrivateSharedPrefs(context, usePrivateSharedPrefs);
    }

    static boolean shouldSaveApplicationCode(Context context) {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.SAVE_APP_CODE_ON_DISK.getKey(), true);
    }

    @Override
    public void cleanup() {
        MobileMessagingCore.cleanup(context);
    }

    private static void cleanup(Context context) {
        for (MessageHandlerModule module : Platform.mobileMessagingCore.get(context).messageHandlerModules.values()) {
            module.cleanup();
        }

        applicationCode = null;
        PreferenceHelper.remove(context, MobileMessagingProperty.APPLICATION_CODE);
        PreferenceHelper.remove(context, MobileMessagingProperty.APPLICATION_CODE_HASH);

        if (mobileMessagingSynchronizationReceiver != null) {
            ComponentUtil.setSynchronizationReceiverStateEnabled(context, mobileMessagingSynchronizationReceiver, false);
            mobileMessagingSynchronizationReceiver = null;
        }
        ComponentUtil.setConnectivityComponentsStateEnabled(context, false);
        resetMobileApi();

        PreferenceHelper.remove(context, MobileMessagingProperty.CLOUD_TOKEN);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_PUSH_SERVICE_TYPE);

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, false);

        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_USER_DATA);
        PreferenceHelper.remove(context, MobileMessagingProperty.USER_DATA);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_UNREPORTED_MESSAGE_IDS);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_UNREPORTED_SEEN_MESSAGE_IDS);
        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);
        PreferenceHelper.remove(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED);
        PreferenceHelper.remove(context, MobileMessagingProperty.UNSENT_MO_MESSAGES);

        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_SYNC_MESSAGES_IDS);
        PreferenceHelper.remove(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES);
        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES);
        PreferenceHelper.remove(context, MobileMessagingProperty.APP_USER_ID);
        PreferenceHelper.remove(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED);
        PreferenceHelper.remove(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED);
        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_PUSH_REGISTRATION_ENABLED);
        PreferenceHelper.remove(context, MobileMessagingProperty.IS_DEPERSONALIZE_UNREPORTED);
        PreferenceHelper.remove(context, MobileMessagingProperty.BASEURL_CHECK_LAST_TIME);
        PreferenceHelper.remove(context, MobileMessagingProperty.BASEURL_CHECK_INTERVAL_HOURS);
        PreferenceHelper.remove(context, MobileMessagingProperty.POST_NOTIFICATIONS_REQUEST_ENABLED);

        MobileMessagingCore mmCore = Platform.mobileMessagingCore.get(context);
        mmCore.messagesSynchronizer = null;
        mmCore.userDataReporter = null;
        mmCore.installationSynchronizer = null;
        mmCore.personalizeSynchronizer = null;
        mmCore.userEventsSynchronizer = null;
        mmCore.moMessageSender = null;
        mmCore.seenStatusReporter = null;
        mmCore.versionChecker = null;
        mmCore.baseUrlChecker = null;
        resetApiUri(context);

        mmCore.didSyncAtLeastOnce = false;
        mmCore.lastForegroundSyncMillis = null;
        mmCore.lastSyncTimeMillis = null;

        ComponentUtil.setSynchronizationReceiverStateEnabled(context, mobileMessagingSynchronizationReceiver, false);
        ComponentUtil.setConnectivityComponentsStateEnabled(context, false);

        //it's needed for MobileMessagingCore.Build, when user uses different appCode
        MobileMessagingCloudService.enqueueTokenCleanup(context, mmCore.firebaseAppProvider);
    }

    public void resetCloudToken(boolean force) {
        if (force || !didSyncRecently()) {
            MobileMessagingCloudService.enqueueTokenReset(context, firebaseAppProvider);
        }
    }

    public static void resetMobileApi() {
        mobileApiResourceProvider = null;
    }

    @Override
    public void saveUser(@NonNull User user) {
        saveUser(user, null);
    }

    @Override
    public void saveUser(@NonNull User user, final MobileMessaging.ResultListener<User> listener) {
        User userToReport = storeAndMergeUnreportedUserLocally(user);

        if (isDepersonalizeInProgress()) {
            reportErrorDepersonalizeInProgress(listener);
            return;
        }

        userDataReporter().patch(listener, userToReport);
    }

    private User storeAndMergeUnreportedUserLocally(User user) {
        User existingData = getUnreportedUserData();
        User userToReport = UserMapper.merge(existingData, user);

        if (userToReport != null) {
            saveUnreportedUserData(userToReport);
        }
        return userToReport;
    }

    @Override
    public void fetchUser(@NonNull MobileMessaging.ResultListener<User> listener) {
        if (isDepersonalizeInProgress()) {
            reportErrorDepersonalizeInProgress(listener);
            return;
        }

        userDataReporter().fetch(listener);
    }

    @Nullable
    public User getUser() {
        User existing = null;
        if (PreferenceHelper.contains(context, MobileMessagingProperty.USER_DATA)) {
            existing = UserMapper.fromJson(PreferenceHelper.findString(context, MobileMessagingProperty.USER_DATA));
            if (areInstallationsExpired() && existing != null) {
                existing.setInstallations(null);
            }
        }

        return UserMapper.merge(existing, getUnreportedUserData());
    }

    @Override
    public void personalize(@NonNull UserIdentity userIdentity, @Nullable UserAttributes userAttributes) {
        personalize(userIdentity, userAttributes, false, null);
    }

    @Override
    public void personalize(@NonNull UserIdentity userIdentity, @Nullable UserAttributes userAttributes, boolean forceDepersonalize) {
        personalize(userIdentity, userAttributes, forceDepersonalize, null);
    }

    @Override
    public void personalize(@NonNull UserIdentity userIdentity, @Nullable UserAttributes userAttributes, ResultListener<User> listener) {
        personalize(userIdentity, userAttributes, false, listener);
    }

    @Override
    public void personalize(@NonNull UserIdentity userIdentity, @Nullable UserAttributes userAttributes, boolean forceDepersonalize, final ResultListener<User> listener) {
        if (!isRegistrationAvailable()) {
            if (listener != null) {
                listener.onResult(new Result<>(getUser(), InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
            return;
        }

        if (!MobileNetworkInformation.isNetworkAvailableSafely(context)) {
            registerForNetworkAvailability();
            if (listener != null) {
                listener.onResult(new Result<>(getUser(), InternalSdkError.NETWORK_UNAVAILABLE.getError()));
            }
            return;
        }

        if (forceDepersonalize) {
            depersonalizeCurrentInstallation(true);
        }

        if (!userIdentity.hasDataToReport()) {
            MobileMessagingLogger.w("Attempt to save empty user identity, will do nothing");
            if (listener != null) {
                listener.onResult(new Result<>(getUser(), InternalSdkError.ERROR_SAVING_EMPTY_OBJECT.getError()));
            }
            return;
        }

        if (userAttributes != null && userAttributes.hasDataToReport()) {
            // sanitizing user atts map if the object was initialized as a User, not UserAttributes
            if (userAttributes.containsField(UserAtts.phones))
                userAttributes.getMap().remove(UserAtts.phones);
            if (userAttributes.containsField(UserAtts.emails))
                userAttributes.getMap().remove(UserAtts.emails);
            if (userAttributes.containsField(UserAtts.externalUserId))
                userAttributes.getMap().remove(UserAtts.externalUserId);
        }

        personalizeSynchronizer().personalize(userIdentity, userAttributes, forceDepersonalize, listener);
    }

    private boolean areInstallationsExpired() {
        Date now = Time.date();
        long expiryTimestamp = PreferenceHelper.findLong(context, MobileMessagingProperty.USER_INSTALLATIONS_EXPIRE_AT);
        if (expiryTimestamp != 0) {
            Date expiryDate = new Date(expiryTimestamp);
            return expiryDate.before(now);
        }
        return false;
    }

    @Override
    public void depersonalize() {
        if (!isRegistrationAvailable()) {
            return;
        }

        String currentPushRegistrationId = getPushRegistrationId();
        onDepersonalizeStarted(currentPushRegistrationId);
        if (!MobileNetworkInformation.isNetworkAvailableSafely(context)) {
            registerForNetworkAvailability();
            return;
        }

        personalizeSynchronizer().depersonalize();
    }

    @Override
    public void depersonalize(ResultListener<SuccessPending> listener) {
        depersonalize(getPushRegistrationId(), listener);
    }

    public void depersonalize(@NonNull String pushRegId, final ResultListener<SuccessPending> listener) {
        if (!isRegistrationAvailable()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.onResult(new Result<SuccessPending, MobileMessagingError>(InternalSdkError.NO_VALID_REGISTRATION.getError()));
                    }
                }
            });
            return;
        }

        onDepersonalizeStarted(pushRegId);
        if (!MobileNetworkInformation.isNetworkAvailableSafely(context)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.onResult(new Result<>(SuccessPending.Pending));
                    }
                }
            });
            registerForNetworkAvailability();
            return;
        }

        personalizeSynchronizer().depersonalize(pushRegId, new DepersonalizeActionListener() {
            @Override
            public void onUserInitiatedDepersonalizeCompleted() {
                onDepersonalizeCompleted();
                if (listener != null) {
                    listener.onResult(new Result<>(SuccessPending.Success));
                }
            }

            @Override
            public void onUserInitiatedDepersonalizeFailed(Throwable error) {
                if (listener != null) {
                    listener.onResult(new Result<SuccessPending, MobileMessagingError>(MobileMessagingError.createFrom(error)));
                }
            }
        });
    }

    @Override
    public void depersonalizeInstallation(@NonNull final String pushRegId, final ResultListener<List<Installation>> listener) {
        depersonalize(pushRegId, new ResultListener<SuccessPending>() {

            @Override
            public void onResult(Result<SuccessPending, MobileMessagingError> result) {
                SuccessPending resultData = result.getData();
                if (resultData != null && SuccessPending.Pending.name().equals(resultData.name())) {
                    if (listener != null) {
                        //TODO put something more convenient here or use different approach!
                        listener.onResult(new Result<List<Installation>, MobileMessagingError>(MobileMessagingError.createFrom(new IllegalStateException())));
                    }
                    return;
                }

                if (!result.isSuccess()) {
                    if (listener != null) {
                        listener.onResult(new Result<List<Installation>, MobileMessagingError>(result.getError()));
                    }
                    return;
                }

                List<Installation> installations = performLocalDepersonalization(pushRegId);
                if (listener != null) {
                    listener.onResult(new Result<>(installations));
                }
            }
        });
    }

    @Override
    public void submitEvent(@NonNull CustomEvent customEvent) {
        addUnreportedUserCustomEvent(customEvent);
        userEventsSynchronizer().reportCustomEvents();
    }

    @Override
    public void submitEvent(@NonNull CustomEvent customEvent, ResultListener<CustomEvent> listener) {
        if (TextUtils.isEmpty(customEvent.getDefinitionId())) {
            listener.onResult(new Result<>(customEvent, MobileMessagingError.createFrom(new RuntimeException("Definition ID needs to be provided"))));
            return;
        }
        userEventsSynchronizer().reportCustomEvent(customEvent, listener);
    }

    public void addUnreportedUserCustomEvent(CustomEvent customEvent) {
        UserCustomEventBody customEventRequest = UserEventsRequestMapper.createCustomEventRequest(customEvent);
        if (customEventRequest == null) return;
        UserCustomEventBody.CustomEvent customEvents = customEventRequest.getEvents()[0];
        String customEventRequestJsonString = UserEventsRequestMapper.toJson(customEvents);
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.USER_CUSTOM_EVENTS, customEventRequestJsonString);
    }

    public UserCustomEventBody.CustomEvent[] getUnreportedUserCustomEvents() {
        String[] customEventsStringArray = PreferenceHelper.findStringArray(context, MobileMessagingProperty.USER_CUSTOM_EVENTS);
        List<UserCustomEventBody.CustomEvent> customEvents = new ArrayList<>();
        for (String eventJsonString : customEventsStringArray) {
            customEvents.add(UserEventsRequestMapper.fromJson(eventJsonString));
        }
        return customEvents.toArray(new UserCustomEventBody.CustomEvent[0]);
    }

    public void setUserCustomEventsReported() {
        PreferenceHelper.remove(context, MobileMessagingProperty.USER_CUSTOM_EVENTS);
    }

    public void saveSessionBounds(Context context, long sessionStartTimeMillis, long sessionEndTimeMillis) {
        String sessionStartDateTime = DateTimeUtil.dateToISO8601UTCString(new Date(sessionStartTimeMillis));
        String sessionEndDateTime = DateTimeUtil.dateToISO8601UTCString(new Date(sessionEndTimeMillis));
        String sessionBound = sessionStartDateTime + SESSION_BOUNDS_DELIMITER + sessionEndDateTime;

        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.SESSION_BOUNDS, sessionBound);
    }

    public void reportSessions() {
        if (StringUtils.isBlank(getApplicationCode(context))) {
            MobileMessagingLogger.d("Postponing session sync until app code is available");
            return;
        }
        userEventsSynchronizer().reportSessions();
    }

    public String[] getStoredSessionBounds() {
        return PreferenceHelper.findStringArray(context, MobileMessagingProperty.SESSION_BOUNDS);
    }

    public void setUserSessionsReported(String[] storedSessionBounds, long sessionStartsMillis) {
        if (storedSessionBounds != null && storedSessionBounds.length != 0) {
            PreferenceHelper.remove(context, MobileMessagingProperty.SESSION_BOUNDS);
        }
        PreferenceHelper.saveLong(context, MobileMessagingProperty.LAST_REPORTED_ACTIVE_SESSION_START_TIME_MILLIS, sessionStartsMillis);
    }

    public long getActiveSessionStartTime() {
        return PreferenceHelper.findLong(context, MobileMessagingProperty.ACTIVE_SESSION_START_TIME_MILLIS);
    }

    public long getLastReportedActiveSessionStartTime() {
        return PreferenceHelper.findLong(context, MobileMessagingProperty.LAST_REPORTED_ACTIVE_SESSION_START_TIME_MILLIS);
    }

    public long getActiveSessionEndTime() {
        return PreferenceHelper.findLong(context, MobileMessagingProperty.ACTIVE_SESSION_END_TIME_MILLIS);
    }

    public String getSessionIdHeader() {
        final String pushRegistrationId = getPushRegistrationId();
        final long activeSessionStartTime = getActiveSessionStartTime();
        if (pushRegistrationId != null && activeSessionStartTime != 0) {
            return pushRegistrationId + "_" + activeSessionStartTime;
        }
        return null;
    }

    private List<Installation> performLocalDepersonalization(String pushRegId) {
        User user = getUser();
        if (user == null) {
            return null;
        }

        List<Installation> installations = user.getInstallations();
        if (installations != null && !installations.isEmpty()) {
            List<Installation> installationsTemp = new ArrayList<>(installations);
            for (Installation installation : installationsTemp) {
                if (pushRegId.equals(installation.getPushRegistrationId())) {
                    installations.remove(installation);
                    break;
                }
            }
            user.setInstallations(installations);
            saveUserDataToPrefs(user);
        }
        return user.getInstallations();
    }

    @Nullable
    public User getUnreportedUserData() {
        if (PreferenceHelper.contains(context, MobileMessagingProperty.UNREPORTED_USER_DATA)) {
            return UserMapper.fromJson(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_USER_DATA));
        }
        return null;
    }

    public void setUserDataReportedWithError() {
        setUserDataReported(null, false);
    }

    public void setUserDataReported(User user, boolean merge) {
        if (user != null && shouldSaveUserData()) {
            User dataForStoring = user;
            dataForStoring.clearUnreportedData();
            if (merge) {
                dataForStoring = UserMapper.merge(getUser(), user);
            }
            if (dataForStoring != null) {
                saveUserDataToPrefs(filterOutDeletedData(dataForStoring));
            }
        }
        PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_USER_DATA);
    }

    private void saveUserDataToPrefs(User dataForStoring) {
        long oneMinuteInMillis = 60000;
        Calendar date = Calendar.getInstance();
        long t = date.getTimeInMillis();
        Date inOneMinute = new Date(t + oneMinuteInMillis);

        PreferenceHelper.saveLong(context, MobileMessagingProperty.USER_INSTALLATIONS_EXPIRE_AT, inOneMinute.getTime());
        PreferenceHelper.saveString(context, MobileMessagingProperty.USER_DATA, toJson(dataForStoring));
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

    public void sendMessagesDontStore(MobileMessaging.ResultListener<Message[]> listener, Message... messages) {
        moMessageSender().sendDontSave(listener, messages);
    }

    public void sendMessagesWithRetry(Message... messages) {
        moMessageSender().sendWithRetry(messages);
    }

    public boolean isGeofencingActivated() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED.getKey(), false);
    }

    public SystemData getUnreportedSystemData() {
        if (PreferenceHelper.contains(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA)) {
            return SystemData.fromJson(PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA));
        }
        return null;
    }

    public SystemData systemDataForReport(boolean forceSend) {
        boolean reportEnabled = PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO);

        SystemData data = new SystemData(SoftwareInformation.getSDKVersionWithPostfixForSystemData(context),
                reportEnabled ? SystemInformation.getAndroidSystemVersion() : "",
                reportEnabled ? DeviceInformation.getDeviceManufacturer() : "",
                reportEnabled ? DeviceInformation.getDeviceModel() : "",
                reportEnabled ? SoftwareInformation.getAppVersion(context) : "",
                isGeofencingActivated(),
                SoftwareInformation.areNotificationsEnabled(context),
                reportEnabled && DeviceInformation.isDeviceSecure(context),
                reportEnabled ? SystemInformation.getAndroidSystemLanguage() : "",
                reportEnabled ? SystemInformation.getAndroidDeviceName(context) : "",
                reportEnabled ? DeviceInformation.getDeviceTimeZoneOffset() : "");

        if (forceSend) {
            return data;
        }

        int hash = PreferenceHelper.findInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);
        if (hash != data.hashCode()) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA, data.toString());
            return data;
        }

        return null;
    }

    @NonNull
    public Installation populateInstallationWithSystemData(SystemData data, Installation installation) {
        installation.setSdkVersion(data.getSdkVersion());
        installation.setOsVersion(data.getOsVersion());
        installation.setDeviceManufacturer(data.getDeviceManufacturer());
        installation.setDeviceModel(data.getDeviceModel());
        installation.setAppVersion(data.getApplicationVersion());
        if (installation.getGeoEnabled() == null) installation.setGeoEnabled(data.isGeofencing());
        if (installation.getNotificationsEnabled() == null)
            installation.setNotificationsEnabled(data.areNotificationsEnabled());
        installation.setDeviceSecure(data.isDeviceSecure());
        if (installation.getLanguage() == null) installation.setLanguage(data.getLanguage());
        if (installation.getDeviceTimezoneOffset() == null)
            installation.setDeviceTimezoneOffset(data.getDeviceTimeZoneOffset());
        if (installation.getDeviceName() == null) installation.setDeviceName(data.getDeviceName());
        installation.setOs(Platform.os);
        return installation;
    }

    public void removeReportedSystemData() {
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);
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

    public void saveUnreportedUserData(User user) {
        if (shouldSaveUserData()) {
            PreferenceHelper.remove(context, MobileMessagingProperty.UNREPORTED_USER_DATA);
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_USER_DATA, toJson(user));
        }
    }

    /**
     * This method handles issues with absent registration and forces library to get back to the working state
     *
     * @param mobileMessagingError
     */
    public void handleNoRegistrationError(MobileMessagingError mobileMessagingError) {
        if (ApiErrorCode.NO_REGISTRATION.equalsIgnoreCase(mobileMessagingError.getCode())) {
            setCloudTokenReported(false);
            setUnreportedCustomAttributes(getMergedUnreportedAndReportedCustomAtts());
            setShouldRepersonalize(true);
            removeReportedSystemData();
            setUnreportedPrimarySetting();
            setPushRegistrationEnabledReported(false);
        }
    }

    private void runOnUiThread(final Runnable runnable) {
        new MAsyncTask<Void, Void>() {

            @Override
            public Void run(Void[] voids) {
                return null;
            }

            @Override
            public void after(Void aVoid) {
                runnable.run();
            }
        }.execute();
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
                    registrationAlignedExecutor, stats, retryPolicyProvider.DEFAULT(), mobileApiResourceProvider().getMobileApiMessages(context), getMessageStoreWrapper());
        }
        return moMessageSender;
    }

    @NonNull
    private UserDataReporter userDataReporter() {
        if (userDataReporter == null) {
            userDataReporter = new UserDataReporter(this, registrationAlignedExecutor,
                    broadcaster, retryPolicyProvider, stats, mobileApiResourceProvider().getMobileApiAppInstance(context));
        }
        return userDataReporter;
    }

    @NonNull
    private PersonalizeSynchronizer personalizeSynchronizer() {
        if (personalizeSynchronizer == null) {
            personalizeSynchronizer = new PersonalizeSynchronizer(
                    this,
                    broadcaster,
                    mobileApiResourceProvider().getMobileApiAppInstance(context),
                    retryPolicyProvider.DEFAULT(),
                    registrationAlignedExecutor,
                    new BatchReporter(PreferenceHelper.findLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY)),
                    this);
        }
        return personalizeSynchronizer;
    }

    @NonNull
    private MessagesSynchronizer messagesSynchronizer() {
        if (messagesSynchronizer == null) {
            MobileMessageHandler mobileMessageHandler = new MobileMessageHandler(this, broadcaster, getNotificationHandler(), getMessageStoreWrapper());
            messagesSynchronizer = new MessagesSynchronizer(this, stats, registrationAlignedExecutor,
                    broadcaster, retryPolicyProvider.DEFAULT(), mobileMessageHandler, mobileApiResourceProvider().getMobileApiMessages(context));
        }
        return messagesSynchronizer;
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
    private VersionChecker versionChecker() {
        if (versionChecker == null) {
            versionChecker = new VersionChecker(context, this, stats, mobileApiResourceProvider().getMobileApiVersion(context), retryPolicyProvider);
        }
        return versionChecker;
    }

    @NonNull
    private BaseUrlChecker baseUrlChecker() {
        if (baseUrlChecker == null) {
            baseUrlChecker = new BaseUrlChecker(context, registrationAlignedExecutor, mobileApiResourceProvider().getMobileApiBaseUrl(context));
        }
        return baseUrlChecker;
    }

    @NonNull
    private InstallationSynchronizer installationSynchronizer() {
        if (installationSynchronizer == null) {
            installationSynchronizer = new InstallationSynchronizer(
                    context,
                    this,
                    stats,
                    registrationAlignedExecutor,
                    broadcaster,
                    retryPolicyProvider,
                    mobileApiResourceProvider().getMobileApiAppInstance(context));
        }
        return installationSynchronizer;
    }

    @NonNull
    private UserEventsSynchronizer userEventsSynchronizer() {
        if (userEventsSynchronizer == null) {
            userEventsSynchronizer = new UserEventsSynchronizer(
                    this,
                    broadcaster,
                    mobileApiResourceProvider().getMobileApiAppInstance(context),
                    retryPolicyProvider.DEFAULT(),
                    registrationAlignedExecutor,
                    new BatchReporter(PreferenceHelper.findLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY)));
        }
        return userEventsSynchronizer;
    }

    @Override
    public void registerForRemoteNotifications() {
        setRemoteNotificationsEnabled(context, true);
        MobileMessagingCore.getInstance(context).getPostNotificationsPermissionRequester().requestPermission();
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
    @SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
    public static final class Builder {

        private final Application application;
        private NotificationSettings notificationSettings = null;
        private String applicationCode = null;
        private ApplicationCodeProvider applicationCodeProvider;
        private FirebaseOptions firebaseOptions;
        private Cryptor oldCryptor = null;

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
         * If you don't want to have automatic initialization of {@link FirebaseApp} by <a href=https://developers.google.com/android/guides/google-services-plugin>google-services plugin</a>,
         * you may use this method to provide {@link FirebaseOptions} at runtime. In this case MobileMessaging SDK will initialize [DEFAULT] {@link FirebaseApp}, using provided {@link FirebaseOptions}.
         * To create {@link FirebaseOptions} object use {@link FirebaseOptions.Builder} and values, which you can get from google-services.json file as described in the <a href=https://developers.google.com/android/guides/google-services-plugin>documentation of the google-services plugin<a/>.
         *
         * @param firebaseOptions, used to initialize {@link FirebaseApp} to register for push notifications.
         * @return {@link Builder}
         */
        public Builder withFirebaseOptions(FirebaseOptions firebaseOptions) {
            this.firebaseOptions = firebaseOptions;
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
         * provide it on demand. For example, you should implement <b>patch</b> API call to your server where you store required
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
         * This method will migrate data, encrypted with old unsecure algorithm (ECB) to new one {@link CryptorImpl} (CBC).
         * If you have installations of the application with MobileMessaging SDK version < 5.0.0,
         * use this method with providing old cryptor, so MobileMessaging SDK will migrate data using the new cryptor.
         * For code snippets (old cryptor implementation) and more details check docs on github - https://github.com/infobip/mobile-messaging-sdk-android/wiki/ECB-Cryptor-migration.
         *
         * @param oldCryptor, provide old cryptor, to migrate encrypted data to new one {@link CryptorImpl}.
         * @return {@link Builder}
         */
        public Builder withCryptorMigration(Cryptor oldCryptor) {
            this.oldCryptor = oldCryptor;
            return this;
        }

        /**
         * Builds the <i>MobileMessagingCore</i> configuration. Registration token patch is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @return {@link MobileMessagingCore}
         */
        public MobileMessagingCore build(@Nullable final InitListener initListener) {
            final Context applicationContext = application.getApplicationContext();
            cleanupLibraryDataIfAppCodeWasChanged(applicationContext);

            Platform.verify(application);

            MobileMessagingCore mobileMessagingCore = new MobileMessagingCore(application);
            mobileMessagingCore.firebaseAppProvider.setFirebaseOptions(firebaseOptions);
            mobileMessagingCore.setNotificationSettings(notificationSettings);
            mobileMessagingCore.setApplicationCode(applicationCode);
            mobileMessagingCore.setApplicationCodeProviderClassName(applicationCodeProvider);
            mobileMessagingCore.mobileNetworkStateListener = new MobileNetworkStateListener(application);
            mobileMessagingCore.playServicesSupport = new PlayServicesSupport();

            // do the force invalidation of old push cloud tokens
            boolean shouldResetToken = mobileMessagingCore.isPushServiceTypeChanged() && mobileMessagingCore.getPushRegistrationId() != null;
            mobileMessagingCore.playServicesSupport.checkPlayServicesAndTryToAcquireToken(applicationContext, shouldResetToken, initListener, mobileMessagingCore.firebaseAppProvider);

            Platform.reset(mobileMessagingCore);
            MobileMessagingCloudHandler cloudHandler = Platform.initializeMobileMessagingCloudHandler(application);
            Platform.reset(cloudHandler);
            return mobileMessagingCore;
        }

        private void cleanupLibraryDataIfAppCodeWasChanged(Context applicationContext) {
            PreferenceHelper.migrateCryptorIfNeeded(applicationContext, oldCryptor);
            String existingApplicationCodeHash = MobileMessagingCore.getStoredApplicationCodeHash(applicationContext);
            if (shouldSaveApplicationCode(applicationContext)) {
                String existingApplicationCode = MobileMessagingCore.getStoredApplicationCode(applicationContext);
                if (applicationCode != null) {
                    String resolvedApplicationCodeHash = MobileMessagingCore.calculateAppCodeHash(applicationCode);
                    if ((existingApplicationCode != null && !applicationCode.equals(existingApplicationCode) ||
                            existingApplicationCodeHash != null && !resolvedApplicationCodeHash.equals(existingApplicationCodeHash))) {
                        MobileMessagingLogger.d("Cleaning up push registration data because application code has changed");
                        MobileMessagingCore.cleanup(application);
                    }
                }
            } else {
                String resolvedApplicationCode = MobileMessagingCore.resolveApplicationCodeFromAppCodeProvider(applicationContext, applicationCodeProvider);
                if (existingApplicationCodeHash != null && resolvedApplicationCode != null) {
                    String resolvedApplicationCodeHash = MobileMessagingCore.calculateAppCodeHash(resolvedApplicationCode);
                    if (!existingApplicationCodeHash.equals(resolvedApplicationCodeHash)) {
                        MobileMessagingLogger.d("Cleaning up push registration data because application code has changed");
                        MobileMessagingCore.cleanup(application);
                    }
                }
            }
        }
    }
}
