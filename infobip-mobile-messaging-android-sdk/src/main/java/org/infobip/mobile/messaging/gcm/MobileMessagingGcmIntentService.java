package org.infobip.mobile.messaging.gcm;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.gcm.GcmListenerService;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.dal.bundle.FCMMessageMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;

import static org.infobip.mobile.messaging.MobileMessagingJob.GCM_INTENT_JOB_ID;
import static org.infobip.mobile.messaging.MobileMessagingJob.getScheduleId;

/**
 * MobileMessagingGcmIntentService processes GCM push notifications. To be able to use it you must register it as a receiver in AndroidManifest.xml
 * <pre>
 * {@code <receiver android:name="org.infobip.mobile.messaging.gcm.MobileMessagingGcmReceiver"
 *             android:exported="true"
 *             android:permission="com.google.android.c2dm.permission.SEND">
 *       <intent-filter>
 *           <action android:name="com.google.android.c2dm.intent.RECEIVE"/>
 *       </intent-filter>
 *   </receiver>
 *   <service android:name="org.infobip.mobile.messaging.gcm.MobileMessagingGcmIntentService"
 *             android:exported="false">
 *   </service>
 *   }
 * </pre>
 * <p>
 * On push notifications arrival, it triggers {@link Event#MESSAGE_RECEIVED} events.
 * <p>
 * You should implement a {@link android.content.BroadcastReceiver} to listen to these events and implement your
 * processing logic.
 * <p>
 * You can register receivers in AndroidManifest.xml by adding:
 * <pre>
 * {@code <receiver android:name=".MyMessageReceiver" android:exported="false">
 *       <intent-filter>
 *           <action android:name="org.infobip.mobile.messaging.MESSAGE_RECEIVED"/>
 *        </intent-filter>
 *   </receiver>}
 * </pre>
 * <p>
 * You can also register receivers in you Activity by adding:
 * <pre>
 * {@code
 * public class MyActivity extends AppCompatActivity {
 *        private boolean isReceiverRegistered;
 *        private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
 *            public void onReceive(Context context, Intent intent) {
 *                Message message = new Message(intent.getExtras());
 *                ... process your message here
 *            }
 *        };
 *
 *        protected void onCreate(Bundle savedInstanceState) {
 *            super.onCreate(savedInstanceState);
 *
 *            registerReceiver();
 *        }
 *
 *        protected void onResume() {
 *            super.onResume();
 *            registerReceiver();
 *        }
 *
 *        protected void onPause() {
 *            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
 *            isReceiverRegistered = false;
 *            super.onPause();
 *        }
 *
 *        private void registerReceiver() {
 *            if (isReceiverRegistered) {
 *                return;
 *            }
 *            LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
 *            new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
 *            isReceiverRegistered = true;
 *        }
 *    }}
 * </pre>
 * <p>
 * It offers the possibility to display notifications upon arrival if you use the setting
 * {@link MobileMessaging.Builder#withDisplayNotification(NotificationSettings)} and set the callback activity
 * using {@link NotificationSettings.Builder#withCallbackActivity}
 * <pre>
 * {@code
 * public class MyActivity extends AppCompatActivity {
 *        protected void onCreate(Bundle savedInstanceState) {
 *            super.onCreate(savedInstanceState);
 *
 *            new MobileMessaging.Builder(this)
 *                .withDisplayNotification(
 *                    new NotificationSettings.Builder(this)
 *                        .withDisplayNotification()
 *                        .withCallbackActivity(MyActivity.class)
 *                        .build()
 *                )
 *                ...
 *                .build();
 *        }
 *    }}
 * </pre>
 * <p>
 * This is actually the default behavior. You don't have to configure <i>withDisplayNotification</i>!
 * <pre>
 * {@code
 * public class MyActivity extends AppCompatActivity {
 *        protected void onCreate(Bundle savedInstanceState) {
 *            super.onCreate(savedInstanceState);
 *
 *            new MobileMessaging.Builder(this).build();
 *        }
 *    }}
 * </pre>
 * <p>
 * If you want Android system to display the notification automatically, you scan disable the library default behavior:
 * <pre>
 * {@code
 * public class MyActivity extends AppCompatActivity {
 *        protected void onCreate(Bundle savedInstanceState) {
 *            super.onCreate(savedInstanceState);
 *
 *            new MobileMessaging.Builder(this)
 *                .withoutDisplayNotification()
 *                .build();
 *        }
 *    }}
 * </pre>
 * and register your implementation of {@link GcmListenerService} in AndroidManifest.xml
 * <pre>
 * {@code
 * <service
 *        android:name=".MyGcmListenerService"
 *        android:exported="false" >
 *        <intent-filter>
 *            <action android:name="com.google.android.c2dm.intent.RECEIVE" />
 *        </intent-filter>
 *    </service>
 * }
 * </pre>
 * <p/>
 * MobileMessagingGcmIntentService receives GCM registration tokens and raises several intents you can listen to in your
 * application and services:
 * <ul>
 * <li>{@link Event#REGISTRATION_ACQUIRED} - on any registration token acquisition from GCM</li>
 * <li>{@link Event#REGISTRATION_CREATED} - when registration token is synced with the Infobip service</li>
 * <li>{@link Event#API_COMMUNICATION_ERROR} - when an error happens while communicating with the Infobip service</li>
 * </ul>
 *
 * @author mstipanov
 * @see Event#MESSAGE_RECEIVED
 * @see android.content.BroadcastReceiver
 * @see LocalBroadcastManager
 * @since 21.03.2016.
 */
