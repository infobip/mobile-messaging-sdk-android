package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.geo.CampaignStatusEventResponse;
import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventReports;
import org.infobip.mobile.messaging.api.geo.EventType;
import org.infobip.mobile.messaging.api.geo.MobileApiGeo;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.tools.DebugServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Properties;

import fi.iki.elonen.NanoHTTPD;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sslavin
 * @since 19/10/2016.
 */

public class MobileApiGeoTest {
    private DebugServer debugServer;
    private MobileApiGeo mobileApiGeo;

    @Before
    public void setUp() throws Exception {
        debugServer = new DebugServer();
        debugServer.start();

        Properties properties = new Properties();
        properties.put("api.key", "my_API_key");
        Generator generator = new Generator.Builder()
                .withBaseUrl("http://127.0.0.1:" + debugServer.getListeningPort() + "/")
                .withProperties(properties)
                .build();

        mobileApiGeo = generator.create(MobileApiGeo.class);
    }

    @After
    public void tearDown() throws Exception {
        if (null != debugServer) {
            try {
                debugServer.stop();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    @Test
    public void create_eventReport_success() throws Exception {
        String jsonResponse = "{\n" +
                "  \"finishedCampaignIds\":[\"id1\", \"id2\", \"id3\"],\n" +
                "  \"suspendedCampaignIds\":[\"id4\", \"id5\", \"id6\"]\n" +
                "}";

        EventReport eventReport[] = {
                new EventReport(EventType.entry, "myAreaId1", "campaignId1", "messageId1", 1L),
                new EventReport(EventType.exit, "myAreaId2", "campaignId2", "messageId2", 2L),
                new EventReport(EventType.dwell, "myAreaId3", "campaignId3", "messageId3", 3L)
        };

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);

        CampaignStatusEventResponse response = mobileApiGeo.report(new EventReports(eventReport));

        //inspect http context
        assertThat(debugServer.getUri()).isEqualTo("/mobile/3/geo/event");
        assertThat(debugServer.getRequestCount()).isEqualTo(1);
        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.POST);
        assertThat(debugServer.getQueryParametersCount()).isEqualTo(0);

        assertThat(response.getFinishedCampaignIds().length).isEqualTo(3);
        assertThat(response.getFinishedCampaignIds()[0]).isEqualTo("id1");
        assertThat(response.getFinishedCampaignIds()[1]).isEqualTo("id2");
        assertThat(response.getFinishedCampaignIds()[2]).isEqualTo("id3");
        assertThat(response.getSuspendedCampaignIds().length).isEqualTo(3);
        assertThat(response.getSuspendedCampaignIds()[0]).isEqualTo("id4");
        assertThat(response.getSuspendedCampaignIds()[1]).isEqualTo("id5");
        assertThat(response.getSuspendedCampaignIds()[2]).isEqualTo("id6");


        JSONAssert.assertEquals(debugServer.getBody(),
                "{" +
                    "\"reports\": [" +
                        "{" +
                            "\"event\":\"entry\"," +
                            "\"geoAreaId\":\"myAreaId1\"," +
                            "\"campaignId\":\"campaignId1\"," +
                            "\"messageId\":\"messageId1\"," +
                            "\"timestampDelta\":1" +
                        "}," +
                        "{" +
                            "\"event\":\"exit\"," +
                            "\"geoAreaId\":\"myAreaId2\"," +
                            "\"campaignId\":\"campaignId2\"," +
                            "\"messageId\":\"messageId2\"," +
                            "\"timestampDelta\":2" +
                        "}," +
                        "{" +
                            "\"event\":\"dwell\"," +
                            "\"geoAreaId\":\"myAreaId3\"," +
                            "\"campaignId\":\"campaignId3\"," +
                            "\"messageId\":\"messageId3\"," +
                            "\"timestampDelta\":3" +
                        "}" +
                    "]" +
                 "}"
                , true);
    }
}
