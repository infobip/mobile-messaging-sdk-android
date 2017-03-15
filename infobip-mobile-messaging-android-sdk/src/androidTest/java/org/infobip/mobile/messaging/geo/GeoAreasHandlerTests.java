package org.infobip.mobile.messaging.geo;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.geo.EventReportResponse;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.mobile.geo.GeoReportingResult;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sslavin
 * @since 14/03/2017.
 */

public class GeoAreasHandlerTests extends MobileMessagingTestCase {

    private GeoAreasHandler geoAreasHandler;
    private GeoNotificationHelper geoNotificationHelper;
    private GeoReporter geoReporter;
    private MessageStore messageStore;
    private ArgumentCaptor<GeoReport[]> geoReportCaptor;
    private ArgumentCaptor<Map<Message, GeoEventType>> geoNotificationCaptor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        enableMessageStoreForReceivedMessages();

        geoNotificationHelper = Mockito.mock(GeoNotificationHelper.class);
        geoReporter = Mockito.mock(GeoReporter.class);
        messageStore = Mockito.mock(MessageStore.class);
        geoAreasHandler = new GeoAreasHandler(contextMock, geoNotificationHelper, geoReporter, messageStore);
        geoReportCaptor = ArgumentCaptor.forClass(GeoReport[].class);
        geoNotificationCaptor = new ArgumentCaptor<>();
    }

    public void test_should_report_transition() {
        // Given
        Message m = createMessage(context, "SomeSignalingMessageId", "SomeCampaignId", false, createArea("SomeAreaId"));
        Mockito.when(messageStore.findAll(Mockito.any(Context.class))).thenReturn(Collections.singletonList(m));
        Mockito.when(geoReporter.reportSync(Mockito.any(Context.class), Mockito.any(GeoReport[].class))).thenReturn(new GeoReportingResult(new Exception()));
        GeoTransition transition = GeoHelper.createTransition(123.0, 456.0, "SomeAreaId");
        time.set(789);

        // When
        geoAreasHandler.handleTransition(transition);

        // Then
        Mockito.verify(geoReporter, Mockito.times(1)).reportSync(Mockito.any(Context.class), geoReportCaptor.capture());
        GeoReport report = geoReportCaptor.getValue()[0];
        assertEquals("SomeAreaId", report.getArea().getId());
        assertEquals("SomeCampaignId", report.getCampaignId());
        assertEquals("SomeSignalingMessageId", report.getSignalingMessageId());
        assertNotSame("SomeSignalingMessageId", report.getMessageId());
        assertTrue(StringUtils.isNotBlank(report.getMessageId()));
        assertEquals(789, report.getTimestampOccurred().longValue());
        assertEquals(123.0, report.getTriggeringLocation().getLat());
        assertEquals(456.0, report.getTriggeringLocation().getLng());
    }

    public void test_should_notify_messages_with_generated_ids_if_report_unsuccessful() {
        // Given
        Message m = createMessage(context, "SomeSignalingMessageId", "SomeCampaignId", true, createArea("SomeAreaId"));
        Mockito.when(messageStore.findAll(Mockito.any(Context.class))).thenReturn(Collections.singletonList(m));
        Mockito.when(geoReporter.reportSync(Mockito.any(Context.class), Mockito.any(GeoReport[].class))).thenReturn(new GeoReportingResult(new Exception()));
        GeoTransition transition = GeoHelper.createTransition(123.0, 456.0, "SomeAreaId");
        time.set(789);

        // When
        geoAreasHandler.handleTransition(transition);

        // Then
        Mockito.verify(geoNotificationHelper, Mockito.times(1)).notifyAboutGeoTransitions(geoNotificationCaptor.capture());
        Map.Entry<Message, GeoEventType> notification = geoNotificationCaptor.getValue().entrySet().iterator().next();
        assertEquals(GeoEventType.entry, notification.getValue());

        Message message = notification.getKey();
        assertNotSame("SomeSignalingMessageId", message.getMessageId());
        assertTrue(StringUtils.isNotBlank(message.getMessageId()));
        assertEquals("SomeCampaignId", message.getGeo().getCampaignId());
        assertEquals(123.0, message.getGeo().getTriggeringLatitude());
        assertEquals(456.0, message.getGeo().getTriggeringLongitude());
        assertEquals("SomeAreaId", message.getGeo().getAreasList().get(0).getId());
    }

    public void test_should_notify_messages_with_server_ids_if_report_successful() {
        // Given
        Message m = createMessage(context, "SomeSignalingMessageId", "SomeCampaignId", true, createArea("SomeAreaId"));
        Mockito.when(messageStore.findAll(Mockito.any(Context.class))).thenReturn(Collections.singletonList(m));
        Mockito.when(geoReporter.reportSync(Mockito.any(Context.class), Mockito.any(GeoReport[].class))).thenAnswer(new Answer<GeoReportingResult>() {
            @Override
            public GeoReportingResult answer(InvocationOnMock invocation) throws Throwable {
                final GeoReport reports[] = (GeoReport[]) invocation.getArguments()[1];
                EventReportResponse eventReportResponse = new EventReportResponse();
                eventReportResponse.setMessageIds(new HashMap<String, String>(){{
                    put(reports[0].getMessageId(), "SomeServerMessageId");
                }});
                return new GeoReportingResult(eventReportResponse);
            }
        });
        GeoTransition transition = GeoHelper.createTransition(123.0, 456.0, "SomeAreaId");
        time.set(789);

        // When
        geoAreasHandler.handleTransition(transition);

        // Then
        Mockito.verify(geoNotificationHelper, Mockito.times(1)).notifyAboutGeoTransitions(geoNotificationCaptor.capture());
        Map.Entry<Message, GeoEventType> notification = geoNotificationCaptor.getValue().entrySet().iterator().next();
        assertEquals(GeoEventType.entry, notification.getValue());

        Message message = notification.getKey();
        assertEquals("SomeServerMessageId", message.getMessageId());
        assertEquals("SomeCampaignId", message.getGeo().getCampaignId());
        assertEquals(123.0, message.getGeo().getTriggeringLatitude());
        assertEquals(456.0, message.getGeo().getTriggeringLongitude());
        assertEquals("SomeAreaId", message.getGeo().getAreasList().get(0).getId());
    }

    public void test_should_save_messages_with_generated_ids_if_report_unsuccessful() {
        // Given
        Message m = createMessage(context, "SomeSignalingMessageId", "SomeCampaignId", true, createArea("SomeAreaId"));
        Mockito.when(messageStore.findAll(Mockito.any(Context.class))).thenReturn(Collections.singletonList(m));
        Mockito.when(geoReporter.reportSync(Mockito.any(Context.class), Mockito.any(GeoReport[].class))).thenReturn(new GeoReportingResult(new Exception()));
        GeoTransition transition = GeoHelper.createTransition(123.0, 456.0, "SomeAreaId");
        time.set(789);

        // When
        geoAreasHandler.handleTransition(transition);

        // Then
        Message message = mobileMessaging.getMessageStore().findAll(context).get(0);
        assertNotSame("SomeSignalingMessageId", message.getMessageId());
        assertTrue(StringUtils.isNotBlank(message.getMessageId()));
        assertEquals("SomeCampaignId", message.getGeo().getCampaignId());
        assertEquals(123.0, message.getGeo().getTriggeringLatitude());
        assertEquals(456.0, message.getGeo().getTriggeringLongitude());
        assertEquals("SomeAreaId", message.getGeo().getAreasList().get(0).getId());
    }

    public void test_should_save_messages_with_server_ids_if_report_successful() {
        // Given
        Message m = createMessage(context, "SomeSignalingMessageId", "SomeCampaignId", true, createArea("SomeAreaId"));
        Mockito.when(messageStore.findAll(Mockito.any(Context.class))).thenReturn(Collections.singletonList(m));
        Mockito.when(geoReporter.reportSync(Mockito.any(Context.class), Mockito.any(GeoReport[].class))).thenAnswer(new Answer<GeoReportingResult>() {
            @Override
            public GeoReportingResult answer(InvocationOnMock invocation) throws Throwable {
                final GeoReport reports[] = (GeoReport[]) invocation.getArguments()[1];
                EventReportResponse eventReportResponse = new EventReportResponse();
                eventReportResponse.setMessageIds(new HashMap<String, String>(){{
                    put(reports[0].getMessageId(), "SomeServerMessageId");
                }});
                return new GeoReportingResult(eventReportResponse);
            }
        });
        GeoTransition transition = GeoHelper.createTransition(123.0, 456.0, "SomeAreaId");
        time.set(789);

        // When
        geoAreasHandler.handleTransition(transition);

        // Then
        Message message = mobileMessaging.getMessageStore().findAll(context).get(0);
        assertNotSame("SomeSignalingMessageId", message.getMessageId());
        assertTrue(StringUtils.isNotBlank(message.getMessageId()));
        assertEquals("SomeCampaignId", message.getGeo().getCampaignId());
        assertEquals(123.0, message.getGeo().getTriggeringLatitude());
        assertEquals(456.0, message.getGeo().getTriggeringLongitude());
        assertEquals("SomeAreaId", message.getGeo().getAreasList().get(0).getId());
    }

    public void test_should_notify_messages_only_for_active_campaigns() throws InterruptedException {

        // Given
        Area area = createArea("areaId1");
        Mockito.when(messageStore.findAll(Mockito.any(Context.class))).thenReturn(Arrays.asList(
                createMessage(context,"signalingMessageId1", "campaignId1", true, area),
                createMessage(context,"signalingMessageId2", "campaignId2", true, area),
                createMessage(context,"signalingMessageId3", "campaignId3", true, area)));
        EventReportResponse response = new EventReportResponse();
        response.setSuspendedCampaignIds(Sets.newSet("campaignId1"));
        response.setFinishedCampaignIds(Sets.newSet("campaignId2"));
        Mockito.when(geoReporter.reportSync(Mockito.any(Context.class), Mockito.any(GeoReport[].class))).thenReturn(new GeoReportingResult(response));
        GeoTransition transition = GeoHelper.createTransition("areaId1");

        // When
        geoAreasHandler.handleTransition(transition);

        // Then
        Mockito.verify(geoNotificationHelper, Mockito.times(1)).notifyAboutGeoTransitions(geoNotificationCaptor.capture());
        assertEquals(1, geoNotificationCaptor.getValue().size());
        Map.Entry<Message, GeoEventType> notification = geoNotificationCaptor.getValue().entrySet().iterator().next();
        assertEquals(GeoEventType.entry, notification.getValue());

        Message message = notification.getKey();
        assertNotSame("signalingMessageId3", message.getMessageId());
        assertTrue(StringUtils.isNotBlank(message.getMessageId()));
        assertEquals("campaignId3", message.getGeo().getCampaignId());
        assertEquals("areaId1", message.getGeo().getAreasList().get(0).getId());
    }

    public void test_should_generate_messages_only_for_active_campaigns() throws InterruptedException {

        // Given
        Area area = createArea("areaId1");
        Mockito.when(messageStore.findAll(Mockito.any(Context.class))).thenReturn(Arrays.asList(
                createMessage(context,"signalingMessageId1", "campaignId1", false, area),
                createMessage(context,"signalingMessageId2", "campaignId2", false, area),
                createMessage(context,"signalingMessageId3", "campaignId3", false, area)));
        EventReportResponse response = new EventReportResponse();
        response.setSuspendedCampaignIds(Sets.newSet("campaignId1"));
        response.setFinishedCampaignIds(Sets.newSet("campaignId2"));
        Mockito.when(geoReporter.reportSync(Mockito.any(Context.class), Mockito.any(GeoReport[].class))).thenReturn(new GeoReportingResult(response));
        GeoTransition transition = GeoHelper.createTransition("areaId1");

        // When
        geoAreasHandler.handleTransition(transition);

        // Then
        List<Message> messages = MobileMessaging.getInstance(context).getMessageStore().findAll(context);
        assertEquals(1, messages.size());
        Message message = messages.get(0);
        assertNotSame("signalingMessageId3", message.getMessageId());
        assertTrue(StringUtils.isNotBlank(message.getMessageId()));
        assertEquals("campaignId3", message.getGeo().getCampaignId());
        assertEquals("areaId1", message.getGeo().getAreasList().get(0).getId());
    }
}
