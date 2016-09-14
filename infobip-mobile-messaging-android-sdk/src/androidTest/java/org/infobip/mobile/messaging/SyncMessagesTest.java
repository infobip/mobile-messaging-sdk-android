package org.infobip.mobile.messaging;

import android.test.InstrumentationTestCase;

import java.util.UUID;

/**
 * @author pandric
 * @since 09/09/16.
 */
public class SyncMessagesTest extends InstrumentationTestCase {


    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test_find_all_messageIDs() {
        String[] mockIDs = new String[120];
        for (int i = 0; i < mockIDs.length; i++) {
            mockIDs[i] = UUID.randomUUID().toString();
        }

        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(getInstrumentation().getContext());
        mobileMessagingCore.addSyncMessagesIds(mockIDs);
        String[] messageIDs = mobileMessagingCore.getSyncMessagesIds();

        assertNotNull(messageIDs);
        assertTrue(100 >= messageIDs.length);
    }
}
