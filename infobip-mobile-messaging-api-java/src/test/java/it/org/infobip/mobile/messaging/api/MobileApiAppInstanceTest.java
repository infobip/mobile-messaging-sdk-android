package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.junit.After;
import org.junit.Before;


public class MobileApiAppInstanceTest {

//    private DebugServer debugServer;
    private MobileApiAppInstance mobileApiAppInstance;
    private String regId = "1234regId567";

    @Before
    public void setUp() throws Exception {
//        debugServer = new DebugServer();
//        debugServer.start();
//
//        Properties properties = new Properties();
//        properties.put("api.key", "my_API_key");
//        Generator generator = new Generator.Builder()
//                .withBaseUrl("http://127.0.0.1:" + debugServer.getListeningPort() + "/")
//                .withProperties(properties)
//                .build();
//
//        mobileApiAppInstance = generator.create(MobileApiAppInstance.class);
    }

    @After
    public void tearDown() throws Exception {
//        if (null != debugServer) {
//            try {
//                debugServer.stop();
//            } catch (Exception e) {
//                //ignore
//            }
//        }
    }

//    @Test
//    public void create_instance_success_examineResponse() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        mobileApiAppInstance.createInstance(new AppInstance());
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance", debugServer.getUri());
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
//        assertEquals(1, debugServer.getQueryParametersCount());
//        assertEquals("true", debugServer.getQueryParameter("ri"));
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//    }
//
//    @Test
//    public void patch_instance_success_examineResponse() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        mobileApiAppInstance.patchInstance(regId, new HashMap<String, Object>());
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567", debugServer.getUri());
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
//        assertEquals(HttpMethod.PATCH.name(), debugServer.getHeader("X-HTTP-Method-Override"));
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertEquals(0, debugServer.getQueryParametersCount());
//    }
//
//    @Test
//    public void get_instance_success_examineResponse() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        mobileApiAppInstance.getInstance(regId);
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567", debugServer.getUri());
//        assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(0, debugServer.getQueryParametersCount());
//    }
//
//    @Test
//    public void patch_userData_success_examineResponse() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        mobileApiAppInstance.patchUser(regId, new HashMap<String, Object>());
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567/user", debugServer.getUri());
//        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
//        assertEquals(HttpMethod.PATCH.name(), debugServer.getHeader("X-HTTP-Method-Override"));
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(0, debugServer.getQueryParametersCount());
//    }
//
//    @Test
//    public void get_userData_success_examineResponse() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        mobileApiAppInstance.getUser(regId);
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567/user", debugServer.getUri());
//        assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertEquals("true", debugServer.getQueryParameter("ri"));
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(1, debugServer.getQueryParametersCount());
//    }
//
//    @Test
//    public void repersonalize_success_examineResponse() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        mobileApiAppInstance.repersonalize(regId, new UserPersonalizeBody());
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567/repersonalize", debugServer.getUri());
//        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(0, debugServer.getQueryParametersCount());
//    }
//
//    @Test
//    public void personalize_success_examineResponse() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        mobileApiAppInstance.personalize(regId, true, new UserPersonalizeBody());
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567/personalize", debugServer.getUri());
//        assertEquals("true", debugServer.getQueryParameter("forceDepersonalize"));
//        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(1, debugServer.getQueryParametersCount());
//    }
//
//    @Test
//    public void depersonalize_success_examineResponse() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        mobileApiAppInstance.depersonalize(regId);
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567/depersonalize", debugServer.getUri());
//        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(0, debugServer.getQueryParametersCount());
//    }
//
//    @Test(expected = ApiIOException.class)
//    public void createInstance_onConnectionError_throwsError() throws Exception {
//        debugServer.stop();
//        debugServer = null;
//
//        mobileApiAppInstance.createInstance(new AppInstance());
//    }
//
//    @Test(expected = ApiException.class)
//    public void createInstance_onResponseError_throwsError() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, DefaultApiClient.JSON_SERIALIZER.serialize(new ApiResponse("XY", "Some error!")));
//
//        mobileApiAppInstance.createInstance(new AppInstance());
//    }
//
//    @Test(expected = ApiBackendException.class)
//    public void createInstance_onBackendError_throwsError() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.INTERNAL_ERROR, DefaultApiClient.JSON_SERIALIZER.serialize(new ApiResponse("XY", "Some internal error!")));
//
//        mobileApiAppInstance.createInstance(new AppInstance());
//    }
//
//    @Test
//    public void getUser_examineResponse() {
//        String jsonResponse = "{\n" +
//                "  \"customAttributes\": {\n" +
//                "    \"number_1\": 1234.3,\n" +
//                "    \"string_1\": \"blabla\",\n" +
//                "    \"date_1\": \"2018-12-13\"\n" +
//                "  },\n" +
//                "  \"externalUserId\":\"father_of_luke\",\n" +
//                "  \"emails\": [\n" +
//                "    {\n" +
//                "      \"address\": \"darth_vader@mail.com\",\n" +
//                "      \"preferred\": false\n" +
//                "    }\n" +
//                "  ],\n" +
//                "  \"firstName\": \"Darth\",\n" +
//                "  \"middleName\": \"Beloved\",\n" +
//                "  \"lastName\": \"Vader\",\n" +
//                "  \"birthday\": \"1988-07-31\",\n" +
//                "  \"gender\": \"Male\",\n" +
//                "  \"phones\": [\n" +
//                "    {\n" +
//                "      \"number\": \"385991111666\",\n" +
//                "      \"preferred\": false\n" +
//                "    }\n" +
//                "  ],\n" +
//                "  \"instances\": [\n" +
//                "    {\n" +
//                "      \"appVersion\": \"1.18.0-SNAPSHOT\",\n" +
//                "      \"deviceManufacturer\": \"motorola\",\n" +
//                "      \"deviceModel\": \"Nexus 6\",\n" +
//                "      \"deviceName\": \"Nexus 6\",\n" +
//                "      \"deviceSecure\": true,\n" +
//                "      \"isPrimary\": false,\n" +
//                "      \"notificationsEnabled\": true,\n" +
//                "      \"os\": \"Android\",\n" +
//                "      \"language\": \"en\",\n" +
//                "      \"osVersion\": \"7.1.1\",\n" +
//                "      \"pushRegId\": \"pushRegId\",\n" +
//                "      \"regEnabled\": true,\n" +
//                "      \"sdkVersion\": \"1.18.0\"\n" +
//                "    }\n" +
//                "  ],\n" +
//                "  \"tags\": [\n" +
//                "    \"music\",\n" +
//                "    \"darkSide\"\n" +
//                "  ]\n" +
//                "}";
//
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);
//
//        UserBody response = mobileApiAppInstance.getUser(regId);
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567/user", debugServer.getUri());
//        assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertEquals("true", debugServer.getQueryParameter("ri"));
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(1, debugServer.getQueryParametersCount());
//        assertNull(debugServer.getBody());
//
//        //inspect response
//        assertEquals("Darth", response.getFirstName());
//        assertEquals("Beloved", response.getMiddleName());
//        assertEquals("Vader", response.getLastName());
//        assertEquals("1988-07-31", response.getBirthday());
//        assertEquals("Male", response.getGender());
//        assertEquals("father_of_luke", response.getExternalUserId());
//        HashMap<String, Object> expectedCustomAtts = new HashMap<>();
//        expectedCustomAtts.put("number_1", 1234.3);
//        expectedCustomAtts.put("string_1", "blabla");
//        expectedCustomAtts.put("date_1", "2018-12-13");
//        assertEquals(expectedCustomAtts, response.getCustomAttributes());
//        assertEquals(new HashSet<String>() {{
//            add("music");
//            add("darkSide");
//        }}, response.getTags());
//        assertEquals(new UserBody.Phone("385991111666"), response.getPhones().toArray()[0]);
//        assertEquals(new UserBody.Email("darth_vader@mail.com"), response.getEmails().toArray()[0]);
//    }
//
//    @Test
//    public void getInstance_examineResponse() {
//        String jsonResponse = "{" +
//                "      \"appVersion\": \"1.18.0-SNAPSHOT\",\n" +
//                "      \"deviceManufacturer\": \"motorola\",\n" +
//                "      \"deviceModel\": \"Nexus 6\",\n" +
//                "      \"deviceName\": \"Nexus 6\",\n" +
//                "      \"deviceSecure\": true,\n" +
//                "      \"isPrimary\": false,\n" +
//                "      \"notificationsEnabled\": true,\n" +
//                "      \"os\": \"Android\",\n" +
//                "      \"language\": \"en\",\n" +
//                "      \"osVersion\": \"7.1.1\",\n" +
//                "      \"pushRegId\": \"pushRegId\",\n" +
//                "      \"regEnabled\": true,\n" +
//                "      \"sdkVersion\": \"1.18.0\"\n" +
//                "    }";
//
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);
//
//        AppInstance response = mobileApiAppInstance.getInstance(regId);
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567", debugServer.getUri());
//        assertEquals(NanoHTTPD.Method.GET, debugServer.getRequestMethod());
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(0, debugServer.getQueryParametersCount());
//        assertNull(debugServer.getBody());
//
//        assertEquals("1.18.0-SNAPSHOT", response.getAppVersion());
//        assertEquals("motorola", response.getDeviceManufacturer());
//        assertEquals("Nexus 6", response.getDeviceModel());
//        assertEquals("Nexus 6", response.getDeviceName());
//        assertEquals(true, response.getDeviceSecure());
//        assertEquals(false, response.getIsPrimary());
//        assertEquals(true, response.getNotificationsEnabled());
//        assertEquals("Android", response.getOs());
//        assertEquals("en", response.getLanguage());
//        assertEquals("7.1.1", response.getOsVersion());
//        assertEquals("pushRegId", response.getPushRegId());
//        assertEquals(true, response.getRegEnabled());
//        assertEquals("1.18.0", response.getSdkVersion());
//    }
//
//    @Test
//    public void send_user_session_report__success_examineResponse() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        mobileApiAppInstance.sendUserSessionReport(regId, new UserSessionEventBody());
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567/user/events/session", debugServer.getUri());
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
//        assertEquals(0, debugServer.getQueryParametersCount());
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//    }
//
//    @Test
//    public void send_user_custom_event__success_examineReqAndResponse() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        String userCustomEventRequest =
//                "{\"events\":[" +
//                        "{\"definitionId\":\"123\",\"date\":\"2.2.22\",\"properties\":{}}" +
//                        "]}";
//        UserCustomEventBody.CustomEvent customEvent = new UserCustomEventBody.CustomEvent("123", "2.2.22", new HashMap<String, Object>());
//        UserCustomEventBody userCustomEventBody = new UserCustomEventBody(new UserCustomEventBody.CustomEvent[]{customEvent});
//
//        mobileApiAppInstance.sendUserCustomEvents(regId, false, userCustomEventBody);
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567/user/events/custom", debugServer.getUri());
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
//        assertEquals(1, debugServer.getQueryParametersCount());
//        assertEquals("false", debugServer.getQueryParameter("validate"));
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertEquals(userCustomEventBody.toString(), userCustomEventRequest);
//        assertEquals(userCustomEventBody.toString(), debugServer.getBody());
//    }
//
//    @Test
//    public void send_user_custom_event_with_validation__success_examineReqAndResponse() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        String userCustomEventRequest =
//                "{\"events\":[" +
//                        "{\"definitionId\":\"123\",\"date\":\"2.2.22\",\"properties\":{}}" +
//                        "]}";
//        UserCustomEventBody.CustomEvent customEvent = new UserCustomEventBody.CustomEvent("123", "2.2.22", new HashMap<String, Object>());
//        UserCustomEventBody userCustomEventBody = new UserCustomEventBody(new UserCustomEventBody.CustomEvent[]{customEvent});
//
//        mobileApiAppInstance.sendUserCustomEvents(regId, true, userCustomEventBody);
//
//        //inspect http context
//        assertEquals("/mobile/1/appinstance/1234regId567/user/events/custom", debugServer.getUri());
//        assertEquals(1, debugServer.getRequestCount());
//        assertEquals(NanoHTTPD.Method.POST, debugServer.getRequestMethod());
//        assertEquals(1, debugServer.getQueryParametersCount());
//        assertEquals("true", debugServer.getQueryParameter("validate"));
//        assertEquals("App my_API_key", debugServer.getHeader("Authorization"));
//        assertEquals(userCustomEventBody.toString(), userCustomEventRequest);
//        assertEquals(userCustomEventBody.toString(), debugServer.getBody());
//    }
}
