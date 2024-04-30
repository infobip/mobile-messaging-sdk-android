package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.chat.MobileApiChat;
import org.junit.After;
import org.junit.Before;


public class MobileApiChatTest {
//    private DebugServer debugServer;
    private MobileApiChat mobileApiChat;

    @Before
    public void setUp() throws Exception {
//        debugServer = new DebugServer();
//        debugServer.start();

//        Properties properties = new Properties();
//        properties.put("api.key", "my_API_key");
//        Generator generator = new Generator.Builder()
//                .withBaseUrl("http://127.0.0.1:" + debugServer.getListeningPort() + "/")
//                .withProperties(properties)
//                .build();
//
//        mobileApiChat = generator.create(MobileApiChat.class);
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
//    public void create_eventReport_success() throws Exception {
//        String jsonResponse = "{" +
//                "  'id':'widgetId123'," +
//                "  'title':'widgetTitle'," +
//                "  'primaryColor':'widgetPrimaryColor'," +
//                "  'backgroundColor':'widgetBackgroundColor'," +
//                "  'maxUploadContentSize':1024" +
//                "}";
//
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);
//
//        WidgetInfo response = mobileApiChat.getWidgetConfiguration();
//
//        //inspect http context
//        assertThat(debugServer.getUri()).isEqualTo("/mobile/1/chat/widget");
//        assertThat(debugServer.getRequestCount()).isEqualTo(1);
//        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.GET);
//        assertThat(debugServer.getQueryParametersCount()).isEqualTo(0);
//
//        assertThat(response.getId()).isEqualTo("widgetId123");
//        assertThat(response.getTitle()).isEqualTo("widgetTitle");
//        assertThat(response.getPrimaryColor()).isEqualTo("widgetPrimaryColor");
//        assertThat(response.getBackgroundColor()).isEqualTo("widgetBackgroundColor");
//        assertThat(response.getMaxUploadContentSize()).isEqualTo(1024L);
//    }
}
