package org.infobip.mobile.messaging.geo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.test.InstrumentationTestCase;

import com.google.android.gms.location.Geofence;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 15/02/2017.
 */

public class GeoHandlerTests extends InstrumentationTestCase {

    private Context context;
    private GeoAreasHandler handler;
    private MessageStore commonStore;
    private MessageStore geoStore;
    private BroadcastReceiver messageReceiver;
    private ArgumentCaptor<Intent> captor;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());

        handler = new GeoAreasHandler(context);
        commonStore = MobileMessaging.getInstance(context).getMessageStore();
        commonStore.deleteAll(context);
        geoStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
        geoStore.deleteAll(context);
        messageReceiver = Mockito.mock(BroadcastReceiver.class);
        captor = ArgumentCaptor.forClass(Intent.class);

        LocalBroadcastManager.getInstance(context).registerReceiver(messageReceiver, new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageReceiver);
        super.tearDown();
    }

    public void test_shoudProvide_MESSAGE_RECEIVED_forGeoTransition() throws Exception {

        // Given
        final Area area = new Area("areaId", "", 1.0, 2.0, 3);
        Geo geo = new Geo(1.0, 2.0, new ArrayList<Area>(){{add(area);}}, null, new ArrayList<GeoEvent>(), null, null, "campaignId");
        Message m = new Message();
        m.setMessageId("SomeMessageId");
        m.setBody("SomeMessageBody");
        m.setInternalData(new JSONObject(new JsonSerializer().serialize(geo)));
        m.setGeo(geo);
        MobileMessagingCore.getInstance(context).getMessageStoreForGeo().save(context, m);
        List<Geofence> geofences = new ArrayList<Geofence>(){{add(new Geofence() {
            @Override
            public String getRequestId() {
                return "areaId";
            }
        });}};

        // When
        handler.handleTransition(geofences, Geofence.GEOFENCE_TRANSITION_ENTER, new Location(""){{
            setLatitude(1.0);
            setLongitude(2.0);
        }});

        // Then
        Mockito.verify(messageReceiver, Mockito.atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
        Message message = Message.createFrom(captor.getValue().getExtras());
        assertNotSame("SomeMessageId", message.getMessageId());
        assertEquals("campaignId", message.getGeo().getCampaignId());
        assertEquals("areaId", message.getGeo().getAreasList().get(0).getId());
    }
}
