package org.infobip.mobile.messaging.geo;

import android.annotation.SuppressLint;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.InfobipAndroidTestCase;
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

public class GeoTransitionsTest extends InfobipAndroidTestCase {

    private GeoAreasHandler handler;
    private GeoReporter geoReporter;
    private ArgumentCaptor<Message> messageCaptor;
    private ArgumentCaptor<Geo> geoCaptor;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        handler = new GeoAreasHandler(context, broadcaster);
        messageCaptor = ArgumentCaptor.forClass(Message.class);
        geoCaptor = ArgumentCaptor.forClass(Geo.class);

        geoReporter = new GeoReporter(context, broadcaster, MobileMessagingCore.getInstance(context).getStats());

        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());
        MobileMessaging.getInstance(context).getMessageStore().deleteAll(context);
        MobileMessagingCore.getInstance(context).removeUnreportedGeoEvents();
        MobileMessagingLogger.enforce();
    }

    public void test_shouldNotify_MESSAGE_RECEIVED_forTransition() throws Exception {

        // Given
        Area area = createArea("areaId1", "areaTitle", 1.0, 2.0, 3);
        createMessage(context, "signalingMessageId", "campaignId1", true, area);
        GeoTransition geoTransition = GeoHelper.createTransition("areaId1");

        // When
        handler.handleTransition(geoTransition);

        // Then
        Mockito.verify(broadcaster, Mockito.times(1)).messageReceived(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        assertNotSame("signalingMessageId", message.getMessageId());
        assertEquals("campaignId1", message.getGeo().getCampaignId());
        assertEquals(1, message.getGeo().getAreasList().size());
        assertJEquals(area, message.getGeo().getAreasList().get(0));
    }

    public void test_shouldNotify_GEOFENCE_ENTERED_forTransition() throws Exception {

        // Given
        Area area = createArea("areaId1", "areaTitle", 1.1, 2.2, 3);
        Geo geo = createGeo(1.0, 2.0, "campaignId", area);
        createMessage(context, "signalingMessageId", true, geo);
        GeoTransition geoTransition = GeoHelper.createTransition(1.0, 2.0, "areaId1");

        // When
        handler.handleTransition(geoTransition);

        // Then
        Mockito.verify(broadcaster, Mockito.atLeastOnce()).geoEvent(Mockito.eq(GeoEventType.entry), messageCaptor.capture(), geoCaptor.capture());
        assertJEquals(geo, geoCaptor.getValue());
        Message message = messageCaptor.getValue();
        assertJEquals(geo, message.getGeo());
        assertNotSame("signalingMessageId", message.getMessageId());
    }

    public void test_shouldSaveGeneratedMessageToStoreIfStoreEnabled() throws Exception {

        // Given
        Area area = createArea("areaId1", "areaTitle", 1.1, 2.2, 3);
        Geo geo = createGeo(1.0, 2.0, "campaignId", area);
        createMessage(context, "signalingMessageId", true, geo);
        GeoTransition geoTransition = GeoHelper.createTransition(1.0, 2.0, "areaId1");
        PreferenceHelper.saveString(context, MobileMessagingProperty.MESSAGE_STORE_CLASS, SQLiteMessageStore.class.getName());
        MobileMessaging.getInstance(context).getMessageStore().deleteAll(context);

        // When
        handler.handleTransition(geoTransition);

        // Then
        List<Message> messages = MobileMessaging.getInstance(context).getMessageStore().findAll(context);
        assertEquals(1, messages.size());
        assertJEquals(geo, messages.get(0).getGeo());
        assertNotSame("signalingMessageId", messages.get(0).getMessageId());
    }

    public void test_shouldProvideEventsOnlyForTransitionNotForReporting() throws Exception {

        // Given
        Area area = createArea("areaId1", "areaTitle", 1.1, 2.2, 3);
        Geo geo = createGeo(1.0, 2.0, "campaignId", area);
        createMessage(context, "signalingMessageId", true, geo);
        GeoTransition geoTransition = GeoHelper.createTransition(1.0, 2.0, "areaId1");

        // When
        handler.handleTransition(geoTransition);
        geoReporter.synchronize();

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).atMost(1)).messageReceived(messageCaptor.capture());
        Message message = messageCaptor.getValue();
        assertJEquals(geo, message.getGeo());
        assertNotSame("signalingMessageId", message.getMessageId());

        Mockito.verify(broadcaster, Mockito.atMost(1)).geoEvent(Mockito.eq(GeoEventType.entry), messageCaptor.capture(), geoCaptor.capture());
        assertJEquals(geo, geoCaptor.getValue());
        message = messageCaptor.getValue();
        assertJEquals(geo, message.getGeo());
        assertNotSame("signalingMessageId", message.getMessageId());
    }

    public void test_geoReportsShouldGenerateMessagesOnlyForActiveCampaigns() throws InterruptedException {

        // Given
        Area area = createArea("areaId1");
        createMessage(context,"signalingMessageId1", "campaignId1", true, area);
        createMessage(context,"signalingMessageId2", "campaignId2", true, area);
        GeoTransition transition = GeoHelper.createTransition("areaId1");
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{" +
                "  'suspendedCampaignIds':['campaignId1']" +
                "}");

        // When
        handler.handleTransition(transition);

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).times(1)).messageReceived(Mockito.any(Message.class));
        List<Message> messages = MobileMessaging.getInstance(context).getMessageStore().findAll(context);
        assertEquals(1, messages.size());
        assertEquals("campaignId2", messages.get(0).getGeo().getCampaignId());
    }

    public void test_shouldKeepGeneratedMessagesOnFailedReport() throws Exception {

        // Given
        Area area = createArea("areaId1");
        createMessage(context,"signalingMessageId1", "campaignId1", true, area);
        createMessage(context,"signalingMessageId2", "campaignId2", true, area);
        GeoTransition transition = GeoHelper.createTransition(area.getId());
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, null);

        // When
        handler.handleTransition(transition);

        // Then
        Mockito.verify(broadcaster, Mockito.after(1000).times(2)).messageReceived(Mockito.any(Message.class));
        List<Message> messages = MobileMessaging.getInstance(context).getMessageStore().findAll(context);
        assertEquals(2, messages.size());
        List<String> campaignIds = Arrays.asList(messages.get(0).getGeo().getCampaignId(), messages.get(1).getGeo().getCampaignId());
        assertTrue(campaignIds.contains("campaignId1"));
        assertTrue(campaignIds.contains("campaignId2"));
    }
}
