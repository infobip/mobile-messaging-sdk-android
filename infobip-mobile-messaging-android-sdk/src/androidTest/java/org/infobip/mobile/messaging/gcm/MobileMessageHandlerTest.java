package org.infobip.mobile.messaging.gcm;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * @author sslavin
 * @since 15/02/2017.
 */

public class MobileMessageHandlerTest extends MobileMessagingTestCase {

    private MobileMessageHandler handler;
    private MessageStore commonStore;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        enableMessageStoreForReceivedMessages();

        handler = new MobileMessageHandler(mobileMessagingCore, broadcaster, notificationHandler, mobileMessagingCore.getMessageStoreWrapper());
        commonStore = MobileMessaging.getInstance(context).getMessageStore();
    }

    @Test
    public void test_shouldSaveNonGeoMessageToUserStore() throws Exception {

        // Given
        Message m = createMessage(context, "SomeMessageId", false);

        // When
        handler.handleMessage(m);

        // Then
        List<Message> messages = commonStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
    }

    @Test
    public void test_shouldSend_messageReceived_forNonGeoMessage() throws Exception {

        // Given
        Message m = createMessage(context, "SomeMessageId", false);

        // When
        handler.handleMessage(m);

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).messageReceived(Mockito.any(Message.class));
    }

    @Test
    public void test_shouldSend_geoMessageReceived_forGeoMessage() throws Exception {

        // Given
        String messageJson = "{\n" +
                "  \"body\": null,\n" +
                "  \"category\": null,\n" +
                "  \"customPayload\": null,\n" +
                "  \"destination\": null,\n" +
                "  \"from\": null,\n" +
                "  \"icon\": null,\n" +
                "  \"internalData\": \"{\\\"geo\\\":[{\\\"id\\\":\\\"areaId1\\\",\\\"latitude\\\":1.0,\\\"longitude\\\":1.0,\\\"radiusInMeters\\\":1,\\\"title\\\":\\\"\\\"}],\\\"campaignId\\\":\\\"campaigId1\\\",\\\"deliveryTime\\\":null,\\\"event\\\":[],\\\"expiryTime\\\":null,\\\"startTime\\\":null,\\\"triggeringLatitude\\\":0.0,\\\"triggeringLongitude\\\":0.0}\",\n" +
                "  \"messageId\": \"messageId1\",\n" +
                "  \"receivedTimestamp\": 1493048632769,\n" +
                "  \"seenTimestamp\": 0,\n" +
                "  \"silent\": false,\n" +
                "  \"sound\": null,\n" +
                "  \"status\": \"UNKNOWN\",\n" +
                "  \"statusMessage\": null,\n" +
                "  \"title\": null,\n" +
                "  \"vibrate\": false\n" +
                "}";
        Message m = new JsonSerializer().deserialize(messageJson, Message.class);

        // When
        handler.handleMessage(m);

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).times(1)).geoMessageReceived(Mockito.any(Message.class));
    }
}
