package org.infobip.mobile.messaging.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventReportBody;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.DebugServer;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

public class GeoReportsTest extends InstrumentationTestCase {

    private Context context;
    private DebugServer debugServer;
    private GeoReporter geoReporter;
    private BroadcastReceiver receiver;
    private ArgumentCaptor<Intent> captor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getContext();

        debugServer = new DebugServer();
        debugServer.start();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();

        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");

        geoReporter = new GeoReporter();

        MobileApiResourceProvider.INSTANCE.resetMobileApi();

        captor = ArgumentCaptor.forClass(Intent.class);
        receiver = Mockito.mock(BroadcastReceiver.class);
        context.registerReceiver(receiver, new IntentFilter(Event.GEOFENCE_EVENTS_REPORTED.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        context.unregisterReceiver(receiver);

        if (null != debugServer) {
            try {
                debugServer.stop();
            } catch (Exception e) {
                //ignore
            }
        }

        super.tearDown();
    }

    public void test_success() throws Exception {

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{}");

        final GeoReport reports[] = new GeoReport[3];
        reports[0] = new GeoReport("campaignId1", "messageId1", "signalingMessageId1", GeoEventType.entry, new Area("areaId1", "Area1", 1.0, 1.0, 3), 1001L, new GeoReport.GeoLatLng(1.0, 2.0));
        reports[1] = new GeoReport("campaignId1", "messageId2", "signalingMessageId1", GeoEventType.exit, new Area("areaId2", "Area2", 2.0, 2.0, 4), 1002L, new GeoReport.GeoLatLng(3.0, 4.0));
        reports[2] = new GeoReport("campaignId3", "messageId3", "signalingMessageId2", GeoEventType.dwell, new Area("areaId3", "Area3", 3.0, 3.0, 5), 1003L, new GeoReport.GeoLatLng(5.0, 6.0));

        Message messages[] = new Message[2];
        messages[0] = new Message();
        messages[0].setMessageId("signalingMessageId1");
        messages[0].setGeo(new Geo(null, null, null, null, null, "campaignId1", new ArrayList<Area>(){{
            add(reports[0].getArea());
            add(reports[1].getArea());
        }}, null));
        messages[1] = new Message();
        messages[1].setMessageId("signalingMessageId2");
        messages[1].setGeo(new Geo(null, null, null, null, null, "campaignId2", new ArrayList<Area>(){{
            add(reports[2].getArea());
        }}, null));

        MobileMessagingCore.getInstance(context).getMessageStoreForGeo().deleteAll(context);
        MobileMessagingCore.getInstance(context).getMessageStoreForGeo().save(context, messages);
        MobileMessagingCore.getInstance(context).addUnreportedGeoEvents(reports);
        geoReporter.report(context);

        // Examine what is reported back via broadcast intent

        Mockito.verify(receiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());

        List<GeoReport> broadcastedReports = GeoReport.createFrom(captor.getValue().getExtras());
        assertEquals(broadcastedReports.size(), 3);

        Map<String, GeoReport> reportMap = new HashMap<>();
        for (GeoReport r : broadcastedReports) {
            reportMap.put(r.getMessageId(), r);
        }

        GeoReport geoReport1 = reportMap.get("messageId1");
        assertEquals(geoReport1.getEvent(), GeoEventType.entry);
        assertEquals(geoReport1.getCampaignId(), "campaignId1");
        assertEquals("signalingMessageId1", geoReport1.getSignalingMessageId());
        assertEquals(geoReport1.getMessageId(), "messageId1");
        assertEquals(geoReport1.getTimestampOccurred(), (Long) 1001L);
        assertEquals(geoReport1.getArea().getId(), "areaId1");
        assertEquals(geoReport1.getArea().getTitle(), "Area1");
        assertEquals(geoReport1.getArea().getLatitude(), 1.0);
        assertEquals(geoReport1.getArea().getLongitude(), 1.0);
        assertEquals(geoReport1.getArea().getRadius(), (Integer) 3);

        GeoReport geoReport2 = reportMap.get("messageId2");
        assertEquals(geoReport2.getEvent(), GeoEventType.exit);
        assertEquals(geoReport2.getCampaignId(), "campaignId1");
        assertEquals("signalingMessageId1", geoReport2.getSignalingMessageId());
        assertEquals(geoReport2.getMessageId(), "messageId2");
        assertEquals(geoReport2.getTimestampOccurred(), (Long) 1002L);
        assertEquals(geoReport2.getArea().getId(), "areaId2");
        assertEquals(geoReport2.getArea().getTitle(), "Area2");
        assertEquals(geoReport2.getArea().getLatitude(), 2.0);
        assertEquals(geoReport2.getArea().getLongitude(), 2.0);
        assertEquals(geoReport2.getArea().getRadius(), (Integer) 4);

        GeoReport geoReport3 = reportMap.get("messageId3");
        assertEquals(geoReport3.getEvent(), GeoEventType.dwell);
        assertEquals(geoReport3.getCampaignId(), "campaignId3");
        assertEquals("signalingMessageId2", geoReport3.getSignalingMessageId());
        assertEquals(geoReport3.getMessageId(), "messageId3");
        assertEquals(geoReport3.getTimestampOccurred(), (Long) 1003L);
        assertEquals(geoReport3.getArea().getId(), "areaId3");
        assertEquals(geoReport3.getArea().getTitle(), "Area3");
        assertEquals(geoReport3.getArea().getLatitude(), 3.0);
        assertEquals(geoReport3.getArea().getLongitude(), 3.0);
        assertEquals(geoReport3.getArea().getRadius(), (Integer) 5);

        // Examine HTTP request body

        String stringBody = debugServer.getBody();
        EventReportBody body = new JsonSerializer().deserialize(stringBody, EventReportBody.class);

        assertEquals(body.getReports().size(), 3);
        EventReport r[] = body.getReports().toArray(new EventReport[body.getReports().size()]);
        JSONAssert.assertEquals(
                "{" +
                        "\"messages\": [" +
                        "{" +
                        "\"messageId\":\"signalingMessageId1\"" +
                        "}," +
                        "{" +
                        "\"messageId\":\"signalingMessageId2\"" +
                        "}" +
                        "]," +
                        "\"reports\": [" +
                        "{" +
                        "\"event\":\"entry\"," +
                        "\"geoAreaId\":\"areaId1\"," +
                        "\"messageId\":\"signalingMessageId1\"," +
                        "\"sdkMessageId\":\"messageId1\"," +
                        "\"campaignId\":\"campaignId1\"," +
                        "\"timestampDelta\":" + r[0].getTimestampDelta() +
                        "}," +
                        "{" +
                        "\"event\":\"exit\"," +
                        "\"geoAreaId\":\"areaId2\"," +
                        "\"messageId\":\"signalingMessageId1\"," +
                        "\"sdkMessageId\":\"messageId2\"," +
                        "\"campaignId\":\"campaignId1\"," +
                        "\"timestampDelta\":" + r[1].getTimestampDelta() +
                        "}," +
                        "{" +
                        "\"event\":\"dwell\"," +
                        "\"geoAreaId\":\"areaId3\"," +
                        "\"messageId\":\"signalingMessageId2\"," +
                        "\"sdkMessageId\":\"messageId3\"," +
                        "\"campaignId\":\"campaignId3\"," +
                        "\"timestampDelta\":" + r[2].getTimestampDelta() +
                        "}" +
                        "]" +
                        "}"
                , debugServer.getBody(), JSONCompareMode.LENIENT);

        assertNotSame(r[0].getTimestampDelta(), r[1].getTimestampDelta());
        assertNotSame(r[0].getTimestampDelta(), r[2].getTimestampDelta());
        assertNotSame(r[1].getTimestampDelta(), r[2].getTimestampDelta());
    }

    public void test_withNonActiveCampaigns() {
        String jsonResponse = "{\n" +
                "  \"finishedCampaignIds\":[\"campaignId1\"],\n" +
                "  \"suspendedCampaignIds\":[\"campaignId2\"]\n" +
                "}";

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);

        final GeoReport reports[] = new GeoReport[3];
        reports[0] = new GeoReport("campaignId1", "messageId1", "signalingMessageId1", GeoEventType.entry, new Area("areaId1", "Area1", 1.0, 1.0, 3), 1001L, new GeoReport.GeoLatLng(1.0, 2.0));
        reports[1] = new GeoReport("campaignId1", "messageId2", "signalingMessageId1", GeoEventType.exit, new Area("areaId2", "Area2", 2.0, 2.0, 4), 1002L, new GeoReport.GeoLatLng(3.0, 4.0));
        reports[2] = new GeoReport("campaignId3", "messageId3", "signalingMessageId2", GeoEventType.dwell, new Area("areaId3", "Area3", 3.0, 3.0, 5), 1003L, new GeoReport.GeoLatLng(5.0, 6.0));

        Message messages[] = new Message[2];
        messages[0] = new Message();
        messages[0].setMessageId("signalingMessageId1");
        messages[0].setGeo(new Geo(null, null, null, null, null, "campaignId1", new ArrayList<Area>(){{
            add(reports[0].getArea());
            add(reports[1].getArea());
        }}, null));
        messages[1] = new Message();
        messages[1].setMessageId("signalingMessageId2");
        messages[1].setGeo(new Geo(null, null, null, null, null, "campaignId2", new ArrayList<Area>(){{
            add(reports[2].getArea());
        }}, null));

        MobileMessagingCore.getInstance(context).getMessageStoreForGeo().deleteAll(context);
        MobileMessagingCore.getInstance(context).getMessageStoreForGeo().save(context, messages);
        MobileMessagingCore.getInstance(context).addUnreportedGeoEvents(reports);
        geoReporter.report(context);


        // Examine what is reported back via broadcast intent
        Mockito.verify(receiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());

        List<GeoReport> broadcastedReports = GeoReport.createFrom(captor.getValue().getExtras());
        assertEquals(broadcastedReports.size(), 1);

        GeoReport geoReport = broadcastedReports.get(0);
        assertEquals(geoReport.getEvent(), GeoEventType.dwell);
        assertEquals(geoReport.getCampaignId(), "campaignId3");
        assertEquals(geoReport.getMessageId(), "messageId3");
        assertEquals(geoReport.getTimestampOccurred(), (Long) 1003L);
        assertEquals(geoReport.getArea().getId(), "areaId3");
        assertEquals(geoReport.getArea().getTitle(), "Area3");
        assertEquals(geoReport.getArea().getLatitude(), 3.0);
        assertEquals(geoReport.getArea().getLongitude(), 3.0);
        assertEquals(geoReport.getArea().getRadius(), (Integer) 5);

        final Set<String> finishedCampaignIds = PreferenceHelper.findStringSet(context, MobileMessagingProperty.FINISHED_CAMPAIGN_IDS);
        final Set<String> suspendedCampaignIds = PreferenceHelper.findStringSet(context, MobileMessagingProperty.SUSPENDED_CAMPAIGN_IDS);

        assertEquals(finishedCampaignIds.size(), 1);
        assertEquals(finishedCampaignIds.iterator().next(), "campaignId1");

        assertEquals(suspendedCampaignIds.size(), 1);
        assertEquals(suspendedCampaignIds.iterator().next(), "campaignId2");
    }

