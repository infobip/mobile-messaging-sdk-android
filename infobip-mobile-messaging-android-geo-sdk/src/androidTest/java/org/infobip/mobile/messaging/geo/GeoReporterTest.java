package org.infobip.mobile.messaging.geo;

import androidx.collection.ArraySet;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventReportBody;
import org.infobip.mobile.messaging.api.geo.EventReportResponse;
import org.infobip.mobile.messaging.api.geo.EventType;
import org.infobip.mobile.messaging.api.geo.MessagePayload;
import org.infobip.mobile.messaging.api.geo.MobileApiGeo;
import org.infobip.mobile.messaging.geo.mapper.GeoDataMapper;
import org.infobip.mobile.messaging.geo.report.GeoReport;
import org.infobip.mobile.messaging.geo.report.GeoReporter;
import org.infobip.mobile.messaging.geo.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.internal.util.collections.Sets.newSet;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

public class GeoReporterTest extends MobileMessagingTestCase {

    private MessageStore messageStore;
    private GeoReporter geoReporter;
    private MobileApiGeo mobileApiGeo;

    private ArgumentCaptor<List<GeoReport>> geoReportCaptor;
    private ArgumentCaptor<EventReportBody> reportBodyCaptor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Enable message store for notification messages
        enableMessageStoreForReceivedMessages();

        messageStore = MobileMessaging.getInstance(context).getMessageStore();
        mobileApiGeo = mock(MobileApiGeo.class);
        geoReporter = new GeoReporter(context, mobileMessagingCore, geoBroadcaster, mobileMessagingCore.getStats(), mobileApiGeo);
        geoReportCaptor = new ArgumentCaptor<>();
        reportBodyCaptor = ArgumentCaptor.forClass(EventReportBody.class);

        given(mobileApiGeo.report(reportBodyCaptor.capture()))
                .willReturn(new EventReportResponse());
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

        // When
        geoReporter.synchronize();

        // Then
        // Examine what is reported back via broadcast intent
        Mockito.verify(geoBroadcaster, Mockito.after(5000).atLeastOnce()).geoReported(geoReportCaptor.capture());

        List<GeoReport> broadcastedReports = geoReportCaptor.getValue();
        assertEquals(3, broadcastedReports.size());
        for (GeoReport r : broadcastedReports) {
            switch (r.getMessageId()) {
                case "messageId1":
                    assertEquals(GeoEventType.entry, r.getEvent());
                    assertEquals("campaignId1", r.getCampaignId());
                    assertEquals("signalingMessageId1", r.getSignalingMessageId());
                    assertEquals(r.getMessageId(), "messageId1");
                    assertEquals(r.getArea().getId(), "areaId1");
                    break;

                case "messageId2":
                    assertEquals(GeoEventType.entry, r.getEvent());
                    assertEquals("campaignId1", r.getCampaignId());
                    assertEquals("signalingMessageId1", r.getSignalingMessageId());
                    assertEquals("messageId2", r.getMessageId());
                    assertEquals("areaId2", r.getArea().getId());
                    break;

                case "messageId3":
                    assertEquals(GeoEventType.entry, r.getEvent());
                    assertEquals("campaignId3", r.getCampaignId());
                    assertEquals("signalingMessageId2", r.getSignalingMessageId());
                    assertEquals("messageId3", r.getMessageId());
                    assertEquals("areaId3", r.getArea().getId());
                    break;

                default:
                    fail("Unexpected message id " + r.getMessageId());
                    break;
            }
        }

        // Examine HTTP request body
        final EventReportBody body = reportBodyCaptor.getValue();
        assertEquals(2, body.getMessages().size());
        Set<String> messageIds = new HashSet<String>() {{
            List<MessagePayload> ps = new ArrayList<>(body.getMessages());
            add(ps.get(0).getMessageId());
            add(ps.get(1).getMessageId());
        }};
        assertTrue(messageIds.contains("signalingMessageId1"));
        assertTrue(messageIds.contains("signalingMessageId2"));

        assertEquals(3, body.getReports().size());
        EventReport[] reports = body.getReports().toArray(new EventReport[0]);
        assertNotSame(reports[0].getTimestampDelta(), reports[1].getTimestampDelta());
        assertNotSame(reports[0].getTimestampDelta(), reports[2].getTimestampDelta());
        assertNotSame(reports[1].getTimestampDelta(), reports[2].getTimestampDelta());

        assertEquals(3, reports.length);
        for (EventReport report : reports) {
            switch (report.getSdkMessageId()) {
                case "messageId1":
                    assertEquals(EventType.entry, report.getEvent());
                    assertEquals("areaId1", report.getGeoAreaId());
                    assertEquals("signalingMessageId1", report.getMessageId());
                    assertEquals("campaignId1", report.getCampaignId());
                    assertNotNull(report.getTimestampDelta());
                    break;

                case "messageId2":
                    assertEquals(EventType.entry, report.getEvent());
                    assertEquals("areaId2", report.getGeoAreaId());
                    assertEquals("signalingMessageId1", report.getMessageId());
                    assertEquals("campaignId1", report.getCampaignId());
                    assertNotNull(report.getTimestampDelta());
                    break;

                case "messageId3":
                    assertEquals(EventType.entry, report.getEvent());
                    assertEquals("areaId3", report.getGeoAreaId());
                    assertEquals("signalingMessageId2", report.getMessageId());
                    assertEquals("campaignId3", report.getCampaignId());
                    assertNotNull(report.getTimestampDelta());
                    break;

                default:
                    fail("Unexpected message id " + report.getMessageId());
                    break;
            }
        }
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
        given(mobileApiGeo.report(any(EventReportBody.class))).willReturn(new EventReportResponse() {{
            setFinishedCampaignIds(newSet("campaignId1"));
            setSuspendedCampaignIds(newSet("campaignId2"));
        }});

