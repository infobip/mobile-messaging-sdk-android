package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.instance.Instance;
import org.infobip.mobile.messaging.api.instance.MobileApiInstance;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.tools.DebugServer;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import fi.iki.elonen.NanoHTTPD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author sslavin
 * @since 20/06/2018.
 */
public class MobileApiInstanceTest {
    private DebugServer debugServer;
    private MobileApiInstance mobileApiInstance;

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

        mobileApiInstance = generator.create(MobileApiInstance.class);
    }

    @Test
    public void getInstance_success() throws Exception {

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, "{ 'primary': true }");

        Instance instance = mobileApiInstance.get();

        assertThat(debugServer.getUri()).isEqualTo("/mobile/1/instance/");
        assertThat(debugServer.getRequestCount()).isEqualTo(1);
        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.GET);
        assertEquals(true, instance.getPrimary());
    }

    @Test
    public void updateInstance_success() throws Exception {
        Instance instance = new Instance(true);
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileApiInstance.update(instance);

        assertThat(debugServer.getUri()).isEqualTo("/mobile/1/instance/");
        assertThat(debugServer.getRequestCount()).isEqualTo(1);
        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.PUT);
        assertEquals("{\"primary\":true}", debugServer.getBody());
        assertEquals(true, instance.getPrimary());
    }
}
