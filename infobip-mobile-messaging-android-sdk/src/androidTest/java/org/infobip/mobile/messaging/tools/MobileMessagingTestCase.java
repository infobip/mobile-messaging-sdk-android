package org.infobip.mobile.messaging.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.infobip.mobile.messaging.ListCustomAttributeItem;
import org.infobip.mobile.messaging.ListCustomAttributeValue;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.MobileMessagingTestable;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.android.MobileMessagingBaseTestCase;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.api.baseurl.MobileApiBaseUrl;
import org.infobip.mobile.messaging.api.messages.MobileApiMessages;
import org.infobip.mobile.messaging.api.version.MobileApiVersion;
import org.infobip.mobile.messaging.cloud.firebase.FirebaseAppProvider;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.SqliteDatabaseProvider;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import com.google.firebase.FirebaseOptions;

/**
 * @author sslavin
 * @since 10/03/2017.
 */

@RunWith(AndroidJUnit4.class)
public abstract class MobileMessagingTestCase extends MobileMessagingBaseTestCase {

    protected MobileMessaging mobileMessaging;
    protected MobileMessagingTestable mobileMessagingCore;
    protected MobileApiResourceProvider mobileApiResourceProvider;
    protected DatabaseHelper databaseHelper;
    protected SqliteDatabaseProvider databaseProvider;
    protected Broadcaster broadcaster;
    protected TestTimeProvider time;
    protected NotificationHandler notificationHandler;

    protected MobileApiMessages mobileApiMessages;
    protected MobileApiAppInstance mobileApiAppInstance;
    protected MobileApiVersion mobileApiVersion;
    protected MobileApiBaseUrl mobileApiBaseUrl;
    protected String myDeviceRegId = "TestDeviceRegId";
    protected FirebaseAppProvider firebaseAppProvider;

    protected static final String KEY_FOR_LIST_PARAM_1 = "param1";
    protected static final String KEY_FOR_LIST_PARAM_2 = "param2";
    protected static final String KEY_FOR_LIST_PARAM_3 = "param3";
    protected static final String KEY_FOR_LIST_PARAM_4 = "param4";

    protected final String LIST_SOME_STRING_VALUE = "bla";
    protected final int LIST_SOME_NUMBER_VALUE = 1111;
    protected final Boolean LIST_SOME_BOOLEAN_VALUE = true;
    protected final Date LIST_SOME_DATE_VALUE = new Date();

    protected static final String KEY_FOR_LIST = "keyForList";

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

        PreferenceHelper.getPublicSharedPreferences(context).edit().clear().commit();
        PreferenceHelper.getPrivateMMSharedPreferences(context).edit().clear().commit();
        PreferenceHelper.saveUsePrivateSharedPrefs(context, true);

//        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, myDeviceRegId);
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID, "UniversalInstallationId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.CLOUD_TOKEN, "TestRegistrationId");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, true);
        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 100);

        MobileMessagingLogger.enforce();

        time = new TestTimeProvider();
        Time.reset(time);

        mobileApiResourceProvider = mock(MobileApiResourceProvider.class);
        mobileApiAppInstance = mock(MobileApiAppInstance.class, withSettings().verboseLogging());
        mobileApiMessages = mock(MobileApiMessages.class);
        mobileApiVersion = mock(MobileApiVersion.class);
        mobileApiBaseUrl = mock(MobileApiBaseUrl.class);

        given(mobileApiResourceProvider.getMobileApiAppInstance(any(Context.class))).willReturn(mobileApiAppInstance);
        given(mobileApiResourceProvider.getMobileApiMessages(any(Context.class))).willReturn(mobileApiMessages);
        given(mobileApiResourceProvider.getMobileApiVersion(any(Context.class))).willReturn(mobileApiVersion);
        given(mobileApiResourceProvider.getMobileApiBaseUrl(any(Context.class))).willReturn(mobileApiBaseUrl);

        notificationHandler = mock(NotificationHandler.class);
        broadcaster = mock(Broadcaster.class);

        firebaseAppProvider = new FirebaseAppProvider(context);
        FirebaseOptions firebaseOptions = new FirebaseOptions.Builder().setProjectId("project_id").setApiKey("api_key").setApplicationId("application_id").build();
        firebaseAppProvider.setFirebaseOptions(firebaseOptions);

        mobileMessagingCore = MobileMessagingTestable.create(context, broadcaster, mobileApiResourceProvider, firebaseAppProvider);
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
//        if (null != debugServer) {
//            try {
//                debugServer.stop();
//            } catch (Exception ignored) {
//            }
//        }
        databaseProvider.deleteDatabase();
    }

    protected User setListCustomAttributes(User user) {
        return setListCustomAttributes(user, KEY_FOR_LIST);
    }

    protected User setListCustomAttributes(User user, String key) {
        ListCustomAttributeValue list = new ListCustomAttributeValue(getListCustomValueItems());
        user.setListCustomAttribute(key, list);
        return user;
    }

    protected List<ListCustomAttributeItem> getListCustomValueItems() {
        ListCustomAttributeItem.Builder customMapBuilder = ListCustomAttributeItem.builder();
        customMapBuilder.putNumber(KEY_FOR_LIST_PARAM_1, LIST_SOME_NUMBER_VALUE);
        customMapBuilder.putBoolean(KEY_FOR_LIST_PARAM_2, LIST_SOME_BOOLEAN_VALUE);
        customMapBuilder.putString(KEY_FOR_LIST_PARAM_3, LIST_SOME_STRING_VALUE);
        customMapBuilder.putDate(KEY_FOR_LIST_PARAM_4, LIST_SOME_DATE_VALUE);
        List<ListCustomAttributeItem> customList = new ArrayList<>();
        customList.add(customMapBuilder.build());
        return customList;
    }

    /**
     * Generates messages with provided ids
     *
     * @param saveToStorage set to true to save messages to message store
     * @param messageId     message id for a message
     * @return new message
     */
    protected static Message createMessage(Context context, String messageId, boolean saveToStorage) {
        Message message = new Message();
        message.setMessageId(messageId);
        message.setBody("some text");
        if (saveToStorage) {
            MobileMessagingCore.getInstance(context).getMessageStore().save(context, message);
        }
        return message;
    }
}
