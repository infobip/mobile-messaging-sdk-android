package org.infobip.mobile.messaging;

import android.os.Bundle;

import junit.framework.TestCase;

import org.infobip.mobile.messaging.dal.bundle.BundleMessageMapper;
import org.json.JSONObject;

/**
 * @author sslavin
 * @since 29/12/2016.
 */

public class BundleMessageMapperTest extends TestCase {

    @SuppressWarnings("ConstantConditions")
    public void test_should_be_possible_to_save_all_message_fields_to_bundle() throws Exception {
        Message message = new Message(
                "SomeMessageId",
                "SomeMessageTitle",
                "SomeMessageBody",
                "SomeMessageSound",
                true,
                "SomeMessageIcon",
                false,
                "SomeMessageCategory",
                "SomeMessageFrom",
                123L,
                456L,
                new JSONObject(),
                new JSONObject(),
                null,
                "SomeDestination",
                Message.Status.ERROR,
                "SomeStatusMessage"
        );

        Bundle bundle = BundleMessageMapper.toBundle(message);

        assertEquals("SomeMessageId", bundle.getString("gcm.notification.messageId"));
        assertEquals("SomeMessageTitle", bundle.getString("gcm.notification.title"));
        assertEquals("SomeMessageBody", bundle.getString("gcm.notification.body"));
        assertEquals("SomeMessageSound", bundle.getString("gcm.notification.sound"));
        assertEquals("SomeMessageSound", bundle.getString("gcm.notification.sound2"));
        assertEquals(true, "true".equals(bundle.getString("gcm.notification.vibrate")));
        assertEquals("SomeMessageIcon", bundle.getString("gcm.notification.icon"));
        assertEquals(false, "true".equals(bundle.getString("gcm.notification.silent")));
        assertEquals("SomeMessageCategory", bundle.getString("gcm.notification.category"));
        assertEquals("SomeMessageFrom", bundle.getString("from"));
        assertEquals(123L, bundle.getLong("received_timestamp"));
        assertEquals(456L, bundle.getLong("seen_timestamp"));
        assertEquals("{}", bundle.getString("internalData").trim());
        assertEquals("{}", bundle.getString("customPayload").trim());
        assertEquals("SomeDestination", bundle.getString("destination"));
        assertEquals(Message.Status.ERROR.name(), bundle.getString("status"));
        assertEquals("SomeStatusMessage", bundle.getString("status_message"));
    }

    public void test_message_should_be_possible_to_construct_message_from_bundle() throws Exception {
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

        Message message = BundleMessageMapper.fromBundle(bundle);

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
        assertEquals(0, message.getInternalData().length());
        assertEquals(0, message.getCustomPayload().length());
        assertEquals("SomeDestination", message.getDestination());
        assertEquals(Message.Status.SUCCESS, message.getStatus());
        assertEquals("SomeStatusMessage", message.getStatusMessage());
    }
}