public class MobileMessagingGcmIntentService extends JobIntentService {
    public static final String ACTION_GCM_MESSAGE_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";
    public static final String ACTION_FCM_MESSAGE_RECEIVE = "com.google.firebase.MESSAGING_EVENT";
    public static final String ACTION_ACQUIRE_INSTANCE_ID = "org.infobip.mobile.messaging.gcm.INSTANCE_ID";
    public static final String ACTION_TOKEN_CLEANUP = "org.infobip.mobile.messaging.gcm.token.cleanup";
    public static final String ACTION_TOKEN_RESET = "org.infobip.mobile.messaging.gcm.token.reset";
    public static final String EXTRA_GCM_SENDER_ID = "org.infobip.mobile.messaging.gcm.GCM_SENDER_ID";

    private final RegistrationTokenHandler registrationTokenHandler = new RegistrationTokenHandler();
    private MobileMessageHandler mobileMessageHandler;

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, MobileMessagingGcmIntentService.class, getScheduleId(context, GCM_INTENT_JOB_ID), work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        String action = intent.getAction();
        if (action == null) {
            return;
        }

        switch (action) {
            case ACTION_GCM_MESSAGE_RECEIVE:
                Message message = FCMMessageMapper.fromCloudBundle(intent.getExtras());
                MobileMessagingLogger.v("RECEIVED MESSAGE FROM FCM", message);
                mobileMessageHandler().handleMessage(message);
                break;

            case ACTION_TOKEN_CLEANUP:
                registrationTokenHandler.handleRegistrationTokenCleanup(this, intent.getStringExtra(EXTRA_GCM_SENDER_ID));
                break;

            case ACTION_TOKEN_RESET:
                registrationTokenHandler.handleRegistrationTokenReset(this, intent.getStringExtra(EXTRA_GCM_SENDER_ID));
                break;

            case ACTION_ACQUIRE_INSTANCE_ID:
                registrationTokenHandler.handleRegistrationTokenUpdate(this);
                break;

            case ACTION_FCM_MESSAGE_RECEIVE:
                MobileMessagingLogger.v("Received message event from Firebase SDK, ignoring to avoid duplicate notifications");
                break;
        }
    }

    private MobileMessageHandler mobileMessageHandler() {
        if (mobileMessageHandler == null) {
            MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(this);
            NotificationHandler notificationHandler = mobileMessagingCore.getNotificationHandler();
            mobileMessageHandler = new MobileMessageHandler(
                    mobileMessagingCore,
                    new AndroidBroadcaster(this),
                    notificationHandler,
                    mobileMessagingCore.getMessageStoreWrapper());
        }
        return mobileMessageHandler;
    }
}
