package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;

/**
 * @author sslavin
 * @since 14/02/2017.
 */

public class SeenStatusStorageTest extends MobileMessagingTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        enableMessageStoreForReceivedMessages();
    }

    public void test_shouldUpdateSeenTimestampInMessageStore() {

        // Given
        Long now = Time.now();
        createMessage(context, "SomeMessageId", true);

        // When
        MobileMessaging.getInstance(context).setMessagesSeen("SomeMessageId");

        // Then
        Message message = MobileMessaging.getInstance(context).getMessageStore().findAll(context).get(0);
        assertEquals(now, message.getSeenTimestamp(), 100);
    }
}
