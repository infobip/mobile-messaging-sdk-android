package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.msisdn.MobileApiRegisterMsisdn;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.tools.DebugServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import fi.iki.elonen.NanoHTTPD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author sslavin
 * @since 06/05/16.
 */
public class MobileApiRegisterMsisdnTest {

    private DebugServer debugServer;
    private MobileApiRegisterMsisdn mobileApiRegisterMsisdn;

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

        mobileApiRegisterMsisdn = generator.create(MobileApiRegisterMsisdn.class);
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
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileApiRegisterMsisdn.registerMsisdn("12347", 1234567890);

        //inspect http context
        assertThat(debugServer.getUri()).isEqualTo("/mobile/1/msisdn");
        assertThat(debugServer.getRequestCount()).isEqualTo(1);
        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.POST);
        assertThat(debugServer.getQueryParametersCount()).isEqualTo(2);
        assertThat(debugServer.getBody()).isNull();

        //inspect parameters
        assertEquals("12347", debugServer.getQueryParameter("deviceApplicationInstanceId"));
        assertEquals("1234567890", debugServer.getQueryParameter("msisdn"));
    }
}
