package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.tools.DebugServer;
import org.infobip.mobile.messaging.api.userdata.MobileApiUserDataSync;
import org.infobip.mobile.messaging.api.userdata.UserDataReport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import fi.iki.elonen.NanoHTTPD;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author sslavin
 * @since 15/07/16.
 */
public class MobileApiUserDataReportSyncTest {
    private DebugServer debugServer;
    private MobileApiUserDataSync mobileApiUserDataSync;

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

        mobileApiUserDataSync = generator.create(MobileApiUserDataSync.class);
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
    public void create_success_examineResponse() throws Exception {
        String jsonResponse =
        "{" +
                "\"predefinedUserData\":" +
                "{" +
                    "\"msisdn\"     :   \"1234567890\"," +
                    "\"firstName\"  :   \"Firstname\"," +
                    "\"lastName\"   :   \"Lastname\"," +
                    "\"gender\"     :   \"Gender\"," +
                    "\"birthdate\"  :   \"2016-12-31\"," +
                    "\"email\"      :   \"user@mailbox.com\"" +
                "}," +
                "\"customUserData\":" +
                "{" +
                    "\"SomeString\" :   \"String\"," +
                    "\"SomeBoolean\":   true," +
                    "\"SomeDouble\" :   1.0" +
                "}" +
        "}";

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);

        UserDataReport response = mobileApiUserDataSync.sync("myDeviceInstanceId", "myExternalUserId", null);

        //inspect http context
        assertEquals("/mobile/1/userdata", debugServer.getUri());
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
        assertEquals(2, debugServer.getQueryParametersCount());
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
        assertNull(debugServer.getBody());

        //inspect parameters
        assertEquals("myDeviceInstanceId", debugServer.getQueryParameter("deviceApplicationInstanceId"));
        assertEquals("myExternalUserId", debugServer.getQueryParameter("externalUserId"));

        //inspect response
        assertEquals("1234567890", response.getPredefinedUserData().get("msisdn"));
        assertEquals("Firstname", response.getPredefinedUserData().get("firstName"));
        assertEquals("Lastname", response.getPredefinedUserData().get("lastName"));
        assertEquals("Gender", response.getPredefinedUserData().get("gender"));
        assertEquals("2016-12-31", response.getPredefinedUserData().get("birthdate"));
        assertEquals("user@mailbox.com", response.getPredefinedUserData().get("email"));
        assertEquals("String", response.getCustomUserData().get("SomeString"));
        assertEquals(true, response.getCustomUserData().get("SomeBoolean"));
        assertEquals(1.0, response.getCustomUserData().get("SomeDouble"));
    }
}
