package it.org.infobip.mobile.messaging.api;

import org.infobip.mobile.messaging.api.messages.MoMessage;
import org.infobip.mobile.messaging.api.messages.MoMessagesBody;
import org.infobip.mobile.messaging.api.messages.MoMessagesResponse;
import org.infobip.mobile.messaging.api.messages.MobileApiMessages;
import org.infobip.mobile.messaging.api.messages.SeenMessages;
import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.tools.DebugServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import fi.iki.elonen.NanoHTTPD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author sslavin
 * @since 06/05/16.
 */
public class MobileApiMessagesTest {
    private DebugServer debugServer;
    private MobileApiMessages mobileApiMessages;

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

        mobileApiMessages = generator.create(MobileApiMessages.class);
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
    public void create_seenReport_success() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        SeenMessages messages = new SeenMessages()
        {{
            setMessages(new ArrayList<Message>()
            {{
                add(new Message("myMessageId", System.currentTimeMillis()));
            }}.toArray(new Message[1]));
        }};

        mobileApiMessages.reportSeen(messages);

        //inspect http context
        assertThat(debugServer.getUri()).isEqualTo("/mobile/1/messages/seen");
        assertThat(debugServer.getRequestCount()).isEqualTo(1);
        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.POST);
        assertThat(debugServer.getQueryParametersCount()).isEqualTo(0);
    }

    @Test
    public void create_sendMO_success() throws Exception {

        String serverResponse =
        "{" +
            "\"messages\":" +
            "[" +
                "{" +
                    "\"status\" :       \"myStatusId\"," +
                    "\"statusCode\" :   0," +
                    "\"messageId\" :    \"myMessageId\"," +
                    "\"destination\" :  \"myDestination\"," +
                    "\"text\" :         \"myText\"," +
                    "\"customPayload\":" +
                    "{" +
                        "\"myStringKey\" :   \"string\"," +
                        "\"myBooleanKey\":   true," +
                        "\"myNumberKey\" :   1" +
                    "}" +
                "}," +
                "{" +
                    "\"status\" :       \"myStatusId2\"," +
                    "\"statusCode\" :   1," +
                    "\"messageId\" :    \"myMessageId2\"," +
                    "\"destination\" :  \"myDestination2\"," +
                    "\"text\" :         \"myText2\"," +
                    "\"customPayload\":" +
                    "{" +
                        "\"myStringKey\" :   \"string2\"," +
                        "\"myBooleanKey\":   false," +
                        "\"myNumberKey\" :   2" +
                    "}" +
                "}" +
            "]" +
        "}";

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, serverResponse);

        MoMessage moMessages[] =
        {
            new MoMessage("myMessageId", "myDestination", "myText", new HashMap<String, Object>()
                {{
                    put("myStringKey", "string1");
                    put("myNubmberKey", 1);
                    put("myBooleanKey", true);
                }}),
            new MoMessage("myMessageId2", "myDestination2", "myText2", new HashMap<String, Object>()
                {{
                    put("myStringKey", "string2");
                    put("myNubmberKey", 2);
                    put("myBooleanKey", false);
                }})
        };

        MoMessagesBody requestBody = new MoMessagesBody();
        requestBody.setFrom("fromTest");
        requestBody.setMessages(moMessages);

        MoMessagesResponse moMessagesResponse = mobileApiMessages.sendMO(requestBody);

        // inspect http context
        assertThat(debugServer.getUri()).isEqualTo("/mobile/1/messages/mo");
        assertThat(debugServer.getRequestCount()).isEqualTo(1);
        assertThat(debugServer.getRequestMethod()).isEqualTo(NanoHTTPD.Method.POST);
        assertThat(debugServer.getQueryParametersCount()).isEqualTo(1);

        // inspect parameters
        assertEquals("GCM", debugServer.getQueryParameter("platformType"));

        // inspect response
        assertEquals(2, moMessagesResponse.getMessages().length);
        assertEquals("myMessageId", moMessagesResponse.getMessages()[0].getMessageId());
        assertEquals("myStatusId", moMessagesResponse.getMessages()[0].getStatus());
        assertEquals(0, moMessagesResponse.getMessages()[0].getStatusCode());
        assertEquals("myDestination", moMessagesResponse.getMessages()[0].getDestination());
        assertEquals("myText", moMessagesResponse.getMessages()[0].getText());
        assertEquals("string", moMessagesResponse.getMessages()[0].getCustomPayload().get("myStringKey"));
        assertEquals(1.0, moMessagesResponse.getMessages()[0].getCustomPayload().get("myNumberKey"));
        assertEquals(true, moMessagesResponse.getMessages()[0].getCustomPayload().get("myBooleanKey"));
        assertEquals("myMessageId2", moMessagesResponse.getMessages()[1].getMessageId());
        assertEquals("myStatusId2", moMessagesResponse.getMessages()[1].getStatus());
        assertEquals(1, moMessagesResponse.getMessages()[1].getStatusCode());
        assertEquals("myDestination2", moMessagesResponse.getMessages()[1].getDestination());
        assertEquals("myText2", moMessagesResponse.getMessages()[1].getText());
        assertEquals("string2", moMessagesResponse.getMessages()[1].getCustomPayload().get("myStringKey"));
        assertEquals(2.0, moMessagesResponse.getMessages()[1].getCustomPayload().get("myNumberKey"));
        assertEquals(false, moMessagesResponse.getMessages()[1].getCustomPayload().get("myBooleanKey"));
    }
}
