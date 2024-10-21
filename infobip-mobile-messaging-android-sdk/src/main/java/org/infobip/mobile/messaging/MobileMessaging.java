package org.infobip.mobile.messaging;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.Cryptor;
import org.infobip.mobile.messaging.util.CryptorImpl;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.List;

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
 * @see Builder#withoutDisplayNotification()
 * @see Builder#withMessageStore(Class)
 * @see Builder#withFullFeaturedInApps()
 * @see Builder#withApiUri(String)
 * @see Builder#withApplicationCode(String)
 * @see Builder#withDisplayNotification(NotificationSettings)
 * @see Builder#withoutMessageStore()
 * @see Builder#withoutCarrierInfo()
 * @see Builder#withoutSystemInfo()
 * @since 29.02.2016.
 */
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
     * Reports delivery of messages to Mobile Messaging servers.
     * <br>
     * This method has to be used only if you handle FCM message notifications
     * without Mobile Messaging library. In all other cases the library will
     * send delivery report automatically whenever FCM push is delivered to device.
     *
     * @param messageIds ids of messages to report delivery for
     * @see Event#DELIVERY_REPORTS_SENT
     */
    public abstract void setMessagesDelivered(final String... messageIds);

    /**
     * Reports seen status of messages to Mobile Messaging servers asynchronously. If something went wrong, the library will repeat the request until it reaches the server.
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
     * Asynchronously saves changed installation on the server.
     * <p>
     * This method will synchronize new installation data (such as setting of primary device, application user ID, custom attributes...) with server
     * and will also trigger {@link Event#INSTALLATION_UPDATED} event with the currently available data in local cache for this installation.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param installation installation object with desired changes
     * @see Event#INSTALLATION_UPDATED
     */
    public abstract void saveInstallation(@NonNull Installation installation);

    /**
     * Asynchronously saves changed installation on the server.
     * <p>
     * This method will save new installation data (such as setting of primary device, application user ID, custom attributes...) with server
     * and will also trigger {@link Event#INSTALLATION_UPDATED} event with the currently available data in local cache for this installation. The result will be provided via listener.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param installation installation object with desired changes
     * @param listener     listener to report the result on
     * @see ResultListener
     * @see Event#INSTALLATION_UPDATED
     */
    public abstract void saveInstallation(@NonNull Installation installation, ResultListener<Installation> listener);

    /**
     * Asynchronously fetches the installation data from the server.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param listener listener to report the result on
     * @see ResultListener
     */
    public abstract void fetchInstallation(ResultListener<Installation> listener);

    /**
     * Synchronously retrieves current installation data stored locally such as push registration ID, language, push token, etc.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @return installation installation data object with locally stored data
     */
    public abstract Installation getInstallation();

    /**
     * Asynchronously configures some other device as primary among others devices of a single user.
     * <br>
     * Use this method to let SDK decide when it is best time to try to send request to server. The result will be provided via listener.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param pushRegistrationId set the push registration ID to make some other device installation a primary one
     * @param isPrimary          set to true to make the provided installation as primary or to false otherwise
     * @param listener           listener to report the result on
     */
    public abstract void setInstallationAsPrimary(@NonNull String pushRegistrationId, boolean isPrimary, ResultListener<List<Installation>> listener);

    /**
     * Asynchronously configures this device as primary among others devices of a single user.
     * <br>
     * Use this method to let SDK decide when it is best time to try to send request to server.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param pushRegistrationId set the push registration ID to make some other device installation a primary one.
     * @param isPrimary          set to true to make the provided installation as primary or to false otherwise.
     */
    public abstract void setInstallationAsPrimary(@NonNull String pushRegistrationId, boolean isPrimary);

    /**
     * Asynchronously saves changed user data on the server.
     * <br>
     * This method will save new data on server and will also trigger {@link Event#USER_UPDATED}
     * with the currently available data in local cache for this user.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param user user data object with desired changes
     * @see Event#USER_UPDATED
     */
    public abstract void saveUser(@NonNull User user);

    /**
     * Asynchronously saves changed user data on the server.
     * <br>
     * This method will save new data with server. The result will be provided via listener.
     * It will also trigger {@link Event#USER_UPDATED} with the currently available data in local cache for this user.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param user     user data object with desired changes
     * @param listener listener to report the result on
     * @see ResultListener
     * @see Event#USER_UPDATED
     */
    public abstract void saveUser(@NonNull User user, ResultListener<User> listener);

    /**
     * Asynchronously fetches user data from the server.
     * <br>
     * The result of fetching operation will be provided via listener with all the data currently available on a server for this user.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param listener listener to report the result on
     * @see ResultListener
     */
    public abstract void fetchUser(@NonNull ResultListener<User> listener);

    /**
     * Synchronously retrieves current user data stored locally such as tags, emails, first name, etc.
     *
     * @return last synchronized {@link User} object
     */
    @Nullable
    public abstract User getUser();

    /**
     * Asynchronously personalizes current installation with a person on the server.
     * <br>
     * Each user can have Phone numbers, Emails and External user ID. These fields are unique identifiers of a user profile on Infobip platform
     * and provide capability to personalize any app installation with a user profile. The platform provides data grouping functions based on these parameters.
     * For example, if two installations of a particular app will try to save the same Phone number, then both of them will be collected under a single user.
     * Phone number, Email and External user ID are also widely used when targeting users with messages across different channels via Infobip platform.
     * <p>
     * <b>NOTE:</b> This API doesn't depersonalize current installation from any person that it may be currently personalized with. In order to depersonalize
     * current possible person from current installation and personalize it with another person at once, use another API
     * {@link MobileMessaging#personalize(UserIdentity, UserAttributes, boolean, ResultListener)}
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param userIdentity   required combination of phones, emails and an external user id that will form a unique key for a person
     * @param userAttributes optional user data to be saved for the person
     */
    public abstract void personalize(@NonNull UserIdentity userIdentity, @Nullable UserAttributes userAttributes);

    /**
     * Asynchronously personalizes current installation with a person on the server.
     * <br>
     * Each user can have Phone numbers, Emails and External user ID. These fields are unique identifiers of a user profile on Infobip platform
     * and provide capability to personalize any app installation with a user profile. The platform provides data grouping functions based on these parameters.
     * For example, if two installations of a particular app will try to save the same Phone number, then both of them will be collected under a single user.
     * Phone number, Email and External user ID are also widely used when targeting users with messages across different channels via Infobip platform.
     * <p>
     * <b>NOTE:</b> This API doesn't depersonalize current installation from any person that it may be currently personalized with. In order to depersonalize
     * current possible person from current installation and personalize it with another person at once, use another API
     * {@link MobileMessaging#personalize(UserIdentity, UserAttributes, boolean, ResultListener)}
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param userIdentity   required combination of phones, emails and an external user id that will form a unique key for a person
     * @param userAttributes optional user data to be saved for the person
     * @param listener       listener to report the result on
     */
    public abstract void personalize(@NonNull UserIdentity userIdentity, @Nullable UserAttributes userAttributes, ResultListener<User> listener);

    /**
     * Asynchronously personalizes current installation with a person on the server.
     * <br>
     * Each user can have Phone numbers, Emails and External user ID. These fields are unique identifiers of a user profile on Infobip platform
     * and provide capability to personalize any app installation with a user profile. The platform provides data grouping functions based on these parameters.
     * For example, if two installations of a particular app will try to save the same Phone number, then both of them will be collected under a single user.
     * Phone number, Email and External user ID are also widely used when targeting users with messages across different channels via Infobip platform.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param userIdentity       required combination of phones, emails and an external user id that will form a unique key for a person
     * @param userAttributes     optional user data to be saved for the person
     * @param forceDepersonalize determines whether or not the depersonalization should be performed on our server in order to depersonalize the installation from previous user profile
     */
    public abstract void personalize(@NonNull UserIdentity userIdentity, @Nullable UserAttributes userAttributes, boolean forceDepersonalize);

    /**
     * Asynchronously personalizes current installation with a person on the server.
     * <br>
     * Each user can have Phone numbers, Emails and External user ID. These fields are unique identifiers of a user profile on Infobip platform
     * and provide capability to personalize any app installation with a user profile. The platform provides data grouping functions based on these parameters.
     * For example, if two installations of a particular app will try to save the same Phone number, then both of them will be collected under a single user.
     * Phone number, Email and External user ID are also widely used when targeting users with messages across different channels via Infobip platform.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param userIdentity       required combination of phones, emails and an external user id that will form a unique key for a person
     * @param userAttributes     optional user data to be saved for the person
     * @param forceDepersonalize determines whether or not the depersonalization should be performed on our server in order to depersonalize the installation from previous user profile
     * @param listener           listener to report the result on
     */
    public abstract void personalize(@NonNull UserIdentity userIdentity, @Nullable UserAttributes userAttributes, boolean forceDepersonalize, ResultListener<User> listener);

    /**
     * Asynchronously personalizes current installation with a person on the server.
     * <br>
     * Each user can have Phone numbers, Emails and External user ID. These fields are unique identifiers of a user profile on Infobip platform
     * and provide capability to personalize any app installation with a user profile. The platform provides data grouping functions based on these parameters.
     * For example, if two installations of a particular app will try to save the same Phone number, then both of them will be collected under a single user.
     * Phone number, Email and External user ID are also widely used when targeting users with messages across different channels via Infobip platform.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param userIdentity       required combination of phones, emails and an external user id that will form a unique key for a person
     * @param userAttributes     optional user data to be saved for the person
     * @param forceDepersonalize determines whether or not the depersonalization should be performed on our server in order to depersonalize the installation from previous user profile
     * @param keepAsLead         you can set this parameter to true If you want to prevent the promotion from Lead to Customer, only for specific use cases where the default behaviour wants to be avoided
     */
    public abstract void personalize(@NonNull UserIdentity userIdentity, @Nullable UserAttributes userAttributes, boolean forceDepersonalize, boolean keepAsLead);

    /**
     * Asynchronously personalizes current installation with a person on the server.
     * <br>
     * Each user can have Phone numbers, Emails and External user ID. These fields are unique identifiers of a user profile on Infobip platform
     * and provide capability to personalize any app installation with a user profile. The platform provides data grouping functions based on these parameters.
     * For example, if two installations of a particular app will try to save the same Phone number, then both of them will be collected under a single user.
     * Phone number, Email and External user ID are also widely used when targeting users with messages across different channels via Infobip platform.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param userIdentity       required combination of phones, emails and an external user id that will form a unique key for a person
     * @param userAttributes     optional user data to be saved for the person
     * @param forceDepersonalize determines whether or not the depersonalization should be performed on our server in order to depersonalize the installation from previous user profile
     * @param keepAsLead         you can set this parameter to true If you want to prevent the promotion from Lead to Customer, only for specific use cases where the default behaviour wants to be avoided
     * @param listener           listener to report the result on
     */
    public abstract void personalize(@NonNull UserIdentity userIdentity, @Nullable UserAttributes userAttributes, boolean forceDepersonalize, boolean keepAsLead, ResultListener<User> listener);

    /**
     * Asynchronously erases currently stored {@link User} on SDK and server associated with push registration, along with messages in SDK storage (also, deletes data for chat module).
     * <p>
     * {@link User}'s data synced over MobileMessaging is by default associated with created push registration. Depersonalizing an installation means that a push registration and
     * device specific data will remain, but user's data (such as first name, custom data, ...) will be wiped out.
     * <br>
     * If you depersonalize an installation from person, there is a way to personalize it again by providing new user data (either by {@link #saveUser(User)} setters or
     * {@link #personalize(UserIdentity, UserAttributes, boolean, ResultListener)} method) in order to target this user specifically.
     * <p>
     * Use this method in following cases:
     * <ul>
     * <li>you want to handle possible failures of server depersonalize request, retry and maintain pending depersonalize state by yourself</li>
     * <li>you're syncing user data to our server</li>
     * <li>your application has logout functionality</li>
     * <li>you don't want new personalized installation to be targeted by other user's data, e.g. first name</li>
     * <li>you want to depersonalize installation from user and still be able to receive broadcast notifications (otherwise, you need to disable push registration via {@link Installation#setPushRegistrationEnabled(Boolean)}
     * with <i>false</i> value to disable all messages and {@link MobileMessaging#saveInstallation(Installation)} to update it to the server</li>
     * </ul>
     *
     * @see Event#DEPERSONALIZED
     */
    public abstract void depersonalize();

    /**
     * Asynchronously erases currently stored {@link User} on SDK and server associated with push registration, along with messages in SDK storage (also, deletes data for chat module).
     * <p>
     * {@link User}'s data synced over MobileMessaging is by default associated with created push registration. Depersonalizing an installation means that a push registration and
     * device specific data will remain, but user's data (such as first name, custom data, ...) will be wiped out.
     * <br>
     * If you depersonalize an installation from person, there is a way to personalize it again by providing new user data (either by {@link #saveUser(User)} setters or
     * {@link #personalize(UserIdentity, UserAttributes, boolean, ResultListener)} method) in order to target this user specifically.
     * <p>
     * <b>NOTE:</b> There is another version of depersonalize method that doesn't require listener parameter which means the SDK will handle any unsuccessful depersonalize
     * requests by itself. See the method documentation for more details.
     * <p>
     * Use this method in following cases:
     * <ul>
     * <li>you want to handle possible failures of server depersonalize request, retry and maintain pending depersonalize state by yourself</li>
     * <li>you're syncing user data to our server</li>
     * <li>your application has logout functionality</li>
     * <li>you don't want new personalized installation to be targeted by other user's data, e.g. first name</li>
     * <li>you want to depersonalize installation from user and still be able to receive broadcast notifications (otherwise, you need to disable push registration via {@link Installation#setPushRegistrationEnabled(Boolean)}
     * with <i>false</i> value to disable all messages and {@link MobileMessaging#saveInstallation(Installation)} to update it to the server</li>
     * </ul>
     * <p>
     * This method can be called in offline mode. In this case library will return {@link SuccessPending#Pending} and will proceed with depersonalize when network becomes available,
     * {@link Event#DEPERSONALIZED} will be produced upon success.
     *
     * @param listener listener to report the result on
     * @see ResultListener
     * @see Event#DEPERSONALIZED
     */
    public abstract void depersonalize(ResultListener<SuccessPending> listener);

    /**
     * Asynchronously depersonalizes some other device among others devices of a single user.
     * <p>
     * For more information and examples see: <a href=https://github.com/infobip/mobile-messaging-sdk-android/wiki/Users-and-installations>Users and installations</a>
     *
     * @param pushRegistrationId push registration ID of the installation to be depersonalized
     * @param listener           listener to report the result on
     */
    public abstract void depersonalizeInstallation(@NonNull String pushRegistrationId, ResultListener<List<Installation>> listener);

    /**
     * Asynchronously submits custom events without validation. Custom event should be registered by definition ID and optional properties.
     * Validation will not be performed. If wrong definition is provided event will be considered as invalid and won't be visible on user.
     * <p>
     * This method will report custom event on server and will also trigger {@link Event#CUSTOM_EVENTS_SENT} event with the provided event.
     *
     * @param customEvent custom event to report
     * @see Event#CUSTOM_EVENTS_SENT
     */
    public abstract void submitEvent(@NonNull CustomEvent customEvent);

    /**
     * Synchronously submits custom event and validates it on backend. Custom event should be registered by definition ID and optional properties.
     * In case of validation or network issues error will be returned and you'd need to manually retry sending of the event.
     * <p>
     * This method will report and validate custom event on server and will also trigger {@link Event#CUSTOM_EVENTS_SENT} event with the provided event.
     *
     * @param customEvent custom event to report
     * @param listener    listener to report the result on
     * @see ResultListener
     * @see Event#CUSTOM_EVENTS_SENT
     */
    public abstract void submitEvent(@NonNull CustomEvent customEvent, ResultListener<CustomEvent> listener);

    /**
     * Send mobile originated messages.
     * <p>
     * Destination for each message is set inside {@link Message}.
     *
     * @param messages messages to send
     */
    public abstract void sendMessages(Message... messages);

    /**
     * Send mobile originated messages.
     * <p>
     * Destination for each message is set inside {@link Message}.
     * The result of fetchInstance operation will be provided via listener.
     * {@link ResultListener#onResult(Result)}} will be called both in case of success and error,
     * separate status for each message can be retrieved via {@link Message#getStatus()} and {@link Message#getStatusMessage()}.
     *
     * @param listener listener to invoke when the operation is complete
     * @param messages messages to send
     * @see ResultListener
     */
    public abstract void sendMessages(ResultListener<Message[]> listener, Message... messages);

    /**
     * Call this method to initiate the registration for Push Notification service.
     * User will be prompted to allow receiving Push Notifications.
     * Should be used together with {@link Builder#withoutRegisteringForRemoteNotifications()} builder method.
     *
     * <pre>
     * {@code
     *   MobileMessaging.registerForRemoteNotifications()
     * }
     * </pre>
     * <br>
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    public abstract void registerForRemoteNotifications();


    /**
     * Synchronously cleans up all persisted data.
     * This method deletes SDK data related to current application code (also, deletes data for other modules: interactive, chat).
     * There might be a situation where you'll want to switch between different Application Codes during development/testing.
     * If you disable the Application Code storing {@link Builder#withoutStoringApplicationCode(ApplicationCodeProvider)},
     * the SDK won't detect the Application Code changes, thus won't cleanup the old Application Code related data.
     * In this case you should manually invoke cleanup() prior to {@link Builder#build()} otherwise the SDK will not
     * detect Application Code changes.
     */
    public abstract void cleanup();

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
        public abstract void onResult(Result<T, MobileMessagingError> result);
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
         * @param e               internal SDK error describing the problem, see {@link InternalSdkError}
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
     * @see Builder#withApiUri(String)
     * @see Builder#withMessageStore(Class)
     * @see Builder#withFullFeaturedInApps()
     * @see Builder#withoutMessageStore()
     * @see Builder#withApplicationCode(String)
     * @see Builder#withoutStoringApplicationCode(ApplicationCodeProvider)
     * @see Builder#withDisplayNotification(NotificationSettings)
     * @see Builder#withoutRegisteringForRemoteNotifications()
     * @see Builder#withoutDisplayNotification()
     * @see Builder#withoutStoringUserData()
     * @see Builder#withoutCarrierInfo()
     * @see Builder#withoutSystemInfo()
     * @see Builder#withoutMarkingSeenOnNotificationTap()
     * @see Builder#withFirebaseOptions(FirebaseOptions)
     * @since 29.02.2016.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static final class Builder {
        private final Application application;
        private String applicationCode = (String) MobileMessagingProperty.APPLICATION_CODE.getDefaultValue();
        private String apiUri;
        private NotificationSettings notificationSettings = null;
        private boolean reportCarrierInfo = true;
        private boolean reportSystemInfo = true;
        private boolean doMarkSeenOnNotificationTap = true;
        private boolean shouldSaveUserData = true;
        private boolean storeAppCodeOnDisk = true;
        private boolean allowUntrustedSSLOnError = false;
        private boolean usePrivateSharedPrefs = true;
        private boolean postNotificationPermissionRequest = true;
        private boolean fullFeaturedInApps = false;
        private ApplicationCodeProvider applicationCodeProvider = null;
        private FirebaseOptions firebaseOptions = null;
        private Cryptor oldCryptor = null;

        @SuppressWarnings("unchecked")
        private Class<? extends MessageStore> messageStoreClass = (Class<? extends MessageStore>) MobileMessagingProperty.MESSAGE_STORE_CLASS.getDefaultValue();

        public Builder(Application application) {
            if (null == application) {
                throw new IllegalArgumentException("application object is mandatory!");
            }
            this.application = application;

            loadDefaultApiUri(application);
            final String applicationCode = MobileMessagingCore.getApplicationCodeFromResources(application);
            if (StringUtils.isNotBlank(applicationCode)) {
                this.applicationCode = applicationCode;
            }
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

            throw new IllegalArgumentException("Application code is not provided to MobileMessaging library, make sure it is available in resources, builder or via app code provider");
        }

        /**
         * @deprecated Starting from the version 6.0.0 either provide values for registering in Firebase using google-services.json or set them in strings.xml
         */
        @Deprecated
        public Builder withSenderId(String senderId) {
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
         * When you want to use the Application code that is not stored to <i>infobip_application_code</i> string resource.
         * <br>By default it will use <i>infobip_application_code</i> string resource
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
         * provide it on demand. For example, you should implement <b>patch</b> API call to your server where you store required
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
         * It will fail if set to null or empty string.
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
         * MobileMessaging SDK by default registers for remote notifications during `MobileMessaging.build()` procedure. It is possible to disable this default behavior.
         * This might be needed in case your app should support other push notifications vendors in addition to (or instead of) Infobip's one,
         * or you want to have a more flexible approach of when and where the user will be prompted to allow receiving Push Notifications.
         * - remark: Don't forget to register for Push Notifications explicitly by calling `MobileMessaging.registerForRemoteNotifications()`.
         * <pre>
         * {@code new MobileMessaging.Builder(application)
         *       .withoutRegisteringForRemoteNotifications()
         *       .build();
         * }
         * </pre>
         * <br>
         *
         * @return {@link Builder}
         * @see #registerForRemoteNotifications()
         */
        public Builder withoutRegisteringForRemoteNotifications() {
            this.postNotificationPermissionRequest = false;
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
         * Use this method to enable Full-featured In-App notifications <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/In-app-notifications#full-featured-in-app-notifications">(more about this feature)</a>
         * Without calling this method, event (<a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/Library-events#message_received">MESSAGE_RECEIVED </a>) is triggered, but In-App message not displayed within WebView.
         *
         * @return {@link Builder}
         */
        public Builder withFullFeaturedInApps() {
            this.fullFeaturedInApps = true;
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
         * <b>Note:</b> not using this method will result with some devices not being able to patch all network info.
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
         * It will not store {@link User} on device.
         * <p>
         * <b>Note:</b> since {@link User} is not stored on device, automatic retries will not be applied.
         * It should be handled manually using {@link MobileMessaging#saveUser(User, ResultListener)}} method,
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
         * Set `allowUntrustedSSLOnError` to true to allow connections to untrusted hosts when SSL error happens.
         * <br>Regardless of the setting, SDK will first try to connect in ordinary mode.
         *
         * @param allowUntrustedSSLOnError setting to control SSL connections
         * @return {@link Builder}
         */
        public Builder withHttpSettings(boolean allowUntrustedSSLOnError) {
            this.allowUntrustedSSLOnError = allowUntrustedSSLOnError;
            return this;
        }

        /**
         * It will migrate all MobileMessaging data from public shared preferences to private storage under `MobileMessagingSDK` name maintaining the
         * new private storage. Old public prefs will be deleted.
         * <p>
         * <b>NOTE:</b> switching from using this library version with private shared prefs back to MM SDK version < 2.2.0 is not backwards compatible
         * since older SDK versions use just public prefs by default (and by using this method only private prefs are maintained)
         *
         * <pre>
         * {@code new MobileMessaging.Builder(application)
         *       .withPrivateSharedPrefs()
         *       .build();}
         * </pre>
         *
         * @return {@link Builder}
         * <p>
         * Deprecated, preferences are private by default.
         */
        @Deprecated
        public Builder withPrivateSharedPrefs() {
            this.usePrivateSharedPrefs = true;
            return this;
        }

        /**
         * This method will migrate data, encrypted with old unsecure algorithm (ECB) to new one {@link CryptorImpl} (CBC).
         * If you have installations of the application with MobileMessaging SDK version < 5.0.0,
         * use this method with providing old cryptor, so MobileMessaging SDK will migrate data using the new cryptor.
         * For code snippets (old cryptor implementation) and more details - <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/ECB-Cryptor-migration">check docs on GitHub</a>.
         *
         * @param oldCryptor, provide old cryptor, to migrate encrypted data to new one {@link CryptorImpl}.
         * @return {@link Builder}
         */
        public Builder withCryptorMigration(Cryptor oldCryptor) {
            this.oldCryptor = oldCryptor;
            return this;
        }

        /**
         * Builds the <i>MobileMessaging</i> configuration. Registration token patch is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @return {@link MobileMessaging}
         */
        public MobileMessaging build() {
            return build(null);
        }

        /**
         * Builds the <i>MobileMessaging</i> configuration. Registration token patch is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @param initListener provide listener to handle any errors during initialization
         * @return {@link MobileMessaging}
         */
        public MobileMessaging build(@Nullable InitListener initListener) {
            validateApplicationCodeAvailability();

            MobileMessagingCore.setMessageStoreClass(application, messageStoreClass);
            MobileMessagingCore.setReportCarrierInfo(application, reportCarrierInfo);
            MobileMessagingCore.setReportSystemInfo(application, reportSystemInfo);
            MobileMessagingCore.setDoMarkSeenOnNotificationTap(application, doMarkSeenOnNotificationTap);
            MobileMessagingCore.setRemoteNotificationsEnabled(application, postNotificationPermissionRequest);
            MobileMessagingCore.setFullFeatureInAppsEnabled(application, fullFeaturedInApps);
            MobileMessagingCore.setShouldSaveUserData(application, shouldSaveUserData);
            MobileMessagingCore.setShouldSaveAppCode(application, storeAppCodeOnDisk);
            MobileMessagingCore.setAllowUntrustedSSLOnError(application, allowUntrustedSSLOnError);
            MobileMessagingCore.setSharedPrefsStorage(application, usePrivateSharedPrefs);

            MobileMessagingCore.Builder mobileMessagingCoreBuilder = new MobileMessagingCore.Builder(application)
                    .withDisplayNotification(notificationSettings)
                    .withFirebaseOptions(firebaseOptions);

            if (oldCryptor != null) {
                mobileMessagingCoreBuilder.withCryptorMigration(oldCryptor);
            }
            if (storeAppCodeOnDisk) {
                mobileMessagingCoreBuilder.withApplicationCode(applicationCode);
            } else if (applicationCodeProvider != null) {
                mobileMessagingCoreBuilder.withApplicationCode(applicationCodeProvider);
            }

            MobileMessaging mmCore = mobileMessagingCoreBuilder.build(initListener);
            //must be called after build(), otherwise would be reset on appCode change
            MobileMessagingCore.setProvidedApiUri(application, apiUri);
            return mmCore;
        }
    }
}