    public void test_shouldUpdateMessageIdsOnSuccessfulReport() throws Exception {

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{" +
                "'messageIds': {" +
                "   'messageId1':'ipCoreMessageId1'," +
                "   'messageId2':'ipCoreMessageId2'," +
                "   'messageId3':'ipCoreMessageId3'" +
                "}" +
                "}");

        final GeoReport reports[] = new GeoReport[3];
        reports[0] = new GeoReport("campaignId1", "messageId1", "signalingMessageId1", GeoEventType.entry, new Area("areaId1", "Area1", 1.0, 1.0, 3), 1001L, new GeoReport.GeoLatLng(1.0, 2.0));
        reports[1] = new GeoReport("campaignId1", "messageId2", "signalingMessageId1", GeoEventType.exit, new Area("areaId2", "Area2", 2.0, 2.0, 4), 1002L, new GeoReport.GeoLatLng(3.0, 4.0));
        reports[2] = new GeoReport("campaignId2", "messageId3", "signalingMessageId2", GeoEventType.dwell, new Area("areaId3", "Area3", 3.0, 3.0, 5), 1003L, new GeoReport.GeoLatLng(5.0, 6.0));

        Message messages[] = new Message[2];
        messages[0] = new Message();
        messages[0].setMessageId("signalingMessageId1");
        messages[0].setGeo(new Geo(null, null, null, null, null, "campaignId1", new ArrayList<Area>(){{
            add(reports[0].getArea());
            add(reports[1].getArea());
        }}, null));
        messages[1] = new Message();
        messages[1].setMessageId("signalingMessageId2");
        messages[1].setGeo(new Geo(null, null, null, null, null, "campaignId2", new ArrayList<Area>(){{
            add(reports[2].getArea());
        }}, null));

        // Enable message store
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());

