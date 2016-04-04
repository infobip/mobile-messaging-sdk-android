package it.org.infobip.mobile.messaging.api;

import fi.iki.elonen.NanoHTTPD;
import org.infobip.mobile.messaging.api.registration.MobileApiRegistration;
import org.infobip.mobile.messaging.api.registration.RegistrationResponse;
import org.infobip.mobile.messaging.api.support.ApiBackendException;
import org.infobip.mobile.messaging.api.support.ApiException;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient;
import org.infobip.mobile.messaging.api.support.http.client.model.ApiResponse;
import org.infobip.mobile.messaging.api.tools.DebugServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author mstipanov
 * @since 17.03.2016.
 */
public class MobileApiRegistrationTest {
    private DebugServer debugServer;
    private MobileApiRegistration mobileApiRegistration;

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

        mobileApiRegistration = generator.create(MobileApiRegistration.class);
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
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, DefaultApiClient.JSON_SERIALIZER.serialize(new RegistrationResponse("11")));

        RegistrationResponse response = mobileApiRegistration.upsert(null, "123");

        //inspect http context
        assertEquals("/mobile/2/registration", debugServer.getUri());
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
        assertEquals(2, debugServer.getQueryParametersCount());
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
        assertNull(debugServer.getBody());

        //inspect parameters
        assertEquals("123", debugServer.getQueryParameter("registrationId"));
        assertEquals("GCM", debugServer.getQueryParameter("platformType"));

        //inspect response
        assertEquals("11", response.getDeviceApplicationInstanceId());
    }

    @Test(expected = ApiIOException.class)
    public void create_onConnectionError_throwsError() throws Exception {
        debugServer.stop();
        debugServer = null;

        mobileApiRegistration.upsert(null, "123");
    }

    @Test(expected = ApiException.class)
    public void create_onResponseError_throwsError() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, DefaultApiClient.JSON_SERIALIZER.serialize(new ApiResponse("XY", "Some error!")));

        mobileApiRegistration.upsert(null, "123");
    }

    @Test(expected = ApiBackendException.class)
    public void create_onBackendError_throwsError() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.INTERNAL_ERROR, DefaultApiClient.JSON_SERIALIZER.serialize(new ApiResponse("XY", "Some internal error!")));

        mobileApiRegistration.upsert(null, "123");
    }

    @Test
    public void update() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, DefaultApiClient.JSON_SERIALIZER.serialize(new RegistrationResponse("11")));

        mobileApiRegistration.upsert("12", "123");

        //inspect http context
        assertEquals("/mobile/2/registration", debugServer.getUri());
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
    }
}