package org.infobip.mobile.messaging;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;

import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * The main configuration class. It is used to configure and start the Mobile Messaging System.
 * <br>
 * It should be used in the Application entry point.
 * <pre>
 * {@code
 * public class MyActivity extends AppCompatActivity {
 *        protected void onCreate(Bundle savedInstanceState) {
 *            super.onCreate(savedInstanceState);
 *
 *            new MobileMessaging.Builder(getApplication()).build();
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
 * @see Builder#withoutCarrierInfo()
 * @see Builder#withoutSystemInfo()
 * @since 29.02.2016.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public abstract class MobileMessaging {

    /**
     * Gets an instance of MobileMessaging after it is initialized via {@link MobileMessaging.Builder}.
     * <br>
     * If the app was killed and there is no instance available, it will return a temporary instance based on current context.
     *
     * @param context android context object.
     * @return instance of MobileMessaging.
     * @see MobileMessaging.Builder
     */
    public synchronized static MobileMessaging getInstance(Context context) {
        return MobileMessagingCore.getInstance(context);
    }

    /**
     * Enables the push registration so that the application can receive push notifications
     * (regular push messages/geofencing campaign messages/messages fetched from the server).
     * MobileMessaging SDK has the push registration enabled by default.
     */
    public abstract void enablePushRegistration();

    /**
     * Disables the push registration so that the application is no longer able to receive push notifications
     * through MobileMessaging SDK (regular push messages/geofencing campaign messages/messages fetched from the server).
     * MobileMessaging SDK has the push registration enabled by default.
     */
    public abstract void disablePushRegistration();

    /**
     * Push registration status defines whether the device is allowed to receive push notifications
     * (regular push messages/geofencing campaign messages/messages fetched from the server).
     * MobileMessaging SDK has the push registration enabled by default.
     *
     * @return Current push registration status.
     */
    public abstract boolean isPushRegistrationEnabled();

    /**
     * Reports delivery of messages to Mobile Messaging servers.
     * <br>
     * This method has to be used only if you handle GCM message notifications
     * without Mobile Messaging library. In all other cases the library will
     * send delivery report automatically whenever GCM push is delivered to device.
     *
     * @param messageIds ids of messages to report delivery for
     * @see Event#DELIVERY_REPORTS_SENT
     */
    public abstract void setMessagesDelivered(final String... messageIds);

    /**
     * Reports seen status of messages to Mobile Messaging servers.
     * <br>
     * This method shall be used to report seen status when user actually sees message content.
     *
     * @param messageIds message ids to report seen status for
     * @see Event#SEEN_REPORTS_SENT
     */
    public abstract void setMessagesSeen(final String... messageIds);

    /**
     * Returns instance of message store that is used within the library or null if message store is not set.
     *
     * @return instance of message store.
     * @see MessageStore
     */
    public abstract MessageStore getMessageStore();

    /**
     * Does a synchronization of user data with server.
     * <br>
     * This method will synchronize new data with server and will also trigger {@link Event#USER_DATA_REPORTED}
     * with all the data currently available on a server for this user.
     *
     * @param userData user data object with desired changes
     * @see Event#USER_DATA_REPORTED
     */
    public abstract void syncUserData(UserData userData);

    /**
     * Does a synchronization of user data with server.
     * <br>
     * This method will synchronize new data with server. The result of synchronization will be provided via listener.
     * It will also trigger {@link Event#USER_DATA_REPORTED} with all the data currently available on a server for this user.
     *
     * @param userData user data object with desired changes
     * @param listener listener to report the result on
     * @see ResultListener
     * @see Event#USER_DATA_REPORTED
     */
    public abstract void syncUserData(UserData userData, ResultListener<UserData> listener);

    /**
     * Does a fetch of user data from the server.
     * <br>
     * This method will trigger {@link Event#USER_DATA_REPORTED} with all the data currently available on a server for this user.
     *
     * @see Event#USER_DATA_REPORTED
     * @deprecated Use {@link MobileMessaging#fetchUserData(ResultListener)} instead.
     */
    @Deprecated
    public abstract void fetchUserData();

    /**
     * Does a fetch of user data from the server.
     * <br>
     * The result of fetch operation will be provided via listener.
     * This method will also trigger {@link Event#USER_DATA_REPORTED} with all the data currently available on a server for this user.
     *
     * @see ResultListener
     * @see Event#USER_DATA_REPORTED
     */
    public abstract void fetchUserData(ResultListener<UserData> listener);

    /**
     * Reads user data that is currently stored in the library.
     * <br>
     * This method does not trigger {@link Event#USER_DATA_REPORTED}.
     *
     * @return last synchronized UserData object
     */
    @Nullable
    public abstract UserData getUserData();

    /**
     * Erases currently stored {@link UserData} on SDK and server associated with push registration, along with messages in SDK storage (also, deletes data for chat module).
     * <p>
     * User's data synced over MobileMessaging {@link #syncUserData(UserData)} is by default associated with created push
     * registration. Logging out user means that push registration along with device specific data will remain, but user's data
     * (such as first name, custom data,...) will be wiped out.
     * <p>
     * If you log out user, there is no mechanism to log him in again since he's already subscribed for broadcast notifications from your app,
     * but you might want to sync new user data to target this user specifically.
     * <p>
     * Use this method if:
     * <ul>
     * <li>you're syncing user data to our server</li>
     * <li>your application has logout option</li>
     * <li>you don't want new logged in user to be targeted by other user's data, e.g. first name</li>
     * <li>you want logged out user to still receive broadcast notifications (if not, you need to call {@link #disablePushRegistration()})</li>
     * </ul>
     *
     * @see Event#USER_LOGGED_OUT
     */
    public abstract void logout();

    /**
     * Erases currently stored {@link UserData} on SDK and server associated with push registration, along with messages in SDK storage (also, deletes data for chat module).
     * <br>
     * User's data synced over MobileMessaging {@link #syncUserData(UserData)} is by default associated with created push
     * registration. Logging out user means that push registration along with device specific data will remain, but user's data
     * (such as first name, custom data,...) will be wiped out.
     * <br>
     * If you log out user, there is no mechanism to log him in again since he's already subscribed for broadcast notifications from your app,
     * but you might want to sync new user data to target this user specifically.
     * <br>
     * Use this method if:
     * <ul>
     * <li>you're syncing user data to our server</li>
     * <li>your application has logout option</li>
     * <li>you don't want new logged in user to be targeted by other user's data, e.g. first name</li>
     * <li>you want logged out user to still receive broadcast notifications (if not, you need to call {@link #disablePushRegistration()})</li>
     * </ul>
     * <br>
     * This method can be called in offline mode. In this case library will return {@link SuccessPending#Pending} and will proceed with logout when network becomes available, {@link Event#USER_LOGGED_OUT} will be produced upon success.
     *
     * @param listener listener to report the result on
     * @see ResultListener
     * @see Event#USER_LOGGED_OUT
     */
    public abstract void logout(ResultListener<SuccessPending> listener);

    /**
     * Send mobile originated messages.
     * <br>
     * Destination for each message is set inside {@link Message}.
     *
     * @param messages messages to send
     */
    public abstract void sendMessages(Message... messages);

    /**
     * Send mobile originated messages.
     * <br>
     * Destination for each message is set inside {@link Message}.
     * The result of fetch operation will be provided via listener.
     * {@link ResultListener#onResult(Object)} will be called both in case of success and error,
     * separate status for each message can be retrieved via {@link Message#getStatus()} and {@link Message#getStatusMessage()}.
     *
     * @param listener listener to invoke when the operation is complete
     * @param messages messages to send
     * @see ResultListener
     */
    public abstract void sendMessages(ResultListener<Message[]> listener, Message... messages);

    /**
     * This method allows you to configure this device as primary among other devices of a single user.
     * SDK will try to set this device as primary on a server and report result through the listener.
     * @param isPrimary set to true to make this device primary or to false otherwise.
     * @param listener listener to invoke when the operation is complete.
     */
    public abstract void setAsPrimaryDevice(boolean isPrimary, ResultListener<Boolean> listener);

    /**
     * This method allows you to configure this device as primary among others devices of a single user.
     * Use this method to let SDK decide when it is best time to try to send request to server.
     * @param isPrimary set to true to make this device primary or to false otherwise.
     */
    public abstract void setAsPrimaryDevice(boolean isPrimary);

    /**
     * Use this method to determine if this device is currently primary device or not.
     * @return true if this device is primary or false otherwise. The value represents latest value synchronized with the server.
     */
    public abstract boolean isPrimaryDevice();

    /**
     * Use this method to determine if this device is currently primary device or not. Will trigger communication with server to get latest setting.
     */
    public abstract void getPrimaryDeviceSetting(ResultListener<Boolean> listener);

    /**
     * Use this method to trigger communication between SDK and server and to sync current primary setting for this device.
     * <br>It will later on trigger {@link Event#PRIMARY_CHANGED} if the setting will be updated locally in SDK.
     * <br><b>Note:</b> multiple invocations of this method within short period of time may not necessarily result in multiple calls to server,
     * they will rather be optimized and throttled and number of network calls will be reduced by the library.
     *
     * @deprecated Use {@link MobileMessaging#getPrimaryDeviceSetting(ResultListener)} instead.
     */
    @Deprecated
    public abstract void syncPrimaryDeviceSettingWithServer();

    /**
     * Deletes SDK data related to current application code (also, deletes data for other modules: geo, interactive, chat).
     * There might be a situation where you'll want to switch between different Application Codes during development/testing.
     * If you disable the Application Code storing {@link Builder#withoutStoringApplicationCode(ApplicationCodeProvider)},
     * the SDK won't detect the Application Code changes, thus won't cleanup the old Application Code related data.
     * In this case you should manually invoke cleanup() prior to {@link Builder#build()} otherwise the SDK will not
     * detect Application Code changes.
     */
    public abstract void cleanup();

    /**
     * Retrieves unique push registration identifier issued by server. This identifier matches one to one with FCM(GCM) cloud token
     * of the particular application installation. This identifier is only available after {@link Event#REGISTRATION_CREATED}
     * and does not change for the whole lifetime of the application installation.
     *
     * @return unique push registration id, null if no registration is available yet
     */
    public abstract String getPushRegistrationId();

    /**
     * Default result listener interface for asynchronous operations.
     *
     * @param <T> type of successful result
     */
    public static abstract class ResultListener<T> {
        /**
         * This method is invoked on listener in two cases:
         * <ol>
         * <li>communication with server ended successfully;</li>
         * <li>it is not possible to communicate with server now, but the data is saved inside library and will be sent as soon as possible.</li>
         * </ol>
         *
         * @param result the result of operation
         */
        public abstract void onResult(T result);

        /**
         * This method is invoked on listener in case of error.
         *
         * @param e object that contains error description
         */
        public void onError(MobileMessagingError e) {

        }
    }

    /**
     * Listener for initialization errors.
     */
    public interface InitListener {

        /**
         * This method is called when initialization succeeds
         */
        void onSuccess();

        /**
         * This method is invoked on listener when there's an unrecoverable error.
         *
         * @param e internal SDK error describing the problem, see {@link InternalSdkError}
         * @param googleErrorCode optional error code provided by play services
         */
        void onError(InternalSdkError e, @Nullable Integer googleErrorCode);
    }

    /**
     * The {@link MobileMessaging} builder class.
     *
     * @author mstipanov
     * @see MobileMessaging
     * @see NotificationSettings.Builder
     * @see NotificationSettings
     * @see Builder#withGcmSenderId(String)
     * @see Builder#withApiUri(String)
     * @see Builder#withMessageStore(Class)
     * @see Builder#withoutMessageStore()
     * @see Builder#withApplicationCode(String)
     * @see Builder#withoutStoringApplicationCode(ApplicationCodeProvider)
     * @see Builder#withDisplayNotification(NotificationSettings)
     * @see Builder#withoutDisplayNotification()
     * @see Builder#withoutStoringUserData()
     * @see Builder#withoutCarrierInfo()
     * @see Builder#withoutSystemInfo()
     * @see Builder#withoutMarkingSeenOnNotificationTap()
     * @since 29.02.2016.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static final class Builder {
        private final Application application;
        private String gcmSenderId = (String) MobileMessagingProperty.GCM_SENDER_ID.getDefaultValue();
        private String applicationCode = (String) MobileMessagingProperty.APPLICATION_CODE.getDefaultValue();
        private String apiUri = (String) MobileMessagingProperty.API_URI.getDefaultValue();
        private NotificationSettings notificationSettings = null;
        private boolean reportCarrierInfo = true;
        private boolean reportSystemInfo = true;
        private boolean geofencingActivated = false;
        private boolean doMarkSeenOnNotificationTap = true;
        private boolean shouldSaveUserData = true;
        private boolean storeAppCodeOnDisk = true;
        private ApplicationCodeProvider applicationCodeProvider = null;

        @SuppressWarnings("unchecked")
        private Class<? extends MessageStore> messageStoreClass = (Class<? extends MessageStore>) MobileMessagingProperty.MESSAGE_STORE_CLASS.getDefaultValue();

        public Builder(Application application) {
            if (null == application) {
                throw new IllegalArgumentException("application object is mandatory!");
            }
            this.application = application;

            loadDefaultApiUri(application);
            loadGcmSenderId(application);
            loadApplicationCode(application);
            loadNotificationSettings(application);
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

        private void validateApplicationCodeAvailability() {
            if (applicationCode != null && storeAppCodeOnDisk) {
                return;
            }

            if (applicationCodeProvider != null && !storeAppCodeOnDisk) {
                return;
            }

            throw new IllegalArgumentException("Application code is not provided to the library, make sure it is available in resources, builder or via app code provider");
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
            this.storeAppCodeOnDisk = true;
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
         * @param applicationCodeProvider resolves provided application code. Should be implemented as a separate class.
         *                                If you don't have Application code, you should resolve one
         *                                <a href="https://portal.infobip.com/push/applications">here</a>
         * @return {@link Builder}
         * @throws IllegalArgumentException when {@link ApplicationCodeProvider} is implemented in Activity class
         */
        public Builder withoutStoringApplicationCode(ApplicationCodeProvider applicationCodeProvider) {
            Exception exception = null;
            try {
                application.getClassLoader().loadClass(applicationCodeProvider.getClass().getCanonicalName());
            } catch (ClassNotFoundException | NullPointerException e) {
                exception = e;
            }

            if (exception != null || applicationCodeProvider instanceof Activity)
                throw new IllegalArgumentException("Application code provider should be implemented in a separate class file " +
                        "that implements ApplicationCodeProvider!");

            validateWithParam(applicationCodeProvider);
            this.storeAppCodeOnDisk = false;
            this.applicationCodeProvider = applicationCodeProvider;
            return this;
        }

        /**
         * It will configure the system to use a custom API endpoint.
         * <pre>
         * {@code new MobileMessaging.Builder(application)
         *       .withApiUri("http://127.0.0.1")
         *       .build();
         * }
         * </pre>
         * <br>
         * The default is set to <a href="https://mobile.infobip.com">https://mobile.infobip.com</a>.
         * <br>
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
         * {@code new MobileMessaging.Builder(application)
         *       .withDisplayNotification(
         *           new NotificationSettings.Builder(application)
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
         * {@code new MobileMessaging.Builder(application)
         *       .withoutDisplayNotification()
         *       .build();
         * }
         * </pre>
         * <br>
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
         * {@code new MobileMessaging.Builder(application)
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
         * {@code new MobileMessaging.Builder(application)
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
         * It will not send mobile network carrier info to the server.
         * <pre>
         * {@code new MobileMessaging.Builder(application)
         *       .withoutCarrierInfo()
         *       .build();}
         * </pre>
         *
         * @return {@link Builder}
         */
        public Builder withoutCarrierInfo() {
            this.reportCarrierInfo = false;
            return this;
        }

        /**
         * It will not send system information to the server.
         * <pre>
         * {@code new MobileMessaging.Builder(application)
         *       .withoutSystemInfo()
         *       .build();}
         * </pre>
         *
         * @return {@link Builder}
         */
        public Builder withoutSystemInfo() {
            this.reportSystemInfo = false;
            return this;
        }

        /**
         * It will check the presence of the required {@link Manifest.permission#READ_PHONE_STATE} permission needed for acquiring
         * network information on all Android devices as some devices require the permission to be able to get SIM operator data
         * (needed for SIM country and network code).
         * <p>
         *    <b>Note:</b> not using this method will result with some devices not being able to sync all network info.
         *
         * @return {@link Builder}
         */
        @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
        public Builder withMobileNetworkInfoOnAllDevices() {
            return this;
        }

        /**
         * It will not mark message as seen after user tapped on the notification.
         * <pre>
         * {@code new MobileMessaging.Builder(application)
         *       .withoutMarkingSeenOnNotificationTap()
         *       .build();}
         * </pre>
         *
         * @return {@link Builder}
         */
        public Builder withoutMarkingSeenOnNotificationTap() {
            this.doMarkSeenOnNotificationTap = false;
            return this;
        }

        /**
         * It will not store {@link UserData} on device.
         * <p>
         * <b>Note:</b> since {@link UserData} is not stored on device, automatic retries will not be applied.
         * It should be handled manually using {@link MobileMessaging#syncUserData(UserData, ResultListener)}} method,
         * where you can check error in callback and retry accordingly.
         * <pre>
         * {@code new MobileMessaging.Builder(application)
         *       .withoutStoringUserData()
         *       .build();}
         * </pre>
         *
         * @return {@link Builder}
         */
        public Builder withoutStoringUserData() {
            this.shouldSaveUserData = false;
            return this;
        }

        /**
         * Builds the <i>MobileMessaging</i> configuration. Registration token sync is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @return {@link MobileMessaging}
         */
        public MobileMessaging build() {
            return build(null);
        }

        /**
         * Builds the <i>MobileMessaging</i> configuration. Registration token sync is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @param initListener provide listener to handle any errors during intialization
         *
         * @return {@link MobileMessaging}
         */
        public MobileMessaging build(@Nullable InitListener initListener) {
            validateApplicationCodeAvailability();

            MobileMessagingCore.setApiUri(application, apiUri);
            MobileMessagingCore.setGcmSenderId(application, gcmSenderId);
            MobileMessagingCore.setMessageStoreClass(application, messageStoreClass);
            MobileMessagingCore.setReportCarrierInfo(application, reportCarrierInfo);
            MobileMessagingCore.setReportSystemInfo(application, reportSystemInfo);
            MobileMessagingCore.setDoMarkSeenOnNotificationTap(application, doMarkSeenOnNotificationTap);
            MobileMessagingCore.setShouldSaveUserData(application, shouldSaveUserData);
            MobileMessagingCore.setShouldSaveAppCode(application, storeAppCodeOnDisk);

            MobileMessagingCore.Builder mobileMessagingCoreBuilder = new MobileMessagingCore.Builder(application)
                    .withDisplayNotification(notificationSettings);

            if (storeAppCodeOnDisk) {
                mobileMessagingCoreBuilder.withApplicationCode(applicationCode);
            } else if (applicationCodeProvider != null) {
                mobileMessagingCoreBuilder.withApplicationCode(applicationCodeProvider);
            }

            return mobileMessagingCoreBuilder.build(initListener);
        }
    }
}