        MessageStore messageStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
        messageStore.deleteAll(context);
        messageStore.save(context, messages);
        MobileMessagingCore.getInstance(context).addUnreportedGeoEvents(reports);

        geoReporter.report(context);

        // Wait for reporting to complete
        Mockito.verify(receiver, Mockito.after(2000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());

        // Examine message store
        List<Message> messageList = messageStore.findAll(context);
        assertEquals(messages.length + reports.length, messageList.size());

        Map<String, Message> messageMap = new HashMap<>();
        for (Message m : messageList) {
            messageMap.put(m.getMessageId(), m);
        }

        assertTrue(messageMap.containsKey("ipCoreMessageId1"));
        Message m1 = messageMap.get("ipCoreMessageId1");
        assertEquals("campaignId1", m1.getGeo().getCampaignId());
        assertEquals("areaId1", m1.getGeo().getAreasList().get(0).getId());

        assertTrue(messageMap.containsKey("ipCoreMessageId2"));
        Message m2 = messageMap.get("ipCoreMessageId2");
        assertEquals("campaignId1", m2.getGeo().getCampaignId());
        assertEquals("areaId2", m2.getGeo().getAreasList().get(0).getId());

        assertTrue(messageMap.containsKey("ipCoreMessageId3"));
        Message m3 = messageMap.get("ipCoreMessageId3");
        assertEquals("campaignId2", m3.getGeo().getCampaignId());
        assertEquals("areaId3", m3.getGeo().getAreasList().get(0).getId());
    }

