package org.infobip.mobile.messaging.geo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.Helper;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author sslavin
 * @since 15/02/2017.
 */

public class GeoHandlerTests extends InstrumentationTestCase {

    private Context context;
    private GeoAreasHandler handler;
    private BroadcastReceiver messageReceiver;
    private ArgumentCaptor<Intent> captor;

    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());

        handler = new GeoAreasHandler(context);
        messageReceiver = Mockito.mock(BroadcastReceiver.class);
        captor = ArgumentCaptor.forClass(Intent.class);

        MobileMessaging.getInstance(context).getMessageStore().deleteAll(context);
        MobileMessagingCore.getInstance(context).getMessageStoreForGeo().deleteAll(context);
        LocalBroadcastManager.getInstance(context).registerReceiver(messageReceiver, new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageReceiver);
        super.tearDown();
    }

    public void test_shoudProvide_MESSAGE_RECEIVED_forGeoTransition() throws Exception {

        // Given
        Geo geo = Helper.createGeo(1.0, 2.0, "campaignId", Helper.createArea("areaId"));
        Message m = Helper.createMessage(context, "SomeMessageId", true, geo);
        GeoTransition transition = GeoHelper.createTransition("areaId");

        // When
        handler.handleTransition(transition);

        // Then
        Mockito.verify(messageReceiver, Mockito.atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
        Message message = Message.createFrom(captor.getValue().getExtras());
        assertNotSame("SomeMessageId", message.getMessageId());
        assertEquals("campaignId", message.getGeo().getCampaignId());
        assertEquals("areaId", message.getGeo().getAreasList().get(0).getId());
    }
}
