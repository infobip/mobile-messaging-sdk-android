package org.infobip.mobile.messaging;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import org.infobip.mobile.messaging.gcm.PlayServicesSupport;
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
    private final PlayServicesSupport playServicesSupport = new PlayServicesSupport();

    private MobileMessaging(Context context) {
        this.context = context;
    }

    public synchronized static MobileMessaging getInstance(Context context) {
        if (null != instance) {
            return instance;
        }

        instance = new MobileMessaging(context);
        return instance;
    }

    public void setMessagesDelivered(final String... messageIds) {
        MobileMessagingCore.getInstance(context).setMessagesDelivered(messageIds);
    }

    public long getMsisdn() {
        return MobileMessagingCore.getInstance(context).getMsisdn();
    }

    public void setMsisdn(long msisdn) {
        MobileMessagingCore.getInstance(context).setMsisdn(msisdn);
    }

    public void setMessagesSeen(final String... messageIds) {
        MobileMessagingCore.getInstance(context).setMessagesSeen(messageIds);
    }

    public MessageStore getMessageStore() {
        return MobileMessagingCore.getInstance(context).getMessageStore();
    }

    public void sync() {
        MobileMessagingCore.getInstance(context).sync();
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
        private boolean reportCarrierInfo = true;
        private boolean reportSystemInfo = true;

        @SuppressWarnings("unchecked")
        private Class<? extends MessageStore> messageStoreClass = (Class<? extends MessageStore>) MobileMessagingProperty.MESSAGE_STORE_CLASS.getDefaultValue();

        public Builder(Context context) {
            if (null == context) {
                throw new IllegalArgumentException("context is mandatory!");
            }
            this.context = context.getApplicationContext();

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
         * Builds the <i>MobileMessaging</i> configuration. Registration token sync is started by default.
         * Any messages received in the past will be reported as delivered!
         *
         * @return {@link MobileMessaging}
         */
        public MobileMessaging build() {
            MobileMessagingCore.setApiUri(context, apiUri);
            MobileMessagingCore.setGcmSenderId(context, gcmSenderId);
            MobileMessagingCore.setApplicationCode(context, applicationCode);
            MobileMessagingCore.setMessageStoreClass(context, messageStoreClass);
            MobileMessagingCore.setReportCarrierInfo(context, reportCarrierInfo);
            MobileMessagingCore.setReportSystemInfo(context, reportSystemInfo);

            MobileMessaging mobileMessaging = new MobileMessaging(context);
            MobileMessaging.instance = mobileMessaging;
            mobileMessaging.playServicesSupport.checkPlayServices(context);

            MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
            mobileMessagingCore.setNotificationSettings(notificationSettings);

            return mobileMessaging;
        }
    }
}
