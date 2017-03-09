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
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.DebugServer;
import org.infobip.mobile.messaging.tools.Helper;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author sslavin
 * @since 13/02/2017.
 */

public class GeoTransitionsTest extends InstrumentationTestCase {

    private Context context;
    private GeoAreasHandler handler;
    private GeoReporter geoReporter;
    private DebugServer debugServer;
    private BroadcastReceiver messageReceiver;
    private BroadcastReceiver geoEnteredReceiver;
    private ArgumentCaptor<Intent> captor;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext();
        handler = new GeoAreasHandler(context);
        messageReceiver = Mockito.mock(BroadcastReceiver.class);
        geoEnteredReceiver = Mockito.mock(BroadcastReceiver.class);
        captor = ArgumentCaptor.forClass(Intent.class);
        geoReporter = new GeoReporter();
        debugServer = new DebugServer();
        debugServer.start();
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, null);

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());
        LocalBroadcastManager.getInstance(context).registerReceiver(messageReceiver, new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
        LocalBroadcastManager.getInstance(context).registerReceiver(geoEnteredReceiver, new IntentFilter(Event.GEOFENCE_AREA_ENTERED.getKey()));
        MobileMessagingCore.getInstance(context).getMessageStoreForGeo().deleteAll(context);
        MobileMessagingCore.getInstance(context).removeUnreportedGeoEvents();
        MobileMessaging.getInstance(context).getMessageStore().deleteAll(context);
        MobileMessagingLogger.enforce();
    }

    @Override
    protected void tearDown() throws Exception {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageReceiver);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(geoEnteredReceiver);

        if (null != debugServer) {
            try {
                debugServer.stop();
            } catch (Exception e) {
                //ignore
            }
        }

        super.tearDown();
    }

    public void test_shouldNotify_MESSAGE_RECEIVED_forTransition() throws Exception {

        // Given
        Area area = Helper.createArea("areaId1", "areaTitle", 1.0, 2.0, 3);
        Helper.createMessage(context, "signalingMessageId", "campaignId1", true, area);
        GeoTransition geoTransition = GeoHelper.createTransition("areaId1");

        // When
        handler.handleTransition(geoTransition);

        // Then
        Mockito.verify(messageReceiver, Mockito.atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
        Message message = Message.createFrom(captor.getValue().getExtras());
        assertNotSame("signalingMessageId", message.getMessageId());
        assertEquals("campaignId1", message.getGeo().getCampaignId());
        assertEquals(1, message.getGeo().getAreasList().size());
        Helper.assertEquals(area, message.getGeo().getAreasList().get(0));
    }

    public void test_shouldNotify_GEOFENCE_ENTERED_forTransition() throws Exception {

        // Given
        Area area = Helper.createArea("areaId1", "areaTitle", 1.1, 2.2, 3);
        Geo geo = Helper.createGeo(1.0, 2.0, "campaignId", area);
        Helper.createMessage(context, "signalingMessageId", true, geo);
        GeoTransition geoTransition = GeoHelper.createTransition(1.0, 2.0, "areaId1");

        // When
        handler.handleTransition(geoTransition);

        // Then
        Mockito.verify(geoEnteredReceiver, Mockito.atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
        Helper.assertEquals(geo, Geo.createFrom(captor.getValue().getExtras()));
        Message message = Message.createFrom(captor.getValue().getExtras());
        Helper.assertEquals(geo, message.getGeo());
        assertNotSame("signalingMessageId", message.getMessageId());
    }

    public void test_shouldSaveGeneratedMessageToStoreIfStoreEnabled() throws Exception {

        // Given
        Area area = Helper.createArea("areaId1", "areaTitle", 1.1, 2.2, 3);
        Geo geo = Helper.createGeo(1.0, 2.0, "campaignId", area);
        Helper.createMessage(context, "signalingMessageId", true, geo);
        GeoTransition geoTransition = GeoHelper.createTransition(1.0, 2.0, "areaId1");
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());
        MobileMessaging.getInstance(context).getMessageStore().deleteAll(context);

        // When
        handler.handleTransition(geoTransition);

        // Then
        List<Message> messages = MobileMessaging.getInstance(context).getMessageStore().findAll(context);
        assertEquals(1, messages.size());
        Helper.assertEquals(geo, messages.get(0).getGeo());
        assertNotSame("signalingMessageId", messages.get(0).getMessageId());
    }

    public void test_shouldProvideEventsOnlyForTransitionNotForReporting() throws Exception {

        // Given
        Area area = Helper.createArea("areaId1", "areaTitle", 1.1, 2.2, 3);
        Geo geo = Helper.createGeo(1.0, 2.0, "campaignId", area);
        Helper.createMessage(context, "signalingMessageId", true, geo);
        GeoTransition geoTransition = GeoHelper.createTransition(1.0, 2.0, "areaId1");

        // When
        handler.handleTransition(geoTransition);
        geoReporter.report(context);

        // Then
        Mockito.verify(messageReceiver, Mockito.after(1000).atMost(1)).onReceive(Mockito.any(Context.class), captor.capture());
        Message message = Message.createFrom(captor.getValue().getExtras());
        Helper.assertEquals(geo, message.getGeo());
        assertNotSame("signalingMessageId", message.getMessageId());
        Mockito.verify(geoEnteredReceiver, Mockito.atMost(1)).onReceive(Mockito.any(Context.class), captor.capture());
        Helper.assertEquals(geo, Geo.createFrom(captor.getValue().getExtras()));
        message = Message.createFrom(captor.getValue().getExtras());
        Helper.assertEquals(geo, message.getGeo());
        assertNotSame("signalingMessageId", message.getMessageId());
    }

    public void test_geoReportsShouldGenerateMessagesOnlyForActiveCampaigns() {

        // Given
        Area area = Helper.createArea("areaId1");
        Helper.createMessage(context,"signalingMessageId1", "campaignId1", true, area);
        Helper.createMessage(context,"signalingMessageId2", "campaignId2", true, area);
        GeoTransition transition = GeoHelper.createTransition(area.getId());
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{" +
                "  'suspendedCampaignIds':['campaignId1']" +
                "}");

        // When
        handler.handleTransition(transition);

        // Then
        Mockito.verify(messageReceiver, Mockito.after(1000).times(1)).onReceive(Mockito.any(Context.class), captor.capture());
        List<Message> messages = MobileMessaging.getInstance(context).getMessageStore().findAll(context);
        assertEquals(1, messages.size());
        assertEquals("campaignId2", messages.get(0).getGeo().getCampaignId());
    }

    public void test_shouldKeepGeneratedMessagesOnFailedReport() throws Exception {

        // Given
        Area area = Helper.createArea("areaId1");
        Helper.createMessage(context,"signalingMessageId1", "campaignId1", true, area);
        Helper.createMessage(context,"signalingMessageId2", "campaignId2", true, area);
        GeoTransition transition = GeoHelper.createTransition(area.getId());
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, null);

        // When
        handler.handleTransition(transition);

        // Then
        Mockito.verify(messageReceiver, Mockito.after(2000).times(2)).onReceive(Mockito.any(Context.class), captor.capture());
        List<Message> messages = MobileMessaging.getInstance(context).getMessageStore().findAll(context);
        assertEquals(2, messages.size());
        List<String> campaignIds = Arrays.asList(messages.get(0).getGeo().getCampaignId(), messages.get(1).getGeo().getCampaignId());
        assertTrue(campaignIds.contains("campaignId1"));
        assertTrue(campaignIds.contains("campaignId2"));
    }
}
