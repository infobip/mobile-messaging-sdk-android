package org.infobip.mobile.messaging.gcm;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.Mockito;

import java.util.List;

/**
 * @author sslavin
 * @since 15/02/2017.
 */

public class MessageHandlerTests extends MobileMessagingTestCase {

    private MobileMessageHandler handler;
    private MessageStore commonStore;
    private MessageStore geoStore;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());

        handler = new MobileMessageHandler(broadcaster);
        commonStore = MobileMessaging.getInstance(context).getMessageStore();
        commonStore.deleteAll(context);
        geoStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
        geoStore.deleteAll(context);
    }

    public void test_shouldSaveGeoMessageToGeoStore() throws Exception {

        // Given
        Geo geo = createGeo(1.0, 2.0, "campaignId", createArea("areaId"));
        Message m = createMessage(context, "SomeMessageId", false, geo);

        // When
        handler.handleMessage(context, m);

        // Then
        List<Message> messages = geoStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
    }

    public void test_shouldSaveNonGeoMessageToUserStore() throws Exception {

        // Given
        Message m = createMessage(context, "SomeMessageId", false);

        // When
        handler.handleMessage(context, m);

        // Then
        List<Message> messages = commonStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
    }

    public void test_shouldSend_messageReceived_forNonGeoMessage() throws Exception {

        // Given
        Message m = createMessage(context, "SomeMessageId", false);

        // When
        handler.handleMessage(context, m);

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).messageReceived(Mockito.any(Message.class));
    }

    public void test_shouldNotSend_MESSAGE_RECEIVED_forGeoMessage() throws Exception {

        // Given
        Geo geo = createGeo(1.0, 2.0, "campaignId", createArea("areaId"));
        Message m = createMessage(context, "SomeMessageId", false, geo);

        // When
        handler.handleMessage(context, m);

        // Then
        Mockito.verify(broadcaster, Mockito.never()).messageReceived(Mockito.any(Message.class));
    }
}
