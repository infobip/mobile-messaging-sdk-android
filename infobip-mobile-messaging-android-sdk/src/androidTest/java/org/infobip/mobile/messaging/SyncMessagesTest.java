package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * @author pandric
 * @since 09/09/16.
 */
public class SyncMessagesTest extends MobileMessagingTestCase {

    private static final int MESSAGE_ID_PARAMETER_LIMIT = 100;

    @Test
    public void test_find_all_messageIDs() {
        String[] mockIDs = new String[120];
        for (int i = 0; i < mockIDs.length; i++) {
            mockIDs[i] = UUID.randomUUID().toString();
        }

        mobileMessagingCore.addSyncMessagesIds(mockIDs);
        String[] messageIDs = mobileMessagingCore.getSyncMessagesIds();

        assertNotNull(messageIDs);
        assertTrue(MESSAGE_ID_PARAMETER_LIMIT >= messageIDs.length);
    }

    @Test
    public void test_find_all_no_duplicates_and_nulls() {
        String mockId = UUID.randomUUID().toString();
        String[] mockIDs = new String[10];
        for (int i = 0; i < mockIDs.length; i++) {
            mockIDs[i] = i % 2 == 0 ? mockId : null;
        }

        mobileMessagingCore.addSyncMessagesIds(mockIDs);
        String[] messageIDs = mobileMessagingCore.getSyncMessagesIds();

        assertNotNull(messageIDs);
        assertTrue(messageIDs.length == 1);
    }
}
