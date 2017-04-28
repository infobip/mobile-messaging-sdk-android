package org.infobip.mobile.messaging.geo;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.mapper.GeoBundleMapper;
import org.infobip.mobile.messaging.geo.mapper.GeoDataMapper;
import org.infobip.mobile.messaging.geo.push.PushMessageHandler;
import org.infobip.mobile.messaging.geo.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author sslavin
 * @since 13/02/2017.
 */

public class MobileMessageHandlerTest extends MobileMessagingTestCase {

    private MobileMessageHandler mobileMessageHandler;
    private PushMessageHandler pushMessageHandler;
    private MessageStore commonStore;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        enableMessageStoreForReceivedMessages();

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, true);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, true);

        mobileMessageHandler = new MobileMessageHandler(messageBroadcaster);
        pushMessageHandler = new PushMessageHandler();
        commonStore = mobileMessaging.getMessageStore();
    }

    @Test
    public void test_shouldSaveGeoMessagesToGeoStore() throws Exception {

        // Given
        Message message = createMessage(context, "SomeMessageId", "SomeCampaignId", false, createArea("SomeAreaId"));

        // When
        pushMessageHandler.handleGeoMessage(context, message);

        // Then
        List<Message> messages = geoStore.findAll(context);
        assertEquals(1, messages.size());
        Message message1 = messages.get(0);
        Geo geo = GeoDataMapper.geoFromInternalData(message1.getInternalData());
        assertNotNull(geo);
        assertEquals("SomeMessageId", message1.getMessageId());
        assertEquals("SomeCampaignId", geo.getCampaignId());
        assertEquals("SomeAreaId", geo.getAreasList().get(0).getId());
        assertEquals(0, commonStore.countAll(context));
    }

    @Test
    public void test_shouldSaveNonGeoMessagesToCommonStore() throws Exception {
        // Given
        Message message = createMessage(context, "SomeMessageId", null, false);

        // When
        mobileMessageHandler.handleMessage(context, message);

        // Then
        List<Message> messages = commonStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
        assertEquals(0, geoStore.countAll(context));
    }

    @Test
    public void test_shouldNotSaveGeoMessagesToMessageStore() throws Exception {
        // Given
        Message message1 = createMessage(context, "SomeMessageId1", null, false);
        Message message2 = createMessage(context, "SomeMessageId2", "SomeCampaignId2", false, createArea("SomeAreaId1"));

        // When
        mobileMessageHandler.handleMessage(context, message1);
        mobileMessageHandler.handleMessage(context, message2);

        // Then
        List<Message> messages = commonStore.findAll(context);
        assertEquals(1, messages.size());
        Message message = messages.get(0);
        assertEquals("SomeMessageId1", message.getMessageId());
    }

    @Test
    public void test_shouldDeleteExpiredAreas() throws Exception {
        // Given
        long now = Time.now();
        Long millis30MinBeforeNow = now - 30 * 60 * 1000;
        Long millis15MinBeforeNow = now - 15 * 60 * 1000;
        Long millis15MinAfterNow = now + 15 * 60 * 1000;
        String date30MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis30MinBeforeNow));
        String date15MinBeforeNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinBeforeNow));
        String date15MinAfterNow = DateTimeUtil.ISO8601DateToString(new Date(millis15MinAfterNow));
        String nonExpiredMessageId = "SomeMessageId5";

        saveGeoMessageToDb("SomeMessageId1", null, date30MinBeforeNow);
        saveGeoMessageToDb("SomeMessageId2", null, date30MinBeforeNow);
        saveGeoMessageToDb("SomeMessageId3", null, date15MinBeforeNow);
        saveGeoMessageToDb("SomeMessageId4", null, date15MinBeforeNow);
        saveGeoMessageToDb(nonExpiredMessageId, null, date15MinAfterNow);

        assertEquals(5, geoStore.countAll(context));

        // When
        GeofencingHelper geofencingHelper = new GeofencingHelper(context);
        geofencingHelper.removeExpiredAreas();

        // Then
        assertEquals(1, geoStore.countAll(context));
        assertEquals(nonExpiredMessageId, geoStore.findAll(context).get(0).getMessageId());
    }

    private void saveGeoMessageToDb(String messageId, String startTimeMillis, String expiryTimeMillis) {
        Geo geo = createGeo(0.0, 0.0, expiryTimeMillis, startTimeMillis, "SomeCampaignId",
                createArea("SomeAreaId", "SomeAreaTitle", 0.0, 0.0, 10));
        createMessage(context, messageId, true, geo);
    }
}
