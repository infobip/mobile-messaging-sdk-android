package org.infobip.mobile.messaging.geo;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.geo.EventReportResponse;
import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.geo.report.GeoReport;
import org.infobip.mobile.messaging.geo.report.GeoReportHelper;
import org.infobip.mobile.messaging.geo.report.GeoReportingResult;
import org.infobip.mobile.messaging.geo.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author sslavin
 * @since 14/03/2017.
 */

public class GeoReportHelperTest extends MobileMessagingTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void test_should_find_signaling_messages_for_report() throws Exception {

        // Given
        Message m1 = createMessage(context, "signalingMessageId", "campaignId", true, createArea("areaId"));
        Message m2 = createMessage(context, "signalingMessageI2", "campaignId", true, createArea("areaId"));
        Message m3 = createMessage(context, "signalingMessageI3", "campaignId", true, createArea("areaId"));
        GeoReport report = createReport(context, "signalingMessageId", "campaignId", "sdkMessageId", false);

        // When
        Message foundMessage = GeoReportHelper.getSignalingMessageForReport(Arrays.asList(m1, m2, m3), report);

        // Then
        assertJEquals(m1, foundMessage);
    }

    @Test
    public void test_should_find_signaling_messages_and_areas_for_geofencing_request_ids_and_event_type() throws Exception {

        // Given
        Area area = createArea("areaId1");
        Message message = createMessage(context, "messageId1", "campaigId1", true, area);

        // When
        Map<Message, List<Area>> messageAreas = GeoReportHelper.findSignalingMessagesAndAreas(context, geoStore, Sets.newSet("areaId1"), GeoEventType.entry);

        // Then
        // internalData is String, so has to be compared as JSON separately
        assertJEquals(message, messageAreas.keySet().iterator().next(), "internalData");
        JSONAssert.assertEquals(message.getInternalData(), messageAreas.keySet().iterator().next().getInternalData(), true);
        assertJEquals(area, messageAreas.values().iterator().next().get(0));
    }

    @Test
    public void test_should_create_messages_with_generated_ids_for_unsuccessful_report() {

        // Given
        Area area = createArea("areaId");
        GeoReport report = createReport(context, "signalingMessageId", "campaignId", "sdkMessageId", false, area);
        GeoReportingResult geoReportingResult = new GeoReportingResult(new Exception());
        createMessage(context, "signalingMessageId", "campaignId", true, area);

        // When
        Map<Message, GeoEventType> messages = GeoReportHelper.createMessagesToNotify(context, Collections.singletonList(report), geoReportingResult);

        // Then
        Message generated = messages.keySet().iterator().next();
        assertEquals("sdkMessageId", generated.getMessageId());
        assertEquals(GeoEventType.entry, messages.values().iterator().next());
    }

    @Test
    public void test_should_create_messages_with_server_ids_for_successful_report() {

        // Given
        Area area = createArea("areaId");
        GeoReport report = createReport(context, "signalingMessageId", "campaignId", "sdkMessageId", false, area);
        EventReportResponse reportResponse = new EventReportResponse();
        reportResponse.setMessageIds(new HashMap<String, String>() {{
            put("sdkMessageId", "serverMessageId");
        }});
        GeoReportingResult geoReportingResult = new GeoReportingResult(reportResponse);
        createMessage(context, "signalingMessageId", "campaignId", true, area);

        // When
        Map<Message, GeoEventType> messages = GeoReportHelper.createMessagesToNotify(context, Collections.singletonList(report), geoReportingResult);

        // Then
        Message generated = messages.keySet().iterator().next();
        assertEquals("serverMessageId", generated.getMessageId());
        assertEquals(GeoEventType.entry, messages.values().iterator().next());
    }

    @Test
    public void test_should_filter_reports_for_inactive_campaigns() throws Exception {

        // Given
        Area area = createArea("areaId");
        EventReportResponse reportResponse = new EventReportResponse();
        reportResponse.setFinishedCampaignIds(Sets.newSet("campaignId1"));
        reportResponse.setSuspendedCampaignIds(Sets.newSet("campaignId2"));
        createMessage(context, "signalingMessageId1", "campaignId1", true, area);
        createMessage(context, "signalingMessageId2", "campaignId2", true, area);
        createMessage(context, "signalingMessageId3", "campaignId3", true, area);
        GeoReportingResult geoReportingResult = new GeoReportingResult(reportResponse);
        List<GeoReport> reports = Arrays.asList(
                createReport(context, "signalingMessageId1", "campaignId1", "sdkMessageId1", false, area),
                createReport(context, "signalingMessageId2", "campaignId2", "sdkMessageId2", false, area),
                createReport(context, "signalingMessageId3", "campaignId3", "sdkMessageId3", false, area));

        // When
        List<GeoReport> filtered = GeoReportHelper.filterOutNonActiveReports(context, reports, geoReportingResult);

        // Then
        assertJEquals(reports.get(2), filtered.get(0));
    }

    @Test
    public void test_should_update_list_of_inactive_campaigns_based_on_report_result() {

        // Given
        EventReportResponse reportResponse = new EventReportResponse();
        reportResponse.setSuspendedCampaignIds(Sets.newSet("campaignId1"));
        reportResponse.setFinishedCampaignIds(Sets.newSet("campaignId2"));

        // When
        Set<String> ids = GeoReportHelper.getAndUpdateInactiveCampaigns(context, new GeoReportingResult(reportResponse));

        // Then
        assertTrue(GeofencingHelper.getSuspendedCampaignIds(context).contains("campaignId1"));
        assertTrue(ids.contains("campaignId1"));
        assertTrue(GeofencingHelper.getFinishedCampaignIds(context).contains("campaignId2"));
        assertTrue(ids.contains("campaignId2"));
    }

    @Test
    public void test_should_compare_radius_in_geo_areas_list() {
        Area area1 = createArea("areaId1", "", 1.0, 2.0, 700);
        Area area2 = createArea("areaId2", "", 1.0, 2.0, 250);
        Area area3 = createArea("areaId3", "", 1.0, 2.0, 1000);
        List<Area> areasList = Arrays.asList(area1, area2, area3);

        GeoReportHelper.GeoAreaRadiusComparator geoAreaRadiusComparator = new GeoReportHelper.GeoAreaRadiusComparator();
        Collections.sort(areasList, geoAreaRadiusComparator);

        assertEquals(areasList.get(0).getId(), area2.getId());
        assertEquals(areasList.get(0).getRadius(), area2.getRadius());
        assertEquals(areasList.get(1).getId(), area1.getId());
        assertEquals(areasList.get(1).getRadius(), area1.getRadius());
        assertEquals(areasList.get(2).getId(), area3.getId());
        assertEquals(areasList.get(2).getRadius(), area3.getRadius());
    }

    @Test
    public void test_should_filter_overlapping_triggered_areas() {
        //given
        HashMap<Message, List<Area>> messagesAndAreas = new HashMap<>();
        Area area1 = createArea("areaId1", "", 1.0, 2.0, 700);
        Area area2 = createArea("areaId2", "", 1.0, 2.0, 250);
        Area area3 = createArea("areaId3", "", 1.0, 2.0, 1000);
        Message message = createMessage(context, "messageId1", "campaignId1", true, area1, area2, area3);
        List<Area> triggeredAreasList = Arrays.asList(area1, area3);

        messagesAndAreas.put(message, triggeredAreasList);

        //when
        Map<Message, List<Area>> filteredOverlappingAreasForMessages = GeoReportHelper.filterOverlappingAreas(messagesAndAreas);
        List<Area> filteredTriggeredAreasList = new ArrayList<>();
        for (List<Area> areas : filteredOverlappingAreasForMessages.values()) {
            filteredTriggeredAreasList.addAll(areas);
        }

        //then
        assertEquals(1, filteredTriggeredAreasList.size());
        assertEquals(area1.getRadius(), filteredTriggeredAreasList.get(0).getRadius());
        assertEquals(area1, filteredTriggeredAreasList.get(0));
        assertEquals(1, filteredOverlappingAreasForMessages.keySet().size());
        assertEquals(true, filteredOverlappingAreasForMessages.containsKey(message));
    }
}