    public void test_shouldKeepGeneratedIdsOnFailedReport() throws Exception {

        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, null);

        final GeoReport reports[] = new GeoReport[3];
        reports[0] = new GeoReport("campaignId1", "messageId1", "signalingMessageId1", GeoEventType.entry, new Area("areaId1", "Area1", 1.0, 1.0, 3), 1001L, new GeoReport.GeoLatLng(1.0, 2.0));
        reports[1] = new GeoReport("campaignId1", "messageId2", "signalingMessageId1", GeoEventType.exit, new Area("areaId2", "Area2", 2.0, 2.0, 4), 1002L, new GeoReport.GeoLatLng(3.0, 4.0));
        reports[2] = new GeoReport("campaignId2", "messageId3", "signalingMessageId2", GeoEventType.dwell, new Area("areaId3", "Area3", 3.0, 3.0, 5), 1003L, new GeoReport.GeoLatLng(5.0, 6.0));

        Message messages[] = new Message[2];
        messages[0] = new Message();
        messages[0].setMessageId("signalingMessageId1");
        messages[0].setGeo(new Geo(null, null, null, null, null, "campaignId1", new ArrayList<Area>(){{
            add(reports[0].getArea());
            add(reports[1].getArea());
        }}, null));
        messages[1] = new Message();
        messages[1].setMessageId("signalingMessageId2");
        messages[1].setGeo(new Geo(null, null, null, null, null, "campaignId2", new ArrayList<Area>(){{
            add(reports[2].getArea());
        }}, null));

