package org.infobip.mobile.messaging;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.util.PreferenceHelper;

/**
 * @author sslavin
 * @since 14/02/2017.
 */

public class SeenStatusStorageTest extends InstrumentationTestCase {

    private Context context;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.API_URI, "http://fake.api.com/");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());
        MobileMessaging.getInstance(context).getMessageStore().deleteAll(context);
    }

    public void test_shouldUpdateSeenTimestampInMessageStore() {

        // Given
        Long now = System.currentTimeMillis();
        Message messageBefore = new Message("SomeMessageId", "title", "body", "sound", true, "icon", false, "category", "from", 1, 0, null, null, null, null, Message.Status.SUCCESS, null);
        MobileMessaging.getInstance(context).getMessageStore().save(context, messageBefore);

        // When
        MobileMessaging.getInstance(context).setMessagesSeen("SomeMessageId");

        // Then
        Message message = MobileMessaging.getInstance(context).getMessageStore().findAll(context).get(0);
        assertEquals(now, message.getSeenTimestamp(), 100);
    }
}
