package org.infobip.mobile.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.api.geo.EventReports;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.tools.DebugServer;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.List;

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

        List<GeoReport> reports = new ArrayList<>();
        reports.add(new GeoReport("campaignId1", "messageId1", GeoEventType.entry, new Area("areaId1", "Area1", 1.0, 1.0, 3), 1001L));
        reports.add(new GeoReport("campaignId2", "messageId2", GeoEventType.exit, new Area("areaId2", "Area2", 2.0, 2.0, 4), 1002L));
        reports.add(new GeoReport("campaignId3", "messageId3", GeoEventType.dwell, new Area("areaId3", "Area3", 3.0, 3.0, 5), 1003L));

        MobileMessagingCore.getInstance(context).addUnreportedGeoEvents(reports);
        geoReporter.report(context, MobileMessagingCore.getInstance(context).getStats());

        // Examine what is reported back via broadcast intent

        Mockito.verify(receiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());

        List<GeoReport> broadcastedReports = GeoReport.createFrom(captor.getValue().getExtras());
        assertEquals(broadcastedReports.size(), 3);

        GeoReport geoReport1 = broadcastedReports.get(0);
        assertEquals(geoReport1.getEvent(), GeoEventType.entry);
        assertEquals(geoReport1.getCampaignId(), "campaignId1");
        assertEquals(geoReport1.getMessageId(), "messageId1");
        assertEquals(geoReport1.getTimestampOccurred(), (Long) 1001L);
        assertEquals(geoReport1.getArea().getId(), "areaId1");
        assertEquals(geoReport1.getArea().getTitle(), "Area1");
        assertEquals(geoReport1.getArea().getLatitude(), 1.0);
        assertEquals(geoReport1.getArea().getLongitude(), 1.0);
        assertEquals(geoReport1.getArea().getRadius(), (Integer) 3);

        GeoReport geoReport2 = broadcastedReports.get(1);
        assertEquals(geoReport2.getEvent(), GeoEventType.exit);
        assertEquals(geoReport2.getCampaignId(), "campaignId2");
        assertEquals(geoReport2.getMessageId(), "messageId2");
        assertEquals(geoReport2.getTimestampOccurred(), (Long) 1002L);
        assertEquals(geoReport2.getArea().getId(), "areaId2");
        assertEquals(geoReport2.getArea().getTitle(), "Area2");
        assertEquals(geoReport2.getArea().getLatitude(), 2.0);
        assertEquals(geoReport2.getArea().getLongitude(), 2.0);
        assertEquals(geoReport2.getArea().getRadius(), (Integer) 4);

        GeoReport geoReport3 = broadcastedReports.get(2);
        assertEquals(geoReport3.getEvent(), GeoEventType.dwell);
        assertEquals(geoReport3.getCampaignId(), "campaignId3");
        assertEquals(geoReport3.getMessageId(), "messageId3");
        assertEquals(geoReport3.getTimestampOccurred(), (Long) 1003L);
        assertEquals(geoReport3.getArea().getId(), "areaId3");
        assertEquals(geoReport3.getArea().getTitle(), "Area3");
        assertEquals(geoReport3.getArea().getLatitude(), 3.0);
        assertEquals(geoReport3.getArea().getLongitude(), 3.0);
        assertEquals(geoReport3.getArea().getRadius(), (Integer) 5);


        // Examine HTTP request body

        String stringBody = debugServer.getBody();
        EventReports body = new JsonSerializer().deserialize(stringBody, EventReports.class);

        assertEquals(body.getReports().length, 3);
        JSONAssert.assertEquals(debugServer.getBody(),
                "{" +
                        "\"reports\": [" +
                        "{" +
                        "\"event\":\"entry\"," +
                        "\"geoAreaId\":\"areaId1\"," +
                        "\"messageId\":\"messageId1\"," +
                        "\"campaignId\":\"campaignId1\"," +
                        "\"timestampDelta\":" + body.getReports()[0].getTimestampDelta() +
                        "}," +
                        "{" +
                        "\"event\":\"exit\"," +
                        "\"geoAreaId\":\"areaId2\"," +
                        "\"messageId\":\"messageId2\"," +
                        "\"campaignId\":\"campaignId2\"," +
                        "\"timestampDelta\":" + body.getReports()[1].getTimestampDelta() +
                        "}," +
                        "{" +
                        "\"event\":\"dwell\"," +
                        "\"geoAreaId\":\"areaId3\"," +
                        "\"messageId\":\"messageId3\"," +
                        "\"campaignId\":\"campaignId3\"," +
                        "\"timestampDelta\":" + body.getReports()[2].getTimestampDelta() +
                        "}" +
                        "]" +
                        "}"
                , true);

        assertNotSame(body.getReports()[0].getTimestampDelta(), body.getReports()[1].getTimestampDelta());
        assertNotSame(body.getReports()[0].getTimestampDelta(), body.getReports()[2].getTimestampDelta());
        assertNotSame(body.getReports()[1].getTimestampDelta(), body.getReports()[2].getTimestampDelta());
    }

    public void test_withNonActiveCampaigns() {
        String jsonResponse = "{\n" +
                "  \"finishedCampaignIds\":[\"campaignId1\"],\n" +
                "  \"suspendedCampaignIds\":[\"campaignId2\"]\n" +
                "}";

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);

        List<GeoReport> reports = new ArrayList<>();
        reports.add(new GeoReport("campaignId1", "messageId1", GeoEventType.entry, new Area("areaId1", "Area1", 1.0, 1.0, 3), 1001L));
        reports.add(new GeoReport("campaignId2", "messageId2", GeoEventType.exit, new Area("areaId2", "Area2", 2.0, 2.0, 4), 1002L));
        reports.add(new GeoReport("campaignId3", "messageId3", GeoEventType.dwell, new Area("areaId3", "Area3", 3.0, 3.0, 5), 1003L));

        MobileMessagingCore.getInstance(context).addUnreportedGeoEvents(reports);
        geoReporter.report(context, MobileMessagingCore.getInstance(context).getStats());


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

        final String[] finishedCampaignIds = PreferenceHelper.findStringArray(context, MobileMessagingProperty.FINISHED_CAMPAIGN_IDS);
        final String[] suspendedCampaignIds = PreferenceHelper.findStringArray(context, MobileMessagingProperty.SUSPENDED_CAMPAIGN_IDS);

        assertEquals(finishedCampaignIds.length, 1);
        assertEquals(finishedCampaignIds[0], "campaignId1");

        assertEquals(suspendedCampaignIds.length, 1);
        assertEquals(suspendedCampaignIds[0], "campaignId2");
    }
}
