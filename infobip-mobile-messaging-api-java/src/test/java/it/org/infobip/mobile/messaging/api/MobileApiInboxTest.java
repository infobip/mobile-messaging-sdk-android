package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.inbox.MobileApiInbox;
import org.junit.After;
import org.junit.Before;

public class MobileApiInboxTest {
//    private DebugServer debugServer;
    private MobileApiInbox mobileApiInbox;
    private String externalUserId = "some_externalUserId";

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
//        mobileApiInbox = generator.create(MobileApiInbox.class);
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
//    public void fetch_zero_messages() throws Exception {
//
//        String jsonResponse = "{" +
//                "   \"messages\": []," +
//                "   \"countTotal\": 0," +
//                "   \"countUnread\": 0" +
//                "}";
//
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);
//
//        FetchInboxResponse response = mobileApiInbox.fetchInbox(externalUserId, null, "12345", "23456", "some_topic", 10);
//
//        //inspect http context
//        assertThat(debugServer.getUri()).isEqualTo("/mobile/1/user/" + externalUserId + "/inbox/gcm/messages");
//        assertThat(debugServer.getRequestCount()).isEqualTo(1);
//        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.GET);
//        assertThat(debugServer.getQueryParametersCount()).isEqualTo(4);
//        assertThat(debugServer.getQueryParameter("dateTimeFrom")).isEqualTo("12345");
//        assertThat(debugServer.getQueryParameter("dateTimeTo")).isEqualTo("23456");
//        assertThat(debugServer.getQueryParameter("messageTopic")).isEqualTo("some_topic");
//        assertThat(debugServer.getQueryParameter("limit")).isEqualTo("10");
//        assertThat(response.getCountTotal()).isEqualTo(0);
//        assertThat(response.getCountUnread()).isEqualTo(0);
//    }
//
//    @Test
//    public void fetch_without_filter_options() {
//        String jsonResponse = "{" +
//                "   \"messages\": []," +
//                "   \"countTotal\": 0," +
//                "   \"countUnread\": 0" +
//                "}";
//
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);
//
//        FetchInboxResponse response = mobileApiInbox.fetchInbox(externalUserId, null, null, null, null, null);
//
//        assertThat(debugServer.getUri()).isEqualTo("/mobile/1/user/" + externalUserId + "/inbox/gcm/messages");
//        assertThat(debugServer.getRequestCount()).isEqualTo(1);
//        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.GET);
//        assertThat(debugServer.getQueryParametersCount()).isEqualTo(0);
//        assertThat(response.getCountTotal()).isEqualTo(0);
//        assertThat(response.getCountUnread()).isEqualTo(0);
//        assertThat(response.getMessages().size()).isEqualTo(0);
//    }
//
//    @Test
//    public void fetch_someMessages() throws Exception {
//
//        String jsnResponse1 = "{\n" +
//                "    \"messages\": [\n" +
//                "        {\n" +
//                "            \"gcm.notification.messageId\": \"793fd94e-deda-4298-b41d-ee857d533e23\",\n" +
//                "            \"gcm.notification.title\": \"some title1\",\n" +
//                "            \"gcm.notification.sound\": \"default\",\n" +
//                "            \"gcm.notification.body\": \"some text1\",\n" +
//                "            \"gcm.notification.silent\": false,\n" +
//                "            \"internalData\": \"{" +
//                "\\\"bulkId\\\":\\\"someCampaignId1\\\"," +
//                "\\\"sendDateTime\\\":1643314503233," +
//                "\\\"inbox\\\":{" +
//                "\\\"topic\\\":\\\"default\\\"," +
//                "\\\"seen\\\": true}}\"" +
//                "        },\n" +
//                "        {\n" +
//                "            \"gcm.notification.messageId\": \"9cd50e63-b986-46ce-8992-027fe5e41486\",\n" +
//                "            \"gcm.notification.title\": \"some title2\",\n" +
//                "            \"gcm.notification.sound\": \"default\",\n" +
//                "            \"gcm.notification.body\": \"some text2\",\n" +
//                "            \"gcm.notification.silent\": false,\n" +
//                "            \"internalData\": \"{" +
//                "\\\"bulkId\\\":\\\"someCampaignId2\\\"," +
//                "\\\"sendDateTime\\\":1643314503244," +
//                "\\\"inbox\\\":{" +
//                "\\\"topic\\\":\\\"default\\\"" +
//                "}}\"" +
//                "        }\n" +
//                "    ],\n" +
//                "    \"countTotal\": 2,\n" +
//                "    \"countUnread\": 1\n" +
//                "}";
//
//
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsnResponse1);
//
//        FetchInboxResponse response = mobileApiInbox.fetchInbox(externalUserId, null, "12345", "23456", "some_topic", 10);
//
//        //inspect http context
//        assertThat(debugServer.getUri()).isEqualTo("/mobile/1/user/" + externalUserId + "/inbox/gcm/messages");
//        assertThat(debugServer.getRequestCount()).isEqualTo(1);
//        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.GET);
//        assertThat(debugServer.getQueryParametersCount()).isEqualTo(4);
//        assertThat(debugServer.getQueryParameter("dateTimeFrom")).isEqualTo("12345");
//        assertThat(debugServer.getQueryParameter("dateTimeTo")).isEqualTo("23456");
//        assertThat(debugServer.getQueryParameter("messageTopic")).isEqualTo("some_topic");
//        assertThat(debugServer.getQueryParameter("limit")).isEqualTo("10");
//        assertThat(response.getCountTotal()).isEqualTo(2);
//        assertThat(response.getCountUnread()).isEqualTo(1);
//        assertThat(response.getMessages().size()).isEqualTo(2);
//        assertThat(response.getMessages().get(0).getMessageId()).isEqualTo("793fd94e-deda-4298-b41d-ee857d533e23");
//        assertThat(response.getMessages().get(0).getTitle()).isEqualTo("some title1");
//        assertThat(response.getMessages().get(0).getSound()).isEqualTo("default");
//        assertThat(response.getMessages().get(0).getBody()).isEqualTo("some text1");
//        assertThat(response.getMessages().get(0).getSilent()).isEqualTo("false");
//        assertThat(response.getMessages().get(1).getTitle()).isEqualTo("some title2");
//    }
//
//    @Test
//    public void create_seenInboxReport_success() throws Exception {
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);
//
//        InboxSeenMessages messages = new InboxSeenMessages() {{
//            setMessages(new ArrayList<Message>() {{
//                add(new Message("myMessageId", System.currentTimeMillis()));
//            }}.toArray(new Message[1]));
//        }};
//
//        mobileApiInbox.reportSeen(messages);
//
//        //inspect http context
//        assertThat(debugServer.getUri()).isEqualTo("/mobile/2/messages/seen");
//        assertThat(debugServer.getRequestCount()).isEqualTo(1);
//        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.POST);
//        assertThat(debugServer.getQueryParametersCount()).isEqualTo(0);
//    }
//
//    @Test
//    public void fetch_for_hms_cloudType() throws Exception {
//
//        String jsonResponse = "{" +
//                "   \"messages\": []," +
//                "   \"countTotal\": 0," +
//                "   \"countUnread\": 0" +
//                "}";
//
//        String hmsCloudType = "hms";
//
//        debugServer.respondWith(NanoHTTPD.Response.Status.OK, jsonResponse);
//
//        FetchInboxResponse response = mobileApiInbox.fetchInbox(externalUserId, null, null, null, null, 10, hmsCloudType);
//
//        //inspect http context
//        assertThat(debugServer.getUri()).isEqualTo("/mobile/1/user/" + externalUserId + "/inbox/" + hmsCloudType + "/messages");
//        assertThat(debugServer.getRequestCount()).isEqualTo(1);
//        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.GET);
//    }
}
