package org.infobip.mobile.messaging;

import android.os.Bundle;

import junit.framework.TestCase;

import org.infobip.mobile.messaging.dal.bundle.FCMMessageMapper;

/**
 * @author sslavin
 * @since 29/12/2016.
 */

public class FCMMessageMapperTest extends TestCase {

    public void test_message_should_be_possible_to_construct_message_from_bundle() throws Exception {
        // Given
        Bundle bundle = new Bundle();
        bundle.putString("gcm.notification.messageId", "SomeMessageId");
        bundle.putString("gcm.notification.title", "SomeMessageTitle");
        bundle.putString("gcm.notification.body", "SomeMessageBody");
        bundle.putString("gcm.notification.sound", "SomeMessageSound");
        bundle.putString("gcm.notification.sound2", "SomeMessageSound");
        bundle.putString("gcm.notification.vibrate", "true");
        bundle.putString("gcm.notification.icon", "SomeMessageIcon");
        bundle.putString("gcm.notification.silent", "false");
        bundle.putString("gcm.notification.category", "SomeMessageCategory");
        bundle.putString("from", "SomeMessageFrom");
        bundle.putLong("received_timestamp", 1234L);
        bundle.putLong("seen_timestamp", 5678L);
        bundle.putString("internalData", "{}");
        bundle.putString("customPayload", "{}");
        bundle.putString("destination", "SomeDestination");
        bundle.putString("status", "SUCCESS");
        bundle.putString("status_message", "SomeStatusMessage");

        // When
        Message message = FCMMessageMapper.fromCloudBundle(bundle);

        // Then
        assertEquals("SomeMessageId", message.getMessageId());
        assertEquals("SomeMessageTitle", message.getTitle());
        assertEquals("SomeMessageBody", message.getBody());
        assertEquals("SomeMessageSound", message.getSound());
        assertEquals(true, message.isVibrate());
        assertEquals("SomeMessageIcon", message.getIcon());
        assertEquals(false, message.isSilent());
        assertEquals("SomeMessageCategory", message.getCategory());
        assertEquals("SomeMessageFrom", message.getFrom());
        assertEquals(1234L, message.getReceivedTimestamp());
        assertEquals(5678L, message.getSeenTimestamp());
        assertEquals(0, message.getCustomPayload().length());
        assertTrue(message.getGeo() == null || message.getGeo().getAreasList() == null);
        assertEquals("SomeDestination", message.getDestination());
        assertEquals(Message.Status.SUCCESS, message.getStatus());
        assertEquals("SomeStatusMessage", message.getStatusMessage());
    }
}
