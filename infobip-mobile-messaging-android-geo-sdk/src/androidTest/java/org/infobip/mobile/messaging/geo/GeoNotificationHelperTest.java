package org.infobip.mobile.messaging.geo;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.geo.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.geo.transition.GeoNotificationHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * @author sslavin
 * @since 14/03/2017.
 */

public class GeoNotificationHelperTest extends MobileMessagingTestCase {

    private GeoNotificationHelper geoNotificationHelper;
    private ArgumentCaptor<Message> messageArgumentCaptor;
    private ArgumentCaptor<GeoMessage> geoMessageArgumentCaptor;
    private ArgumentCaptor<GeoEventType> geoEventTypeArgumentCaptor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        geoNotificationHelper = new GeoNotificationHelper(context, geoBroadcaster, messageBroadcaster, notificationHandler);
        geoMessageArgumentCaptor = ArgumentCaptor.forClass(GeoMessage.class);
        messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        geoEventTypeArgumentCaptor = ArgumentCaptor.forClass(GeoEventType.class);
    }

    @Test
    public void test_should_broadcast_message_and_geo() throws Exception {

        // Given
        Geo geo = createGeo(1.0, 2.0, "SomeCampaignId", null, createArea("SomeAreaId", "SomeAreaTitle", 3.0, 4.0, 5));
        final Message message = createMessage(context, "SomeMessageId", false, geo);
        Map<Message, GeoEventType> messages = new HashMap<>();
        messages.put(message, GeoEventType.entry);

        // When
        geoNotificationHelper.notifyAboutGeoTransitions(messages);

        // Then
        Mockito.verify(geoBroadcaster, Mockito.times(1)).geoEvent(geoEventTypeArgumentCaptor.capture(), geoMessageArgumentCaptor.capture());
        Mockito.verify(messageBroadcaster, Mockito.times(1)).messageReceived(messageArgumentCaptor.capture());


        assertEquals(GeoEventType.entry, geoEventTypeArgumentCaptor.getValue());
        assertJEquals(message, GeoMessage.toMessage(geoMessageArgumentCaptor.getValue()));
        assertJEquals(geo, geoMessageArgumentCaptor.getValue().getGeo());
    }
}