        // Enable message store
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());

        MessageStore messageStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
        messageStore.deleteAll(context);
        messageStore.save(context, messages);
        MobileMessagingCore.getInstance(context).addUnreportedGeoEvents(reports);

        geoReporter.report(context);

        // Wait for reporting to complete
        Mockito.verify(receiver, Mockito.after(2000).never()).onReceive(Mockito.any(Context.class), captor.capture());

        // Examine message store
        List<Message> messageList = messageStore.findAll(context);
        assertEquals(messages.length + reports.length, messageList.size());

        Map<String, Message> messageMap = new HashMap<>();
        for (Message m : messageList) {
            messageMap.put(m.getMessageId(), m);
        }

        assertTrue(messageMap.containsKey("messageId1"));
        Message m1 = messageMap.get("messageId1");
        assertEquals("campaignId1", m1.getGeo().getCampaignId());
        assertEquals("areaId1", m1.getGeo().getAreasList().get(0).getId());

        assertTrue(messageMap.containsKey("messageId2"));
        Message m2 = messageMap.get("messageId2");
        assertEquals("campaignId1", m2.getGeo().getCampaignId());
        assertEquals("areaId2", m2.getGeo().getAreasList().get(0).getId());

        assertTrue(messageMap.containsKey("messageId3"));
        Message m3 = messageMap.get("messageId3");
        assertEquals("campaignId2", m3.getGeo().getCampaignId());
        assertEquals("areaId3", m3.getGeo().getAreasList().get(0).getId());
    }

    public void test_shouldSaveOnlyActiveMessages() {

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{" +
                "   'messageIds': {" +
                    "   'messageId1':'ipCoreMessageId1'," +
                    "   'messageId2':'ipCoreMessageId2'," +
                    "   'messageId3':'ipCoreMessageId3'" +
                "   }," +
                "  'finishedCampaignIds':['campaignId1']," +
                "  'suspendedCampaignIds':['campaignId2']" +
                "}");

        final GeoReport reports[] = new GeoReport[3];
        reports[0] = new GeoReport("campaignId1", "messageId1", "signalingMessageId1", GeoEventType.entry, new Area("areaId1", "Area1", 1.0, 1.0, 3), 1001L, new GeoReport.GeoLatLng(1.0, 2.0));
        reports[1] = new GeoReport("campaignId1", "messageId2", "signalingMessageId1", GeoEventType.exit, new Area("areaId2", "Area2", 2.0, 2.0, 4), 1002L, new GeoReport.GeoLatLng(3.0, 4.0));
        reports[2] = new GeoReport("campaignId3", "messageId3", "signalingMessageId2", GeoEventType.dwell, new Area("areaId3", "Area3", 3.0, 3.0, 5), 1003L, new GeoReport.GeoLatLng(5.0, 6.0));

        Message messages[] = new Message[2];
        messages[0] = new Message();
        messages[0].setMessageId("signalingMessageId1");
        messages[0].setGeo(new Geo(null, null, null, null, null, "campaignId1", new ArrayList<Area>(){{
            add(reports[0].getArea());
            add(reports[1].getArea());
        }}, null));
        messages[1] = new Message();
        messages[1].setMessageId("signalingMessageId2");
        messages[1].setGeo(new Geo(null, null, null, null, null, "campaignId3", new ArrayList<Area>(){{
            add(reports[2].getArea());
        }}, null));

        // Enable message store
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());

        MessageStore messageStore = MobileMessagingCore.getInstance(context).getMessageStoreForGeo();
        messageStore.deleteAll(context);
        messageStore.save(context, messages);
        MobileMessagingCore.getInstance(context).addUnreportedGeoEvents(reports);

        geoReporter.report(context);

        // Wait for reporting to complete
        Mockito.verify(receiver, Mockito.after(2000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());

        // Examine message store
        List<Message> messageList = messageStore.findAll(context);
        assertEquals(messages.length + 1, messageList.size());

        Map<String, Message> messageMap = new HashMap<>();
        for (Message m : messageList) {
            messageMap.put(m.getMessageId(), m);
        }

        assertFalse(messageMap.containsKey("messageId1"));
        assertFalse(messageMap.containsKey("messageId2"));
        assertFalse(messageMap.containsKey("ipCoreMessageId1"));
        assertFalse(messageMap.containsKey("ipCoreMessageId2"));

        assertTrue(messageMap.containsKey("ipCoreMessageId3"));
        Message m3 = messageMap.get("ipCoreMessageId3");
        assertEquals("campaignId3", m3.getGeo().getCampaignId());
        assertEquals("areaId3", m3.getGeo().getAreasList().get(0).getId());
    }
}