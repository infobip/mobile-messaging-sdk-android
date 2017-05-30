package org.infobip.mobile.messaging.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.MobileMessagingTestable;
import org.infobip.mobile.messaging.android.MobileMessagingBaseTestCase;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.SqliteDatabaseProvider;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
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

/**
 * @author sslavin
 * @since 10/03/2017.
 */

@RunWith(AndroidJUnit4.class)
public abstract class MobileMessagingTestCase extends MobileMessagingBaseTestCase {

    protected MobileMessaging mobileMessaging;
    protected MobileMessagingTestable mobileMessagingCore;
    protected MessageStore geoStore;
    protected DatabaseHelper databaseHelper;
    protected SqliteDatabaseProvider databaseProvider;
    protected Broadcaster broadcaster;
    protected TestTimeProvider time;
    protected NotificationHandler notificationHandler;

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

    @SuppressWarnings("WeakerAccess")
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

    @SuppressLint("ApplySharedPref")
    @Before
    public void setUp() throws Exception {
        super.setUp();

        MobileMessagingLogger.enforce();

        time = new TestTimeProvider();
        Time.reset(time);

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();

        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_REGISTRATION_ID, "TestRegistrationId");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED, true);

        MobileApiResourceProvider.INSTANCE.resetMobileApi();

        notificationHandler = Mockito.mock(NotificationHandler.class);
        broadcaster = Mockito.mock(Broadcaster.class);
        mobileMessagingCore = MobileMessagingTestable.create(context, broadcaster);
        mobileMessaging = mobileMessagingCore;

        databaseHelper = MobileMessagingCore.getDatabaseHelper(context);
        databaseProvider = MobileMessagingCore.getDatabaseProvider(context);
    }

    protected void enableMessageStoreForReceivedMessages() {
        PreferenceHelper.saveClass(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, TestMessageStore.class);
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
        databaseProvider.deleteDatabase();
    }


    /**
     * Generates messages with provided ids and geo campaign object
     *
     * @param saveToStorage set to true to save messages to message store
     * @param messageId     message id for a message
     * @return new message
     */
    protected static Message createMessage(Context context, String messageId, boolean saveToStorage) {
        Message message = new Message();
        message.setMessageId(messageId);
        if (saveToStorage) {
            MobileMessagingCore.getInstance(context).getMessageStore().save(context, message);
        }
        return message;
    }
}
