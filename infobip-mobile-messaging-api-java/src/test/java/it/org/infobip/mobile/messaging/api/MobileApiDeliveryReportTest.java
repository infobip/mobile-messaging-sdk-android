package it.org.infobip.mobile.messaging.api;

import fi.iki.elonen.NanoHTTPD;
import org.infobip.mobile.messaging.api.registration.DeliveryReportResponse;
import org.infobip.mobile.messaging.api.registration.MobileApiDeliveryReport;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient;
import org.infobip.mobile.messaging.api.tools.DebugServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mstipanov
 * @since 17.03.2016.
 */
public class MobileApiDeliveryReportTest {
    private DebugServer debugServer;
    private MobileApiDeliveryReport mobileApiDeliveryReport;

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

        mobileApiDeliveryReport = generator.create(MobileApiDeliveryReport.class);
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
    public void create_success() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, DefaultApiClient.JSON_SERIALIZER.serialize(new DeliveryReportResponse("11")));

        DeliveryReportResponse response = mobileApiDeliveryReport.report("1", "2", "3");

        //inspect http context
        assertThat(debugServer.getUri()).isEqualTo("/mobile/1/deliveryreports");
        assertThat(debugServer.getRequestCount()).isEqualTo(1);
        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.POST);
        assertThat(debugServer.getQueryParametersCount()).isEqualTo(1); //TODO It should ne 3! Use a better server that knows how to accept arrays!
        assertThat(debugServer.getBody()).isNull();
    }
}