package org.infobip.mobile.messaging.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.bundle.BundleMessageMapper;
import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoEvent;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.BroadcastReceiverMockito;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.json.JSONObject;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 15/02/2017.
 */

public class MessageHandlerTests extends InstrumentationTestCase {

    private Context context;
    private MobileMessageHandler handler;
    private MessageStore commonStore;
    private MessageStore geoStore;
    private BroadcastReceiver messageReceiver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext();

        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());

        handler = new MobileMessageHandler();
        commonStore = MobileMessaging.getInstance(context).getMessageStore();
        commonStore.deleteAll(context);
        geoStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
        geoStore.deleteAll(context);
        messageReceiver = BroadcastReceiverMockito.mock();
        LocalBroadcastManager.getInstance(context).registerReceiver(messageReceiver, new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageReceiver);
        super.tearDown();
    }

    public void test_shouldSaveGeoMessageToGeoStore() throws Exception {

        // Given
        final Area area = new Area("areaId", "", 1.0, 2.0, 3);
        Geo geo = new Geo(1.0, 2.0, new ArrayList<Area>(){{add(area);}}, null, new ArrayList<GeoEvent>(), null, null, "campaignId");
        JSONObject internalData = new JSONObject(new JsonSerializer().serialize(geo));
        Message m = new Message();
        m.setMessageId("SomeMessageId");
        m.setGeo(geo);
        m.setInternalData(internalData);
        Intent intent = new Intent();
        intent.putExtras(BundleMessageMapper.toBundle(m));

        // When
        handler.handleMessage(context, intent);

        // Then
        List<Message> messages = geoStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
    }

    public void test_shouldSaveNonGeoMessageToUserStore() throws Exception {

        // Given
        Message m = new Message();
        m.setMessageId("SomeMessageId");
        Intent intent = new Intent();
        intent.putExtras(BundleMessageMapper.toBundle(m));

        // When
        handler.handleMessage(context, intent);

        // Then
        List<Message> messages = commonStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
    }

    public void test_shouldSend_MESSAGE_RECEIVED_forNonGeoMessage() throws Exception {

        // Given
        Message m = new Message();
        m.setMessageId("SomeMessageId");
        m.setBody("SomeMessageBody");
        Intent intent = new Intent();
        intent.putExtras(BundleMessageMapper.toBundle(m));

        // When
        handler.handleMessage(context, intent);

        // Then
        BroadcastReceiverMockito.verify(messageReceiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    public void test_shouldNotSend_MESSAGE_RECEIVED_forGeoMessage() throws Exception {

        // Given
        final Area area = new Area("areaId", "", 1.0, 2.0, 3);
        Geo geo = new Geo(1.0, 2.0, new ArrayList<Area>(){{add(area);}}, null, new ArrayList<GeoEvent>(), null, null, "campaignId");
        JSONObject internalData = new JSONObject(new JsonSerializer().serialize(geo));
        Message m = new Message();
        m.setMessageId("SomeMessageId");
        m.setBody("SomeMessageBody");
        m.setGeo(geo);
        m.setInternalData(internalData);
        Intent intent = new Intent();
        intent.putExtras(BundleMessageMapper.toBundle(m));

        // When
        handler.handleMessage(context, intent);

        // Then
        BroadcastReceiverMockito.verify(messageReceiver, Mockito.never()).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }
}
