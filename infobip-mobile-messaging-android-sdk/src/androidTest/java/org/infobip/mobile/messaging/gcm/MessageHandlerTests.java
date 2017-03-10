package org.infobip.mobile.messaging.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.Brockito;
import org.infobip.mobile.messaging.tools.Helper;
import org.infobip.mobile.messaging.tools.InfobipAndroidTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.Mockito;

import java.util.List;

/**
 * @author sslavin
 * @since 15/02/2017.
 */

public class MessageHandlerTests extends InfobipAndroidTestCase {

    private MobileMessageHandler handler;
    private MessageStore commonStore;
    private MessageStore geoStore;
    private BroadcastReceiver messageReceiver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());

        handler = new MobileMessageHandler();
        commonStore = MobileMessaging.getInstance(context).getMessageStore();
        commonStore.deleteAll(context);
        geoStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
        geoStore.deleteAll(context);
        messageReceiver = Brockito.mock();
        LocalBroadcastManager.getInstance(context).registerReceiver(messageReceiver, new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageReceiver);
        super.tearDown();
    }

    public void test_shouldSaveGeoMessageToGeoStore() throws Exception {

        // Given
        Geo geo = Helper.createGeo(1.0, 2.0, "campaignId", Helper.createArea("areaId"));
        Message m = Helper.createMessage(context, "SomeMessageId", false, geo);

        // When
        handler.handleMessage(context, m);

        // Then
        List<Message> messages = geoStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
    }

    public void test_shouldSaveNonGeoMessageToUserStore() throws Exception {

        // Given
        Message m = Helper.createMessage(context, "SomeMessageId", false);

        // When
        handler.handleMessage(context, m);

        // Then
        List<Message> messages = commonStore.findAll(context);
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
    }

    public void test_shouldSend_MESSAGE_RECEIVED_forNonGeoMessage() throws Exception {

        // Given
        Message m = Helper.createMessage(context, "SomeMessageId", false);

        // When
        handler.handleMessage(context, m);

        // Then
        Brockito.verify(messageReceiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    public void test_shouldNotSend_MESSAGE_RECEIVED_forGeoMessage() throws Exception {

        // Given
        Geo geo = Helper.createGeo(1.0, 2.0, "campaignId", Helper.createArea("areaId"));
        Message m = Helper.createMessage(context, "SomeMessageId", false, geo);

        // When
        handler.handleMessage(context, m);

        // Then
        Brockito.verify(messageReceiver, Mockito.never()).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }
}
