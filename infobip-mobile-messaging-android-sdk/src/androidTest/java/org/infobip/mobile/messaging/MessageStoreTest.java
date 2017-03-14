package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.geo.GeoSQLiteMessageStore;
import org.infobip.mobile.messaging.storage.TestMessageStore;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;

/**
 * @author sslavin
 * @since 19/01/2017.
 */

public class MessageStoreTest extends MobileMessagingTestCase {

    public void test_shouldUseGeoSqlStoreForGeo_whenConfiguredWithoutMessageStore() {
        MobileMessagingCore.setMessageStoreClass(context, null);

        assertTrue(MobileMessagingCore.getInstance(context).getMessageStoreForGeo() instanceof GeoSQLiteMessageStore);
    }

    public void test_shouldUseGeoSqlStoreForGeo_whenConfiguredWithMessageStore() {
        MobileMessagingCore.setMessageStoreClass(context, TestMessageStore.class);

        assertTrue(MobileMessagingCore.getInstance(context).getMessageStoreForGeo() instanceof GeoSQLiteMessageStore);
    }
}
