package org.infobip.mobile.messaging;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * The main configuration class. It is used to configure and start the Mobile Messaging System.
 * <p/>
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
 * @see Builder#withoutCarrierInfo()
 * @see Builder#withoutSystemInfo()
 * @since 29.02.2016.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MobileMessaging {

    public static final String TAG = "MobileMessaging";

    private static MobileMessaging instance;
    private final Context context;
    private OnReplyClickListener replyClickListener;

    /**
     * Default constructor. Use MobileMessaging.Builder to construct MobileMessaging.
     *
     * @param context android context object.
     *
     * @see MobileMessaging.Builder
     */
    private MobileMessaging(Context context) {
        this.context = context;
    }

    /**
     * Gets an instance of MobileMessaging after it is initialized via {@link MobileMessaging.Builder}.
     * @param context android context object.
     * @return instance of MobileMessaging.
     *
     * @see MobileMessaging.Builder
     */
    public synchronized static MobileMessaging getInstance(Context context) {
        if (null != instance) {
            return instance;
        }

        instance = new MobileMessaging(context);
        return instance;
    }

    /**
     * Reports delivery of messages to Mobile Messaging servers.
     * </p>
     * This method has to be used only if you handle GCM message notifications
     * without Mobile Messaging library. In all other cases the library will
     * send delivery report automatically whenever GCM push is delivered to device.
     * @param messageIds ids of messages to report delivery for
     *
     * @see Event#DELIVERY_REPORTS_SENT
     */
    public void setMessagesDelivered(final String... messageIds) {
        MobileMessagingCore.getInstance(context).setMessagesDelivered(messageIds);
    }

    /**
     * Reports seen status of messages to Mobile Messaging servers.
     * </p>
     * This method shall be user to report seen status when user actually sees message content.
     * @param messageIds message ids to report seen status for
     *
     * @see Event#SEEN_REPORTS_SENT
     */
    public void setMessagesSeen(final String... messageIds) {
        MobileMessagingCore.getInstance(context).setMessagesSeen(messageIds);
    }

    /**
     * Returns instance of message store that is used within the library or null if message store is not set.
     * @return instance of message store.
     *
     * @see MessageStore
     */
    public MessageStore getMessageStore() {
        return MobileMessagingCore.getInstance(context).getMessageStore();
    }

    /**
     * Reports permission request result to the library whenever permission is requested in activity.
     * </p>
     * This method shall be user to inform the library if location permissions are granted or not for {@link Geofencing}.
     * @param requestCode request code passed in <a href=https://developer.android.com/reference/android/support/v4/app/ActivityCompat.html#requestPermissions(android.app.Activity, java.lang.String[], int)>requestPermissions</a>
     * @param permissions requested permissions
     * @param grantResults grant results
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        MobileMessagingCore.getInstance(context).onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Does a synchronization of user data with server.
     * </p>
     * This method will synchronize new data with server and will also trigger {@link Event#USER_DATA_REPORTED}
     * with all the data currently available on a server for this user.
     * @param userData user data object with desired changes
     *
     * @see Event#USER_DATA_REPORTED
     */
    public void syncUserData(UserData userData) {
        MobileMessagingCore.getInstance(context).syncUserData(userData);
    }

    /**
     * Does a fetch of user data from the server.
     * </p>
     * This method will trigger {@link Event#USER_DATA_REPORTED} with all the data currently available on a server for this user.
     *
     * @see Event#USER_DATA_REPORTED
     */
    public void fetchUserData() {
        MobileMessagingCore.getInstance(context).syncUserData(null);
    }

    /**
     * Reads user data that is currently stored in the library.
     * </p>
     * This method does not trigger {@link Event#USER_DATA_REPORTED}.
     * @return last synchronized UserData object
     */
    public UserData getUserData() {
        return MobileMessagingCore.getInstance(context).getUserData();
    }

    /**
     * Send mobile originated messages.
     * </p>
     * Destination for each message is set inside {@link Message}.
     * @param messages messages to send
     */
    public void sendMessages(Message... messages) {
        MobileMessagingCore.getInstance(context).sendMessages(messages);
    }

    /**
     * This interface is used to handle an action of reply button click in actionable notification.
     */
    public interface OnReplyClickListener {
        /**
         * It is triggered when reply button is clicked.
         * @param intent intent containing message
         */
        void onReplyClicked(Intent intent);
    }

    /**
     * Starts tracking geofence areas.
     * @see Geofencing
     */
    public void activateGeofencing() {
        MobileMessagingCore.getInstance(context).activateGeofencing();
    }

    /**
     * Stops tracking geofence areas.
     * @see Geofencing
     */
    public void deactivateGeofencing() {
        MobileMessagingCore.getInstance(context).deactivateGeofencing();
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
        private Geofencing geofencing;
        private final Context context;
        private String gcmSenderId = (String) MobileMessagingProperty.GCM_SENDER_ID.getDefaultValue();
        private String applicationCode = (String) MobileMessagingProperty.APPLICATION_CODE.getDefaultValue();
        private String apiUri = (String) MobileMessagingProperty.API_URI.getDefaultValue();
        private NotificationSettings notificationSettings = null;
        private boolean reportCarrierInfo = true;
        private boolean reportSystemInfo = true;

        @SuppressWarnings("unchecked")
        private Class<? extends MessageStore> messageStoreClass = (Class<? extends MessageStore>) MobileMessagingProperty.MESSAGE_STORE_CLASS.getDefaultValue();
        private OnReplyClickListener replyActionClickListener;

        public Builder(Context context) {
            if (null == context) {
                throw new IllegalArgumentException("context object is mandatory!");
            }
            this.context = context;
            this.geofencing = Geofencing.getInstance(context);

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
         * <p/>
         * The default us set to <a href="https://oneapi.infobip.com">https://oneapi.infobip.com</a>.
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
         * It will disable tracking geofence areas.
         * <pre>
         * {@code new MobileMessaging.Builder(context)
         *       .withoutGeofencing()
         *       .build();}
         * </pre>
         *
         * @return {@link Builder}
         */
        public Builder withoutGeofencing() {
            geofencing = null;
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
         * It will not send mobile network carrier info to the server.
         * <pre>
         * {@code new MobileMessaging.Builder(context)
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
         * {@code new MobileMessaging.Builder(context)
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
         * Sets reply click listener for actionable notification.
         * <pre>
         * {@code new MobileMessaging.Builder(context)
         *       .withOnReplyClickListener(replyActionClickListener)
         *       .build();}
         * </pre>
         *
         * @return {@link Builder}
         */
        public Builder withOnReplyClickListener(OnReplyClickListener replyActionClickListener) {
            this.replyActionClickListener = replyActionClickListener;
            return this;
        }

        /**
         * Builds the <i>MobileMessaging</i> configuration. Registration token sync is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @return {@link MobileMessaging}
         */
        public MobileMessaging build() {
            MobileMessagingCore.setApiUri(context, apiUri);
            MobileMessagingCore.setGcmSenderId(context, gcmSenderId);
            MobileMessagingCore.setMessageStoreClass(context, messageStoreClass);
            MobileMessagingCore.setReportCarrierInfo(context, reportCarrierInfo);
            MobileMessagingCore.setReportSystemInfo(context, reportSystemInfo);

            MobileMessaging mobileMessaging = new MobileMessaging(context);
            MobileMessaging.instance = mobileMessaging;

            new MobileMessagingCore.Builder(context)
                    .withOnReplyClickListener(replyActionClickListener)
                    .withDisplayNotification(notificationSettings)
                    .withApplicationCode(applicationCode)
                    .withGeofencing(geofencing)
                    .build();

            return mobileMessaging;
        }
    }
}
