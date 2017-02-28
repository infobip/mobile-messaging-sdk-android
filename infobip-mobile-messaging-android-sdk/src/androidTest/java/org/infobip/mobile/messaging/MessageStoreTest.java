package org.infobip.mobile.messaging;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.geo.GeoSQLiteMessageStore;
import org.infobip.mobile.messaging.storage.TestMessageStore;
import org.infobip.mobile.messaging.util.PreferenceHelper;

/**
 * @author sslavin
 * @since 19/01/2017.
 */

public class MessageStoreTest extends InstrumentationTestCase {

    Context context;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext();

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .clear()
                .commit();

        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
    }

    public void test_shouldUseGeoSqlStoreForGeo_whenConfiguredWithoutMessageStore() {
        MobileMessagingCore.setMessageStoreClass(context, null);

        assertTrue(MobileMessagingCore.getInstance(context).getMessageStoreForGeo() instanceof GeoSQLiteMessageStore);
    }

    public void test_shouldUseGeoSqlStoreForGeo_whenConfiguredWithMessageStore() {
        MobileMessagingCore.setMessageStoreClass(context, TestMessageStore.class);

        assertTrue(MobileMessagingCore.getInstance(context).getMessageStoreForGeo() instanceof GeoSQLiteMessageStore);
    }
}
