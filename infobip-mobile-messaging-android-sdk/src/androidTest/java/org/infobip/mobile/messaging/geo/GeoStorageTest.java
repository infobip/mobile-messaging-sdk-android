package org.infobip.mobile.messaging.geo;

import android.content.Context;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.gcm.MobileMessageHandler;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.Helper;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.List;

/**
 * @author sslavin
 * @since 13/02/2017.
 */

public class GeoStorageTest extends InstrumentationTestCase {

    private Context context;
    private MessageStore geoStore;
    private MessageStore commonStore;
    private MobileMessageHandler handler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());

        context = getInstrumentation().getContext();
        handler = new MobileMessageHandler();
        geoStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
        geoStore.deleteAll(context);
        commonStore = MobileMessagingCore.getInstance(context).getMessageStore();
        commonStore.deleteAll(context);
    }

    public void test_shouldSaveGeoMessagesToGeoStore() throws Exception {

        // Given
        Message message = Helper.createMessage(context, "SomeMessageId", "SomeCampaignId", false, Helper.createArea("SomeAreaId"));

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
        Message message = Helper.createMessage(context, "SomeMessageId", null, false);

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
        Message message1 = Helper.createMessage(context, "SomeMessageId1", null, false);
        Message message2 = Helper.createMessage(context, "SomeMessageId2", "SomeCampaignId2", false, Helper.createArea("SomeAreaId1"));

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
}
