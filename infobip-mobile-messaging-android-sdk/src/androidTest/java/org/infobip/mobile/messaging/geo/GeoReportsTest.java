package org.infobip.mobile.messaging.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventReportBody;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.sqlite.SqliteMessage;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.DebugServer;
import org.infobip.mobile.messaging.tools.Helper;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

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
    private MessageStore messageStore;
    private DebugServer debugServer;
    private GeoReporter geoReporter;
    private BroadcastReceiver geoReportedReceiver;
    private BroadcastReceiver seenReportedReceiver;
    private ArgumentCaptor<Intent> captor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getContext();

        debugServer = new DebugServer();
        debugServer.start();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();

        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");

        // Enable message store for notification messages
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());
        messageStore = MobileMessaging.getInstance(context).getMessageStore();
        messageStore.deleteAll(context);

        MobileMessagingCore.getDatabaseHelper(context).deleteAll(SqliteMessage.class);

        geoReporter = new GeoReporter();

        MobileApiResourceProvider.INSTANCE.resetMobileApi();

        captor = ArgumentCaptor.forClass(Intent.class);
        geoReportedReceiver = Mockito.mock(BroadcastReceiver.class);
        seenReportedReceiver = Mockito.mock(BroadcastReceiver.class);
        context.registerReceiver(geoReportedReceiver, new IntentFilter(Event.GEOFENCE_EVENTS_REPORTED.getKey()));
        context.registerReceiver(seenReportedReceiver, new IntentFilter(Event.SEEN_REPORTS_SENT.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        context.unregisterReceiver(geoReportedReceiver);
        context.unregisterReceiver(seenReportedReceiver);

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

        // Given
        Area area1 = Helper.createArea("areaId1");
        Area area2 = Helper.createArea("areaId2");
        Area area3 = Helper.createArea("areaId3");
        Helper.createMessage(context, "signalingMessageId1", "campaignId1", true, area1, area2);
        Helper.createMessage(context, "signalingMessageId2", "campaignId3", true, area3);
        Helper.createReport(context, "signalingMessageId1", "campaignId1", "messageId1", true, area1);
        Helper.createReport(context, "signalingMessageId1", "campaignId1", "messageId2", true, area2);
        Helper.createReport(context, "signalingMessageId2", "campaignId3", "messageId3", true, area3);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{}");

        // When
        geoReporter.report(context);

        // Then
        // Examine what is reported back via broadcast intent
        Mockito.verify(geoReportedReceiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());

        List<GeoReport> broadcastedReports = GeoReport.createFrom(captor.getValue().getExtras());
        assertEquals(broadcastedReports.size(), 3);

        Map<String, GeoReport> reportMap = new HashMap<>();
        for (GeoReport r : broadcastedReports) {
            reportMap.put(r.getMessageId(), r);
        }

        GeoReport geoReport1 = reportMap.get("messageId1");
        assertEquals(GeoEventType.entry, geoReport1.getEvent());
        assertEquals("campaignId1", geoReport1.getCampaignId());
        assertEquals("signalingMessageId1", geoReport1.getSignalingMessageId());
        assertEquals(geoReport1.getMessageId(), "messageId1");
        assertEquals(geoReport1.getArea().getId(), "areaId1");

        GeoReport geoReport2 = reportMap.get("messageId2");
        assertEquals(GeoEventType.entry, geoReport2.getEvent());
        assertEquals("campaignId1", geoReport2.getCampaignId());
        assertEquals("signalingMessageId1", geoReport2.getSignalingMessageId());
        assertEquals("messageId2", geoReport2.getMessageId());
        assertEquals("areaId2", geoReport2.getArea().getId());

        GeoReport geoReport3 = reportMap.get("messageId3");
        assertEquals(GeoEventType.entry, geoReport3.getEvent());
        assertEquals("campaignId3", geoReport3.getCampaignId());
        assertEquals("signalingMessageId2", geoReport3.getSignalingMessageId());
        assertEquals("messageId3", geoReport3.getMessageId());
        assertEquals("areaId3", geoReport3.getArea().getId());

        // Examine HTTP request body
        String stringBody = debugServer.getBody("geo/event");

        EventReportBody body = new JsonSerializer().deserialize(stringBody, EventReportBody.class);
        assertEquals(body.getReports().size(), 3);

        EventReport r[] = body.getReports().toArray(new EventReport[body.getReports().size()]);
        assertNotSame(r[0].getTimestampDelta(), r[1].getTimestampDelta());
        assertNotSame(r[0].getTimestampDelta(), r[2].getTimestampDelta());
        assertNotSame(r[1].getTimestampDelta(), r[2].getTimestampDelta());

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
                        "\"event\":\"entry\"," +
                        "\"geoAreaId\":\"areaId2\"," +
                        "\"messageId\":\"signalingMessageId1\"," +
                        "\"sdkMessageId\":\"messageId2\"," +
                        "\"campaignId\":\"campaignId1\"," +
                        "\"timestampDelta\":" + r[1].getTimestampDelta() +
                        "}," +
                        "{" +
                        "\"event\":\"entry\"," +
                        "\"geoAreaId\":\"areaId3\"," +
                        "\"messageId\":\"signalingMessageId2\"," +
                        "\"sdkMessageId\":\"messageId3\"," +
                        "\"campaignId\":\"campaignId3\"," +
                        "\"timestampDelta\":" + r[2].getTimestampDelta() +
                        "}" +
                        "]" +
                        "}"
                , stringBody, JSONCompareMode.LENIENT);
    }

    public void test_withNonActiveCampaigns() {

        // Given
        Area area1 = Helper.createArea("areaId1");
        Area area2 = Helper.createArea("areaId2");
        Area area3 = Helper.createArea("areaId3");
        Helper.createMessage(context, "signalingMessageId1", "campaignId1", true, area1, area2);
        Helper.createMessage(context, "signalingMessageId2", "campaignId3", true, area3);
        Helper.createReport(context, "signalingMessageId1", "campaignId1", "messageId1", true, area1);
        Helper.createReport(context, "signalingMessageId1", "campaignId1", "messageId2", true, area2);
        Helper.createReport(context, "signalingMessageId2", "campaignId3", "messageId3", true, area3);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{\n" +
                "  \"finishedCampaignIds\":[\"campaignId1\"],\n" +
                "  \"suspendedCampaignIds\":[\"campaignId2\"]\n" +
                "}");

        // When
        geoReporter.report(context);

        // Then
        // Examine what is reported back via broadcast intent
        Mockito.verify(geoReportedReceiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());

        List<GeoReport> broadcastedReports = GeoReport.createFrom(captor.getValue().getExtras());
        assertEquals(broadcastedReports.size(), 1);

        GeoReport geoReport = broadcastedReports.get(0);
        assertEquals(GeoEventType.entry, geoReport.getEvent());
        assertEquals("campaignId3", geoReport.getCampaignId());
        assertEquals("messageId3", geoReport.getMessageId());
        assertEquals("areaId3", geoReport.getArea().getId());

        final Set<String> finishedCampaignIds = PreferenceHelper.findStringSet(context, MobileMessagingProperty.FINISHED_CAMPAIGN_IDS);
        final Set<String> suspendedCampaignIds = PreferenceHelper.findStringSet(context, MobileMessagingProperty.SUSPENDED_CAMPAIGN_IDS);

        assertEquals(finishedCampaignIds.size(), 1);
        assertEquals(finishedCampaignIds.iterator().next(), "campaignId1");

        assertEquals(suspendedCampaignIds.size(), 1);
        assertEquals(suspendedCampaignIds.iterator().next(), "campaignId2");
    }

    public void test_shouldUpdateMessageIdsOnSuccessfulReport() throws Exception {

        // Given
        Area area1 = Helper.createArea("areaId1");
        Area area2 = Helper.createArea("areaId2");
        Area area3 = Helper.createArea("areaId3");
        Helper.createMessage(context, "signalingMessageId1", "campaignId1", true, area1, area2);
        Helper.createMessage(context, "signalingMessageId2", "campaignId2", true, area3);
        Helper.createReport(context, "signalingMessageId1", "campaignId1", "messageId1", true, area1);
        Helper.createReport(context, "signalingMessageId1", "campaignId1", "messageId2", true, area2);
        Helper.createReport(context, "signalingMessageId2", "campaignId2", "messageId3", true, area3);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{" +
                "'messageIds': {" +
                "   'messageId1':'ipCoreMessageId1'," +
                "   'messageId2':'ipCoreMessageId2'," +
                "   'messageId3':'ipCoreMessageId3'" +
                "}" +
                "}");

        // When
        geoReporter.report(context);

        // Then
        // Wait for reporting to complete
        Mockito.verify(geoReportedReceiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());

        // Examine message store
        List<Message> messageList = messageStore.findAll(context);
        assertEquals(2/*signaling*/ + 3/*generated*/, messageList.size());

        Map<String, Message> messageMap = new HashMap<>();
        for (Message m : messageList) messageMap.put(m.getMessageId(), m);

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

    public void test_shouldKeepGeneratedMessagesOnFailedReport() throws Exception {

        // Given
        Helper.createMessage(context,"signalingMessageId1", "campaignId1", true);
        Helper.createMessage(context,"signalingMessageId2", "campaignId2", true);
        Helper.createReport(context,"signalingMessageId1", "campaignId1", "messageId1", true);
        Helper.createReport(context,"signalingMessageId1", "campaignId1", "messageId2", true);
        Helper.createReport(context,"signalingMessageId2", "campaignId2", "messageId3", true);
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, null);

        // When
        geoReporter.report(context);

        // Then
        Mockito.verify(geoReportedReceiver, Mockito.after(2000).never()).onReceive(Mockito.any(Context.class), captor.capture());

        List<Message> messageList = messageStore.findAll(context);
        assertEquals(2/*signaling*/ + 3/*generated*/, messageList.size());

        Map<String, Message> messageMap = new HashMap<>();
        for (Message m : messageList) {
            messageMap.put(m.getMessageId(), m);
        }

        assertTrue(messageMap.containsKey("messageId1"));
        Message m1 = messageMap.get("messageId1");
        assertEquals("campaignId1", m1.getGeo().getCampaignId());

        assertTrue(messageMap.containsKey("messageId2"));
        Message m2 = messageMap.get("messageId2");
        assertEquals("campaignId1", m2.getGeo().getCampaignId());

        assertTrue(messageMap.containsKey("messageId3"));
        Message m3 = messageMap.get("messageId3");
        assertEquals("campaignId2", m3.getGeo().getCampaignId());
    }

    public void test_geoReportsShouldGenerateMessagesOnlyForActiveCampaigns() {

        // Given
        Helper.createMessage(context,"signalingMessageId1", "campaignId1", true);
        Helper.createMessage(context,"signalingMessageId2", "campaignId2", true);
        Helper.createReport(context,"signalingMessageId1", "campaignId1", "messageId1", true);
        Helper.createReport(context,"signalingMessageId1", "campaignId1", "messageId2", true);
        Helper.createReport(context,"signalingMessageId2", "campaignId2", "messageId3", true);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{" +
                "   'messageIds': {" +
                "   'messageId1':'ipCoreMessageId1'," +
                "   'messageId2':'ipCoreMessageId2'," +
                "   'messageId3':'ipCoreMessageId3'" +
                "   }," +
                "  'suspendedCampaignIds':['campaignId1']" +
                "}");

        // When
        geoReporter.report(context);

        // Then
        Mockito.verify(geoReportedReceiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
        List<Message> messageList = MobileMessaging.getInstance(context).getMessageStore().findAll(context);
        assertEquals(2 /*signaling*/ + 1 /*generated*/, messageList.size());

        Map<String, Message> messageMap = new HashMap<>();
        for (Message m : messageList) {
            messageMap.put(m.getMessageId(), m);
        }

        assertFalse(messageMap.containsKey("messageId1"));
        assertFalse(messageMap.containsKey("messageId2"));
        assertFalse(messageMap.containsKey("messageId3"));
        assertFalse(messageMap.containsKey("ipCoreMessageId1"));
        assertFalse(messageMap.containsKey("ipCoreMessageId2"));

        assertTrue(messageMap.containsKey("ipCoreMessageId3"));
        Message m3 = messageMap.get("ipCoreMessageId3");
        assertEquals("campaignId2", m3.getGeo().getCampaignId());
    }

    public void test_shouldReportSeenForMessageIdsIfNoCorrespondingGeoReport() {
        // Given
        Helper.createMessage(context,"generatedMessageId2", "campaignId2", true);
        Helper.createReport(context,"signalingMessageId1", "campaignId1", "generatedMessageId1", true);
        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        // When
        MobileMessaging.getInstance(context).setMessagesSeen("generatedMessageId2");

        // Then
        Mockito.verify(seenReportedReceiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
    }

    public void test_shouldNotReportSeenForMessageIdsGeneratedForGeoReports() {

        // Given
        Helper.createMessage(context,"signalingMessageId1", "campaignId1", true);
        Helper.createMessage(context,"generatedMessageId1", "campaignId1", true);
        Helper.createReport(context,"signalingMessageId1", "campaignId1", "generatedMessageId1", true);
        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        // When
        MobileMessaging.getInstance(context).setMessagesSeen("generatedMessageId1");

        // Then
        Mockito.verify(seenReportedReceiver, Mockito.after(1000).never()).onReceive(Mockito.any(Context.class), captor.capture());
    }

    public void test_shouldReportSeenAfterGeoSuccessfullyReported() {

        // Given
        Helper.createMessage(context,"signalingMessageId1", "campaignId1", true);
        Helper.createMessage(context,"generatedMessageId1", "campaignId1", true);
        Helper.createReport(context,"signalingMessageId1", "campaignId1", "generatedMessageId1", true);
        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{}");

        // When
        MobileMessaging.getInstance(context).setMessagesSeen("generatedMessageId1");
        geoReporter.report(context);

        // Then
        Mockito.verify(seenReportedReceiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
    }
}