package org.infobip.mobile.messaging.geo;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventReportBody;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.iki.elonen.NanoHTTPD;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

public class GeoReporterTest extends MobileMessagingTestCase {

    private MessageStore messageStore;
    private GeoReporter geoReporter;

    private ArgumentCaptor<List<GeoReport>> geoReportCaptor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Enable message store for notification messages
        enableMessageStoreForReceivedMessages();

        messageStore = MobileMessaging.getInstance(context).getMessageStore();
        geoReporter = new GeoReporter(context, broadcaster, MobileMessagingCore.getInstance(context).getStats());
        geoReportCaptor = new ArgumentCaptor<>();
    }

    @Test
    public void test_success() throws Exception {

        // Given
        Area area1 = createArea("areaId1");
        Area area2 = createArea("areaId2");
        Area area3 = createArea("areaId3");
        createMessage(context, "signalingMessageId1", "campaignId1", true, area1, area2);
        createMessage(context, "signalingMessageId2", "campaignId3", true, area3);
        createReport(context, "signalingMessageId1", "campaignId1", "messageId1", true, area1);
        createReport(context, "signalingMessageId1", "campaignId1", "messageId2", true, area2);
        createReport(context, "signalingMessageId2", "campaignId3", "messageId3", true, area3);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{}");

        // When
        geoReporter.synchronize();

        // Then
        // Examine what is reported back via broadcast intent
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).geoReported(geoReportCaptor.capture());

        List<GeoReport> broadcastedReports = geoReportCaptor.getValue();
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

    @Test
    public void test_withNonActiveCampaigns() throws InterruptedException {

        // Given
        Area area1 = createArea("areaId1");
        Area area2 = createArea("areaId2");
        Area area3 = createArea("areaId3");
        createMessage(context, "signalingMessageId1", "campaignId1", true, area1, area2);
        createMessage(context, "signalingMessageId2", "campaignId3", true, area3);
        createReport(context, "signalingMessageId1", "campaignId1", "messageId1", true, area1);
        createReport(context, "signalingMessageId1", "campaignId1", "messageId2", true, area2);
        createReport(context, "signalingMessageId2", "campaignId3", "messageId3", true, area3);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{\n" +
                "  \"finishedCampaignIds\":[\"campaignId1\"],\n" +
                "  \"suspendedCampaignIds\":[\"campaignId2\"]\n" +
                "}");

        // When
        geoReporter.synchronize();

        // Then
        // Examine what is reported back via broadcast intent
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).geoReported(geoReportCaptor.capture());

        List<GeoReport> broadcastedReports = geoReportCaptor.getValue();
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

    @Test
    public void test_shouldUpdateMessageIdsOnSuccessfulReport() throws Exception {

        // Given
        Area area1 = createArea("areaId1");
        Area area2 = createArea("areaId2");
        Area area3 = createArea("areaId3");
        createMessage(context, "signalingMessageId1", "campaignId1", true, area1, area2);
        createMessage(context, "signalingMessageId2", "campaignId2", true, area3);
        createReport(context, "signalingMessageId1", "campaignId1", "messageId1", true, area1);
        createReport(context, "signalingMessageId1", "campaignId1", "messageId2", true, area2);
        createReport(context, "signalingMessageId2", "campaignId2", "messageId3", true, area3);
        messageStore.save(context,
            createMessage(context, "messageId1", "campaignId1", false, area1),
            createMessage(context, "messageId2", "campaignId1", false, area2),
            createMessage(context, "messageId3", "campaignId2", false, area3));
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{" +
                "'messageIds': {" +
                "   'messageId1':'ipCoreMessageId1'," +
                "   'messageId2':'ipCoreMessageId2'," +
                "   'messageId3':'ipCoreMessageId3'" +
                "}" +
                "}");

        // When
        geoReporter.synchronize();

        // Then
        // Wait for reporting to complete
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).geoReported(geoReportCaptor.capture());

        // Examine message store
        List<Message> messageList = messageStore.findAll(context);
        assertEquals(3, messageList.size());

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

    @Test
    public void test_shouldReportSeenForMessageIdsIfNoCorrespondingGeoReport() throws InterruptedException {
        // Given
        createMessage(context,"generatedMessageId1", true);
        createReport(context,"signalingMessageId1", "campaignId1", "generatedMessageId2", true);
        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        // When
        mobileMessaging.setMessagesSeen("generatedMessageId1");

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).seenStatusReported(Mockito.any(String[].class));
    }

    @Test
    public void test_shouldNotReportSeenForMessageIdsGeneratedForGeoReports() throws InterruptedException {

        // Given
        createMessage(context,"signalingMessageId1", "campaignId1", true);
        createMessage(context,"generatedMessageId1", "campaignId1", true);
        createReport(context,"signalingMessageId1", "campaignId1", "generatedMessageId1", true);
        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        // When
        mobileMessaging.setMessagesSeen("generatedMessageId1");

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).never()).seenStatusReported(Mockito.any(String[].class));
    }

    @Test
    public void test_shouldReportSeenAfterGeoSuccessfullyReported() throws InterruptedException {

        // Given
        createMessage(context,"signalingMessageId1", "campaignId1", true);
        createMessage(context,"generatedMessageId1", "campaignId1", true);
        createReport(context,"signalingMessageId1", "campaignId1", "generatedMessageId1", true);
        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{}");

        // When
        mobileMessaging.setMessagesSeen("generatedMessageId1");
        geoReporter.synchronize();

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).seenStatusReported(Mockito.any(String[].class));
    }
}