package org.infobip.mobile.messaging.interactive.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.android.MobileMessagingBaseTestCase;
import org.infobip.mobile.messaging.cloud.firebase.FirebaseAppProvider;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.SqliteDatabaseProvider;
import org.infobip.mobile.messaging.interactive.InteractiveEvent;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationActionBundleMapper;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationCategoryBundleMapper;
import org.infobip.mobile.messaging.interactive.notification.NotificationActionTapReceiver;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.platform.TimeProvider;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_MESSAGE;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_NOTIFICATION_ID;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_ACTION;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_CATEGORY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * @author tjuric
 * @since 07/08/2017.
 */
@RunWith(AndroidJUnit4.class)
public abstract class MobileMessagingTestCase extends MobileMessagingBaseTestCase {

    protected DatabaseHelper databaseHelper;
    protected SqliteDatabaseProvider databaseProvider;
    protected Broadcaster messageBroadcaster;
    protected TestTimeProvider time;
    protected MobileMessagingCore mobileMessagingCore;
    protected MobileMessaging mobileMessaging;
    protected NotificationHandler notificationHandler;
    protected FirebaseAppProvider firebaseAppProvider;

    protected static class TestTimeProvider implements TimeProvider {

        long delta = 0;
        boolean overwritten = false;

        public void forward(long time, TimeUnit unit) {
            delta += unit.toMillis(time);
        }

        public void backward(long time, TimeUnit unit) {
            delta -= unit.toMillis(time);
        }

        public void reset() {
            overwritten = false;
            delta = 0;
        }

        public void set(long time) {
            overwritten = true;
            delta = time;
        }

        @Override
        public long now() {
            if (overwritten) {
                return delta;
            } else {
                return System.currentTimeMillis() + delta;
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    @Before
    public void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.getDefaultMMSharedPreferences(context).edit().clear().commit();

        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID, "UniversalInstallationId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.CLOUD_TOKEN, "TestRegistrationId");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, true);

        MobileMessagingLogger.enforce();

        time = new TestTimeProvider();
        Time.reset(time);

        notificationHandler = Mockito.mock(NotificationHandler.class);
        messageBroadcaster = Mockito.mock(Broadcaster.class);

        firebaseAppProvider = mock(FirebaseAppProvider.class);
        FirebaseOptions firebaseOptions = new FirebaseOptions.Builder().setProjectId("project_id").setApiKey("api_key").setApplicationId("application_id").build();
        Mockito.when(firebaseAppProvider.getFirebaseApp()).thenCallRealMethod();
        Mockito.when(firebaseAppProvider.getContext()).thenReturn(context);
        Mockito.when(firebaseAppProvider.loadFirebaseOptions(Mockito.any(Context.class))).thenReturn(firebaseOptions);

        mobileMessagingCore = MobileMessagingTestable.create(context, messageBroadcaster, firebaseAppProvider);
        mobileMessaging = mobileMessagingCore;

        databaseHelper = MobileMessagingCore.getDatabaseHelper(context);
        databaseProvider = MobileMessagingCore.getDatabaseProvider(context);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        time.reset();
        if (null != debugServer) {
            try {
                debugServer.stop();
            } catch (Exception ignored) {
            }
        }

        if (databaseProvider != null)
            databaseProvider.deleteDatabase();
    }

    public static class TestMessageStore implements MessageStore {

        Map<String, Message> messages = new HashMap<>();

        @Override
        public List<Message> findAll(Context context) {
            return new ArrayList<>(messages.values());
        }

        @Override
        public long countAll(Context context) {
            return messages.values().size();
        }

        @Override
        public void save(Context context, Message... messages) {
            for (Message message : messages) {
                this.messages.put(message.getMessageId(), message);
            }
        }

        @Override
        public void deleteAll(Context context) {
            messages.clear();
        }
    }

    protected void enableMessageStoreForReceivedMessages() {
        PreferenceHelper.saveClass(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, TestMessageStore.class);
    }

    /**
     * Generates messages with provided ids and geo campaign object
     *
     * @param saveToStorage set to true to save messages to message store
     * @param messageId     message id for a message
     * @return new message
     */
    protected static Message createMessage(Context context, String messageId, boolean saveToStorage) {
        return createMessage(context, messageId, null, saveToStorage);
    }

    protected static Message createMessage(Context context, String messageId, String categoryId, boolean saveToStorage) {
        Message message = new Message();
        message.setMessageId(messageId);
        message.setCategory(categoryId);
        if (saveToStorage) {
            MobileMessagingCore.getInstance(context).getMessageStore().save(context, message);
        }
        return message;
    }

    @NonNull
    protected NotificationAction.Builder givenNotificationAction(String givenTappedActionId) {
        return new NotificationAction.Builder()
                .withId(givenTappedActionId)
                .withIcon(android.R.drawable.btn_default)
                .withTitleResourceId(android.R.string.ok);
    }

    @NonNull
    protected NotificationCategory givenNotificationCategory(NotificationAction tappedAction) {
        return new NotificationCategory(
                "categoryId",
                tappedAction,
                givenNotificationAction("actionIdNotTapped").build());
    }

    protected Intent givenIntent(Message message, NotificationCategory notificationCategory, NotificationAction action, int notificationId) {
        return new Intent(context, NotificationActionTapReceiver.class)
                .setAction(InteractiveEvent.NOTIFICATION_ACTION_TAPPED.getKey())
                .putExtra(EXTRA_MESSAGE, MessageBundleMapper.messageToBundle(message))
                .putExtra(EXTRA_TAPPED_ACTION, NotificationActionBundleMapper.notificationActionToBundle(action))
                .putExtra(EXTRA_TAPPED_CATEGORY, NotificationCategoryBundleMapper.notificationCategoryToBundle(notificationCategory))
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);
    }

}
