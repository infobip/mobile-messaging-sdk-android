package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.AppInstanceWithPushRegId;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.api.support.ApiBackendException;
import org.infobip.mobile.messaging.api.support.ApiException;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;
import org.infobip.mobile.messaging.api.support.http.client.model.ApiResponse;
import org.infobip.mobile.messaging.api.tools.DebugServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import fi.iki.elonen.NanoHTTPD;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class MobileApiAppInstanceTest {

    private DebugServer debugServer;
    private MobileApiAppInstance mobileApiAppInstance;
    private String regId = "1234regId567";

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

        mobileApiAppInstance = generator.create(MobileApiAppInstance.class);
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
    public void create_instance_success_examineResponse() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileApiAppInstance.createInstance(false, new AppInstance());

        //inspect http context
        assertEquals("/mobile/1/appinstance", debugServer.getUri());
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
        assertEquals(2, debugServer.getQueryParametersCount());
        assertEquals("false", debugServer.getQueryParameter("rt"));
        assertEquals("true", debugServer.getQueryParameter("ri"));
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
    }

    @Test
    public void patch_instance_success_examineResponse() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileApiAppInstance.patchInstance(regId, false, new AppInstance());

        //inspect http context
        assertEquals("/mobile/1/appinstance/1234regId567", debugServer.getUri());
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
        assertEquals(HttpMethod.PATCH.name(), debugServer.getHeader("X-HTTP-Method-Override"));
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
        assertEquals("false", debugServer.getQueryParameter("ri"));
        assertEquals("false", debugServer.getQueryParameter("rt"));
        assertEquals(2, debugServer.getQueryParametersCount());
    }

    @Test
    public void get_instance_success_examineResponse() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileApiAppInstance.getInstance(regId);

        //inspect http context
        assertEquals("/mobile/1/appinstance/1234regId567", debugServer.getUri());
        assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(0, debugServer.getQueryParametersCount());
    }

    @Test
    public void expire_instance_success_examineResponse() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileApiAppInstance.expireInstance(regId);

        //inspect http context
        assertEquals("/mobile/1/appinstance/1234regId567", debugServer.getUri());
        assertEquals(NanoHTTPD.Method.DELETE, debugServer.getRequestMethod());
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(0, debugServer.getQueryParametersCount());
    }

    @Test
    public void patch_userData_success_examineResponse() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileApiAppInstance.patchUser(regId, false, new UserBody());

        //inspect http context
        assertEquals("/mobile/1/appinstance/1234regId567/user", debugServer.getUri());
        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
        assertEquals(HttpMethod.PATCH.name(), debugServer.getHeader("X-HTTP-Method-Override"));
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
        assertEquals("false", debugServer.getQueryParameter("ru"));
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(1, debugServer.getQueryParametersCount());
    }

    @Test
    public void get_userData_success_examineResponse() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileApiAppInstance.getUser(regId);

        //inspect http context
        assertEquals("/mobile/1/appinstance/1234regId567/user", debugServer.getUri());
        assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
        assertEquals("true", debugServer.getQueryParameter("ri"));
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(1, debugServer.getQueryParametersCount());
    }


    @Test
    public void log_out_user_success_examineResponse() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileApiAppInstance.logoutUser(regId);

        //inspect http context
        assertEquals("/mobile/1/appinstance/1234regId567/logout", debugServer.getUri());
        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(0, debugServer.getQueryParametersCount());
    }

    @Test(expected = ApiIOException.class)
    public void createInstance_onConnectionError_throwsError() throws Exception {
        debugServer.stop();
        debugServer = null;

        mobileApiAppInstance.createInstance(false, new AppInstance());
    }

    @Test(expected = ApiException.class)
    public void createInstance_onResponseError_throwsError() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, DefaultApiClient.JSON_SERIALIZER.serialize(new ApiResponse("XY", "Some error!")));

        mobileApiAppInstance.createInstance(false, new AppInstance());
    }

    @Test(expected = ApiBackendException.class)
    public void createInstance_onBackendError_throwsError() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.INTERNAL_ERROR, DefaultApiClient.JSON_SERIALIZER.serialize(new ApiResponse("XY", "Some internal error!")));

        mobileApiAppInstance.createInstance(false, new AppInstance());
    }

    @Test
    public void getUser_examineResponse() {
        String jsonResponse = "{\n" +
                "  \"customAttributes\": {\n" +
                "    \"number_1\": 1234.3,\n" +
                "    \"string_1\": \"blabla\",\n" +
                "    \"date_1\": \"2018-12-13\"\n" +
                "  },\n" +
                "  \"emails\": [\n" +
                "    {\n" +
                "      \"address\": \"darth_vader@mail.com\",\n" +
                "      \"preferred\": false\n" +
                "    }\n" +
                "  ],\n" +
                "  \"firstName\": \"Darth\",\n" +
                "  \"middleName\": \"Beloved\",\n" +
                "  \"lastName\": \"Vader\",\n" +
                "  \"birthday\": \"1988-07-31\",\n" +
                "  \"gender\": \"Male\",\n" +
                "  \"gsms\": [\n" +
                "    {\n" +
                "      \"number\": \"385991111666\",\n" +
                "      \"preferred\": false\n" +
                "    }\n" +
                "  ],\n" +
                "  \"instances\": [\n" +
                "    {\n" +
                "      \"appVersion\": \"1.17.0-SNAPSHOT\",\n" +
                "      \"deviceManufacturer\": \"motorola\",\n" +
                "      \"deviceModel\": \"Nexus 6\",\n" +
                "      \"deviceName\": \"Nexus 6\",\n" +
                "      \"deviceSecure\": true,\n" +
                "      \"geoEnabled\": true,\n" +
                "      \"isPrimary\": false,\n" +
                "      \"notificationsEnabled\": true,\n" +
                "      \"os\": \"Android\",\n" +
                "      \"osLanguage\": \"en\",\n" +
                "      \"osVersion\": \"7.1.1\",\n" +
                "      \"pushRegId\": \"pushRegId\",\n" +
                "      \"regEnabled\": true,\n" +
                "      \"sdkVersion\": \"1.18.0\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"tags\": [\n" +
                "    \"music\",\n" +
                "    \"darkSide\"\n" +
                "  ]\n" +
                "}";

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);

        UserBody response = mobileApiAppInstance.getUser(regId);

        //inspect http context
        assertEquals("/mobile/1/appinstance/1234regId567/user", debugServer.getUri());
        assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
        assertEquals("true", debugServer.getQueryParameter("ri"));
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(1, debugServer.getQueryParametersCount());
        assertNull(debugServer.getBody());

        //inspect response
        assertEquals("Darth", response.getFirstName());
        assertEquals("Beloved", response.getMiddleName());
        assertEquals("Vader", response.getLastName());
        assertEquals("1988-07-31", response.getBirthday());
        assertEquals("Male", response.getGender());
        HashMap<String, Object> expectedCustomAtts = new HashMap<>();
        expectedCustomAtts.put("number_1", 1234.3);
        expectedCustomAtts.put("string_1", "blabla");
        expectedCustomAtts.put("date_1", "2018-12-13");
        assertEquals(expectedCustomAtts, response.getCustomAttributes());
        assertEquals(new HashSet<String>() {{
            add("music");
            add("darkSide");
        }}, response.getTags());
    }

    @Test
    public void getInstance_examineResponse() {
        String jsonResponse = "{" +
                "      \"appVersion\": \"1.17.0-SNAPSHOT\",\n" +
                "      \"deviceManufacturer\": \"motorola\",\n" +
                "      \"deviceModel\": \"Nexus 6\",\n" +
                "      \"deviceName\": \"Nexus 6\",\n" +
                "      \"deviceSecure\": true,\n" +
                "      \"geoEnabled\": true,\n" +
                "      \"isPrimary\": false,\n" +
                "      \"notificationsEnabled\": true,\n" +
                "      \"os\": \"Android\",\n" +
                "      \"osLanguage\": \"en\",\n" +
                "      \"osVersion\": \"7.1.1\",\n" +
                "      \"pushRegId\": \"pushRegId\",\n" +
                "      \"regEnabled\": true,\n" +
                "      \"sdkVersion\": \"1.18.0\"\n" +
                "    }";

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);

        AppInstanceWithPushRegId response = mobileApiAppInstance.getInstance(regId);

        //inspect http context
        assertEquals("/mobile/1/appinstance/1234regId567", debugServer.getUri());
        assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
        assertEquals(1, debugServer.getRequestCount());
        assertEquals(0, debugServer.getQueryParametersCount());
        assertNull(debugServer.getBody());

        assertEquals("1.17.0-SNAPSHOT", response.getAppVersion());
        assertEquals("motorola", response.getDeviceManufacturer());
        assertEquals("Nexus 6", response.getDeviceModel());
        assertEquals("Nexus 6", response.getDeviceName());
        assertEquals(true, response.getDeviceSecure());
        assertEquals(true, response.getGeoEnabled());
        assertEquals(false, response.getIsPrimary());
        assertEquals(true, response.getNotificationsEnabled());
        assertEquals("Android", response.getOs());
        assertEquals("en", response.getOsLanguage());
        assertEquals("7.1.1", response.getOsVersion());
        assertEquals("pushRegId", response.getPushRegId());
        assertEquals(true, response.getRegEnabled());
        assertEquals("1.18.0", response.getSdkVersion());
    }
}
