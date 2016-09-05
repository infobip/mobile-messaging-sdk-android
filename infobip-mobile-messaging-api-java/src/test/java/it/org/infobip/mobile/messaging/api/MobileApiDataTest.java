package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.data.MobileApiData;
import org.infobip.mobile.messaging.api.data.SystemDataReport;
import org.infobip.mobile.messaging.api.data.UserDataReport;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.api.tools.DebugServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Properties;

import fi.iki.elonen.NanoHTTPD;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class MobileApiDataTest {

    private DebugServer debugServer;
    private MobileApiData mobileApiData;


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

        mobileApiData = generator.create(MobileApiData.class);
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
    public void create_userData_success_examineResponse() throws Exception {
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

        UserDataReport response = mobileApiData.reportUserData("myDeviceInstanceId", "myExternalUserId", null);

        //inspect http context
        assertEquals("/mobile/1/data/user", debugServer.getUri());
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

    @Test
    public void create_systemData_success_examineRequest() throws Exception {

        // prepare request data
        SystemDataReport systemDataReport = new SystemDataReport();
        systemDataReport.setSdkVersion("1.2.3.TEST");
        systemDataReport.setOsVersion("0.1.2.TEST");
        systemDataReport.setDeviceManufacturer("INFOBIP");
        systemDataReport.setDeviceModel("TEST");
        systemDataReport.setApplicationVersion("3.4.5.TEST");
        systemDataReport.setGeofencing(false);

        // prepare server
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        // send request
        mobileApiData.reportSystemData("myDeviceInstanceId", systemDataReport);

        //inspect http context
        assertEquals("/mobile/1/data/system", debugServer.getUri());
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
        assertEquals(1, debugServer.getQueryParametersCount());
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
        assertNotNull(debugServer.getBody());

        //inspect parameters
        assertEquals("myDeviceInstanceId", debugServer.getQueryParameter("deviceApplicationInstanceId"));

        //inspect request
        HashMap<String, Object> request = new JsonSerializer().deserialize(debugServer.getBody(), HashMap.class);
        assertEquals("1.2.3.TEST", request.get("sdkVersion"));
        assertEquals("0.1.2.TEST", request.get("osVersion"));
        assertEquals("INFOBIP", request.get("deviceManufacturer"));
        assertEquals("TEST", request.get("deviceModel"));
        assertEquals("3.4.5.TEST", request.get("applicationVersion"));
        assertEquals(false, request.get("geofencing"));
    }
}
