package org.infobip.mobile.messaging.geo;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.Date;
import java.util.List;

/**
 * @author sslavin
 * @since 13/02/2017.
 */

public class MobileMessageHandlerTest extends MobileMessagingTestCase {

    private MobileMessageHandler handler;
    private MessageStore commonStore;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext();

        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, true);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, true);

        handler = new MobileMessageHandler(broadcaster);
        commonStore = MobileMessaging.getInstance(context).getMessageStore();
    }

    public void test_shouldSaveGeoMessagesToGeoStore() throws Exception {

        // Given
        Message message = createMessage(context, "SomeMessageId", "SomeCampaignId", false, createArea("SomeAreaId"));

        // When
        handler.handleMessage(context, message);

        // Then
        List<Message> messages = geoStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
        assertEquals("SomeCampaignId", messages.get(0).getGeo().getCampaignId());
        assertEquals("SomeAreaId", messages.get(0).getGeo().getAreasList().get(0).getId());
        assertEquals(0, commonStore.countAll(context));
    }

    public void test_shouldSaveNonGeoMessagesToCommonStore() throws Exception {
        // Given
        Message message = createMessage(context, "SomeMessageId", null, false);

        // When
        handler.handleMessage(context, message);

        // Then
        List<Message> messages = commonStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
        assertEquals(0, geoStore.countAll(context));
    }

    public void test_shouldSaveMessagesToCorrespondingSeparateStores() throws Exception {
        // Given
        Message message1 = createMessage(context, "SomeMessageId1", null, false);
        Message message2 = createMessage(context, "SomeMessageId2", "SomeCampaignId2", false, createArea("SomeAreaId1"));

        // When
        handler.handleMessage(context, message1);
        handler.handleMessage(context, message2);

        // Then
        List<Message> messages = commonStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId1", messages.get(0).getMessageId());
        messages = geoStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId2", messages.get(0).getMessageId());
        assertEquals("SomeCampaignId2", messages.get(0).getGeo().getCampaignId());
        assertEquals("SomeAreaId1", messages.get(0).getGeo().getAreasList().get(0).getId());
    }

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
        MobileMessagingCore.getInstance(context).removeExpiredAreas();

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
