package org.infobip.mobile.messaging;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresPermission;

import org.infobip.mobile.messaging.geo.Geofencing;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * The main configuration class. It is used to configure and start the Mobile Messaging System.
 * <p/>
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
public class MobileMessaging {

    private static MobileMessaging instance;
    private final Context context;


    /**
     * Default constructor. Use MobileMessaging.Builder to construct MobileMessaging.
     *
     * @param context android context object.
     * @see MobileMessaging.Builder
     */
    private MobileMessaging(Context context) {
        this.context = context;
    }


    /**
     * Gets an instance of MobileMessaging after it is initialized via {@link MobileMessaging.Builder}.
     * </p>
     * If the app was killed and there is no instance available, it will return a temporary instance based on current context.
     *
     * @param context android context object.
     * @return instance of MobileMessaging.
     * @see MobileMessaging.Builder
     */
    public synchronized static MobileMessaging getInstance(Context context) {
        if (null != instance) {
            return instance;
        }

        return new MobileMessaging(context);
    }

    /**
     * Enables the push registration so that the application can receive push notifications
     * (regular push messages/geofencing campaign messages/messages fetched from the server).
     * MobileMessaging SDK has the push registration enabled by default.
     */
    public void enablePushRegistration() {
        MobileMessagingCore.getInstance(context).enablePushRegistration();
    }

    /**
     * Disables the push registration so that the application is no longer able to receive push notifications
     * through MobileMessaging SDK (regular push messages/geofencing campaign messages/messages fetched from the server).
     * MobileMessaging SDK has the push registration enabled by default.
     */
    public void disablePushRegistration() {
        MobileMessagingCore.getInstance(context).disablePushRegistration();
    }

    /**
     * Push registration status defines whether the device is allowed to receive push notifications
     * (regular push messages/geofencing campaign messages/messages fetched from the server).
     * MobileMessaging SDK has the push registration enabled by default.
     *
     * @return Current push registration status.
     */
    public boolean isPushRegistrationEnabled() {
        return MobileMessagingCore.getInstance(context).isPushRegistrationEnabled();
    }

    /**
     * Reports delivery of messages to Mobile Messaging servers.
     * </p>
     * This method has to be used only if you handle GCM message notifications
     * without Mobile Messaging library. In all other cases the library will
     * send delivery report automatically whenever GCM push is delivered to device.
     *
     * @param messageIds ids of messages to report delivery for
     * @see Event#DELIVERY_REPORTS_SENT
     */
    public void setMessagesDelivered(final String... messageIds) {
        MobileMessagingCore.getInstance(context).setMessagesDelivered(messageIds);
    }

    /**
     * Reports seen status of messages to Mobile Messaging servers.
     * </p>
     * This method shall be user to report seen status when user actually sees message content.
     *
     * @param messageIds message ids to report seen status for
     * @see Event#SEEN_REPORTS_SENT
     */
    public void setMessagesSeen(final String... messageIds) {
        MobileMessagingCore.getInstance(context).setMessagesSeen(messageIds);
    }

    /**
     * Returns instance of message store that is used within the library or null if message store is not set.
     *
     * @return instance of message store.
     * @see MessageStore
     */
    public MessageStore getMessageStore() {
        return MobileMessagingCore.getInstance(context).getMessageStore();
    }

    /**
     * Does a synchronization of user data with server.
     * </p>
     * This method will synchronize new data with server and will also trigger {@link Event#USER_DATA_REPORTED}
     * with all the data currently available on a server for this user.
     *
     * @param userData user data object with desired changes
     * @see Event#USER_DATA_REPORTED
     */
    public void syncUserData(UserData userData) {
        MobileMessagingCore.getInstance(context).syncUserData(userData, null);
    }

    /**
     * Does a synchronization of user data with server.
     * </p>
     * This method will synchronize new data with server. The result of synchronization will be provided via listener.
     * It will also trigger {@link Event#USER_DATA_REPORTED} with all the data currently available on a server for this user.
     *
     * @param userData user data object with desired changes
     * @param listener listener to report the result on
     * @see ResultListener
     * @see Event#USER_DATA_REPORTED
     */
    public void syncUserData(UserData userData, ResultListener<UserData> listener) {
        MobileMessagingCore.getInstance(context).syncUserData(userData, listener);
    }

    /**
     * Does a fetch of user data from the server.
     * </p>
     * This method will trigger {@link Event#USER_DATA_REPORTED} with all the data currently available on a server for this user.
     *
     * @see Event#USER_DATA_REPORTED
     */
    public void fetchUserData() {
        MobileMessagingCore.getInstance(context).syncUserData(null, null);
    }

