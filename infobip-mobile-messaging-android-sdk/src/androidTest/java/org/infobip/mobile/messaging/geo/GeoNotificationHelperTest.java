package org.infobip.mobile.messaging.geo;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

/**
 * @author sslavin
 * @since 14/03/2017.
 */

public class GeoNotificationHelperTest extends MobileMessagingTestCase {

    private GeoNotificationHelper geoNotificationHelper;
    private ArgumentCaptor<Message> messageArgumentCaptor;
    private ArgumentCaptor<Geo> geoArgumentCaptor;
    private ArgumentCaptor<GeoEventType> geoEventTypeArgumentCaptor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        geoNotificationHelper = new GeoNotificationHelper(context, broadcaster);
        messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        geoArgumentCaptor = ArgumentCaptor.forClass(Geo.class);
        geoEventTypeArgumentCaptor = ArgumentCaptor.forClass(GeoEventType.class);
    }

    public void test_shoud_broadcast_message_and_geo() throws Exception {

        // Given
        Geo geo = createGeo(1.0, 2.0, "SomeCampaignId", createArea("SomeAreaId", "SomeAreaTitle", 3.0, 4.0, 5));
        final Message message = createMessage(context, "SomeMessageId", false, geo);
        Map<Message, GeoEventType> messages = new HashMap<>();
        messages.put(message, GeoEventType.entry);

        // When
        geoNotificationHelper.notifyAboutGeoTransitions(messages);

        // Then
        Mockito.verify(broadcaster, Mockito.times(1)).messageReceived(messageArgumentCaptor.capture());
        assertJEquals(message, messageArgumentCaptor.getValue());
        
        Mockito.verify(broadcaster, Mockito.times(1)).geoEvent(geoEventTypeArgumentCaptor.capture(), messageArgumentCaptor.capture(), geoArgumentCaptor.capture());
        assertEquals(GeoEventType.entry, geoEventTypeArgumentCaptor.getValue());
        assertJEquals(message, messageArgumentCaptor.getValue());
        assertJEquals(geo, geoArgumentCaptor.getValue());
    }
}
