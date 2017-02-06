package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventReportBody;
import org.infobip.mobile.messaging.api.geo.EventReportResponse;
import org.infobip.mobile.messaging.api.geo.EventType;
import org.infobip.mobile.messaging.api.geo.MessagePayload;
import org.infobip.mobile.messaging.api.geo.MobileApiGeo;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.tools.DebugServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

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

        Set<EventReport> events = new HashSet<EventReport>(){{
                add(new EventReport(EventType.entry, "myAreaId1", "campaignId1", "messageId1", "sdkMessageId1", 1L));
                add(new EventReport(EventType.exit, "myAreaId2", "campaignId1", "messageId1", "sdkMessageId2", 2L));
                add(new EventReport(EventType.dwell, "myAreaId3", "campaignId2", "messageId2", "sdkMessageId3", 3L));
        }};

        Set<MessagePayload> messages = new HashSet<MessagePayload>() {{
           add(new MessagePayload("messageId1", null, null, null, null, null, null, null, null));
           add(new MessagePayload("messageId2", null, null, null, null, null, null, null, null));
        }};

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);

        EventReportResponse response = mobileApiGeo.report(new EventReportBody(messages, events, "SomeDeviceInstanceId"));

        //inspect http context
        assertThat(debugServer.getUri()).isEqualTo("/mobile/3/geo/event");
        assertThat(debugServer.getRequestCount()).isEqualTo(1);
        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.POST);
        assertThat(debugServer.getQueryParametersCount()).isEqualTo(0);

        assertThat(response.getFinishedCampaignIds().size()).isEqualTo(3);
        assertThat(response.getFinishedCampaignIds()).contains("id1");
        assertThat(response.getFinishedCampaignIds()).contains("id2");
        assertThat(response.getFinishedCampaignIds()).contains("id3");
        assertThat(response.getSuspendedCampaignIds().size()).isEqualTo(3);
        assertThat(response.getSuspendedCampaignIds()).contains("id4");
        assertThat(response.getSuspendedCampaignIds()).contains("id5");
        assertThat(response.getSuspendedCampaignIds()).contains("id6");


        JSONAssert.assertEquals(debugServer.getBody(),
                "{" +
                    "\"platformType\":\"GCM\"," +
                    "\"deviceApplicationInstanceId\":\"SomeDeviceInstanceId\"," +
                    "\"messages\": [" +
                        "{\n" +
                        "      \"messageId\": \"messageId1\",\n" +
                        "      \"title\": null,\n" +
                        "      \"body\": null,\n" +
                        "      \"sound\": null,\n" +
                        "      \"vibrate\": null,\n" +
                        "      \"category\": null,\n" +
                        "      \"silent\": null,\n" +
                        "      \"customPayload\": null,\n" +
                        "      \"internalData\": null\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"messageId\": \"messageId2\",\n" +
                        "      \"title\": null,\n" +
                        "      \"body\": null,\n" +
                        "      \"sound\": null,\n" +
                        "      \"vibrate\": null,\n" +
                        "      \"category\": null,\n" +
                        "      \"silent\": null,\n" +
                        "      \"customPayload\": null,\n" +
                        "      \"internalData\": null\n" +
                        "    }" +
                    "]," +
                    "\"reports\": [" +
                        "{" +
                            "\"event\":\"entry\"," +
                            "\"geoAreaId\":\"myAreaId1\"," +
                            "\"campaignId\":\"campaignId1\"," +
                            "\"messageId\":\"messageId1\"," +
                            "\"sdkMessageId\":\"sdkMessageId1\"," +
                            "\"timestampDelta\":1" +
                        "}," +
                        "{" +
                            "\"event\":\"exit\"," +
                            "\"geoAreaId\":\"myAreaId2\"," +
                            "\"campaignId\":\"campaignId1\"," +
                            "\"messageId\":\"messageId1\"," +
                            "\"sdkMessageId\":\"sdkMessageId2\"," +
                            "\"timestampDelta\":2" +
                        "}," +
                        "{" +
                            "\"event\":\"dwell\"," +
                            "\"geoAreaId\":\"myAreaId3\"," +
                            "\"campaignId\":\"campaignId2\"," +
                            "\"messageId\":\"messageId2\"," +
                            "\"sdkMessageId\":\"sdkMessageId3\"," +
                            "\"timestampDelta\":3" +
                        "}" +
                    "]" +
                 "}"
                , JSONCompareMode.LENIENT);
    }
}