    /**
     * Does a fetch of user data from the server.
     * </p>
     * The result of fetch operation will be provided via listener.
     * This method will also trigger {@link Event#USER_DATA_REPORTED} with all the data currently available on a server for this user.
     *
     * @see ResultListener
     * @see Event#USER_DATA_REPORTED
     */
    public void fetchUserData(ResultListener<UserData> listener) {
        MobileMessagingCore.getInstance(context).syncUserData(null, listener);
    }

    /**
     * Reads user data that is currently stored in the library.
     * </p>
     * This method does not trigger {@link Event#USER_DATA_REPORTED}.
     *
     * @return last synchronized UserData object
     */
    public UserData getUserData() {
        return MobileMessagingCore.getInstance(context).getUserData();
    }

    /**
     * Send mobile originated messages.
     * </p>
     * Destination for each message is set inside {@link Message}.
     *
     * @param messages messages to send
     */
    public void sendMessages(Message... messages) {
        MobileMessagingCore.getInstance(context).sendMessages(null, messages);
    }

    /**
     * Send mobile originated messages.
     * </p>
     * Destination for each message is set inside {@link Message}.
     * The result of fetch operation will be provided via listener.
     * {@link ResultListener#onResult(Object)} will be called both in case of success and error,
     * separate status for each message can be retrieved via {@link Message#getStatus()} and {@link Message#getStatusMessage()}.
     *
     * @param listener listener to invoke when the operation is complete
     * @param messages messages to send
     * @see ResultListener
     */
    public void sendMessages(ResultListener<Message[]> listener, Message... messages) {
        MobileMessagingCore.getInstance(context).sendMessages(listener, messages);
    }

    /**
     * Starts tracking geofence areas.
     *
     * @see Geofencing
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void activateGeofencing() {
        MobileMessagingCore.getInstance(context).activateGeofencing(Geofencing.getInstance(context));
    }

    /**
     * Stops tracking geofence areas.
     *
     * @see Geofencing
     */
    public void deactivateGeofencing() {
        MobileMessagingCore.getInstance(context).deactivateGeofencing();
    }

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
        public void onError(Throwable e) {
        }

        public abstract void onError(MobileMessagingError e);
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
        private final Application application;
        private String gcmSenderId = (String) MobileMessagingProperty.GCM_SENDER_ID.getDefaultValue();
        private String applicationCode = (String) MobileMessagingProperty.APPLICATION_CODE.getDefaultValue();
        private String apiUri = (String) MobileMessagingProperty.API_URI.getDefaultValue();
        private NotificationSettings notificationSettings = null;
        private boolean reportCarrierInfo = true;
        private boolean reportSystemInfo = true;
        private boolean geofencingActivated = false;

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
         * {@code new MobileMessaging.Builder(application)
         *       .withApiUri("http://127.0.0.1")
         *       .build();
         * }
         * </pre>
         * <p/>
         * The default is set to <a href="https://oneapi.infobip.com">https://oneapi.infobip.com</a>.
         * <p/>
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
         * <p/>
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
         * It will enable tracking of geofence areas.
         * <pre>
         * {@code new MobileMessaging.Builder(application)
         *       .withGeofencing()
         *       .build();}
         * </pre>
         *
         * @return {@link Builder}
         */
        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        public Builder withGeofencing() {
            this.geofencingActivated = true;
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
         * Builds the <i>MobileMessaging</i> configuration. Registration token sync is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @return {@link MobileMessaging}
         */
        public MobileMessaging build() {
            MobileMessagingLogger.init(application);
            MobileMessagingCore.setApiUri(application, apiUri);
            MobileMessagingCore.setGcmSenderId(application, gcmSenderId);
            MobileMessagingCore.setMessageStoreClass(application, messageStoreClass);
            MobileMessagingCore.setReportCarrierInfo(application, reportCarrierInfo);
            MobileMessagingCore.setReportSystemInfo(application, reportSystemInfo);

            MobileMessaging mobileMessaging = new MobileMessaging(application);
            MobileMessaging.instance = mobileMessaging;

            new MobileMessagingCore.Builder(application)
                    .withDisplayNotification(notificationSettings)
                    .withApplicationCode(applicationCode)
                    .withGeofencing(geofencingActivated ? Geofencing.getInstance(application) : null)
                    .build();

            return mobileMessaging;
        }
    }
}
