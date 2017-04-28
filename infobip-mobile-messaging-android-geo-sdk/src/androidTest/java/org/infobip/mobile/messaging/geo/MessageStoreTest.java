package org.infobip.mobile.messaging.geo;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.storage.GeoSQLiteMessageStore;
import org.infobip.mobile.messaging.geo.storage.TestMessageStore;
import org.infobip.mobile.messaging.geo.tools.MobileMessagingTestCase;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;

/**
 * @author sslavin
 * @since 19/01/2017.
 */

public class MessageStoreTest extends MobileMessagingTestCase {

    @Test
    public void test_shouldUseGeoSqlStoreForGeo_whenConfiguredWithoutMessageStore() {
        MobileMessagingCore.setMessageStoreClass(context, null);
        assertTrue(new GeofencingHelper(context).getMessageStoreForGeo() instanceof GeoSQLiteMessageStore);
    }

    @Test
    public void test_shouldUseGeoSqlStoreForGeo_whenConfiguredWithMessageStore() {
        MobileMessagingCore.setMessageStoreClass(context, TestMessageStore.class);
        assertTrue(new GeofencingHelper(context).getMessageStoreForGeo() instanceof GeoSQLiteMessageStore);
    }
}