        // When
        geoReporter.synchronize();

        // Then
        // Examine what is reported back via broadcast intent
        Mockito.verify(geoBroadcaster, Mockito.after(1000).atLeastOnce()).geoReported(geoReportCaptor.capture());

        List<GeoReport> broadcastedReports = geoReportCaptor.getValue();
        assertEquals(broadcastedReports.size(), 1);

        GeoReport geoReport = broadcastedReports.get(0);
        assertEquals(GeoEventType.entry, geoReport.getEvent());
        assertEquals("campaignId3", geoReport.getCampaignId());
        assertEquals("messageId3", geoReport.getMessageId());
        assertEquals("areaId3", geoReport.getArea().getId());

        final Set<String> finishedCampaignIds = PreferenceHelper.findStringSet(context, MobileMessagingGeoProperty.FINISHED_CAMPAIGN_IDS.getKey(), new ArraySet<String>(0));
        final Set<String> suspendedCampaignIds = PreferenceHelper.findStringSet(context, MobileMessagingGeoProperty.SUSPENDED_CAMPAIGN_IDS.getKey(), new ArraySet<String>(0));

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
        given(mobileApiGeo.report(any(EventReportBody.class))).willReturn(new EventReportResponse() {{
            setMessageIds(new HashMap<String, String>() {{
                put("messageId1", "ipCoreMessageId1");
                put("messageId2", "ipCoreMessageId2");
                put("messageId3", "ipCoreMessageId3");
            }});
        }});


        // When
        geoReporter.synchronize();

        // Then
        // Wait for reporting to complete
        Mockito.verify(geoBroadcaster, Mockito.after(1000).atLeastOnce()).geoReported(geoReportCaptor.capture());

        // Examine message store
        List<Message> messageList = messageStore.findAll(context);
        assertEquals(3, messageList.size());

        Map<String, Message> messageMap = new HashMap<>();
        for (Message m : messageList) messageMap.put(m.getMessageId(), m);

        assertTrue(messageMap.containsKey("ipCoreMessageId1"));
        Message m1 = messageMap.get("ipCoreMessageId1");
        Geo geo1 = GeoDataMapper.geoFromInternalData(m1.getInternalData());
        assertNotNull(geo1);
        assertEquals("campaignId1", geo1.getCampaignId());
        assertEquals("areaId1", geo1.getAreasList().get(0).getId());

        Message m2 = messageMap.get("ipCoreMessageId2");
        Geo geo2 = GeoDataMapper.geoFromInternalData(m2.getInternalData());
        assertTrue(messageMap.containsKey("ipCoreMessageId2"));
        assertNotNull(geo2);
        assertEquals("campaignId1", geo2.getCampaignId());
        assertEquals("areaId2", geo2.getAreasList().get(0).getId());

        Message m3 = messageMap.get("ipCoreMessageId3");
        Geo geo3 = GeoDataMapper.geoFromInternalData(m3.getInternalData());
        assertNotNull(geo3);
        assertTrue(messageMap.containsKey("ipCoreMessageId3"));
        assertEquals("campaignId2", geo3.getCampaignId());
        assertEquals("areaId3", geo3.getAreasList().get(0).getId());
    }

    @Test
    public void test_shouldReportSeenForMessageIdsIfNoCorrespondingGeoReport() throws InterruptedException {
        // Given
        createMessage(context, "generatedMessageId1", true);
        createReport(context, "signalingMessageId1", "campaignId1", "generatedMessageId2", true);
        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 10L);
        given(mobileApiGeo.report(any(EventReportBody.class))).willReturn(new EventReportResponse());

        // When
        mobileMessaging.setMessagesSeen("generatedMessageId1");

        // Then
        Mockito.verify(coreBroadcaster, Mockito.after(1000).atLeastOnce()).seenStatusReported(any(String[].class));
    }

    @Test
    public void test_shouldNotReportSeenForMessageIdsGeneratedForGeoReports() throws InterruptedException {

        // Given
        createMessage(context, "signalingMessageId1", "campaignId1", true);
        createMessage(context, "generatedMessageId1", "campaignId1", true);
        GeoReport report = createReport(context, "signalingMessageId1", "campaignId1", "generatedMessageId1", true);
        mobileMessagingCore.addGeneratedMessageIds(report.getMessageId());

        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 10L);
        given(mobileApiGeo.report(any(EventReportBody.class))).willReturn(new EventReportResponse());

        // When
        mobileMessaging.setMessagesSeen("generatedMessageId1");

        // Then
        Mockito.verify(coreBroadcaster, Mockito.after(1000).never()).seenStatusReported(any(String[].class));
    }

    @Test
    public void test_shouldReportSeenAfterGeoSuccessfullyReported() throws InterruptedException {

        // Given
        createMessage(context, "signalingMessageId1", "campaignId1", true);
        createMessage(context, "generatedMessageId1", "campaignId1", true);
        createReport(context, "signalingMessageId1", "campaignId1", "generatedMessageId1", true);
        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 10L);
        given(mobileApiGeo.report(any(EventReportBody.class))).willReturn(new EventReportResponse());

        // When
        mobileMessaging.setMessagesSeen("generatedMessageId1");
        geoReporter.synchronize();

        // Then
        Mockito.verify(coreBroadcaster, Mockito.after(1000).atLeastOnce()).seenStatusReported(any(String[].class));
    }
}