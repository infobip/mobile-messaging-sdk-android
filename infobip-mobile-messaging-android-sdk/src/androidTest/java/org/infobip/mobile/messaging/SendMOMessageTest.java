package org.infobip.mobile.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.tools.DebugServer;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author sslavin
 * @since 22/07/16.
 */
public class SendMOMessageTest extends InstrumentationTestCase {

    DebugServer debugServer;
    BroadcastReceiver receiver;
    ArgumentCaptor<Intent> captor;
    MobileMessaging mobileMessaging;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mobileMessaging = MobileMessaging.getInstance(getInstrumentation().getContext());

        debugServer = new DebugServer();
        debugServer.start();

        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");

        captor = ArgumentCaptor.forClass(Intent.class);
        receiver = Mockito.mock(BroadcastReceiver.class);
        getInstrumentation().getContext().registerReceiver(receiver, new IntentFilter(Event.MESSAGES_SENT.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        getInstrumentation().getContext().unregisterReceiver(receiver);

        if (null != debugServer) {
            try {
                debugServer.stop();
            } catch (Exception e) {
                //ignore
            }
        }

        super.tearDown();
    }

    public void test_sendMultipleMessages() {

        String serverResponse =
        "{" +
            "\"messages\":" +
            "[" +
                "{" +
                    "\"status\" :       \"Message not sent\"," +
                    "\"statusCode\" :   1," +
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
                    "\"status\" :       \"Message sent\"," +
                    "\"statusCode\" :   0," +
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
            new MoMessage("dest", "test", null),
            new MoMessage("dest", "test", null)
        };

        mobileMessaging.sendMessages(moMessages);

        Mockito.verify(receiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
        assertTrue(captor.getValue().hasExtra(BroadcastParameter.EXTRA_MO_MESSAGES));

        MoMessage messages[] = MoMessage.createFrom(captor.getValue().getStringArrayListExtra(BroadcastParameter.EXTRA_MO_MESSAGES));
        assertEquals("myMessageId", messages[0].getMessageId());
        assertEquals(MoMessage.Status.ERROR, messages[0].getStatus());
        assertEquals("Message not sent", messages[0].getStatusMessage());
        assertEquals("myDestination", messages[0].getDestination());
        assertEquals("myText", messages[0].getText());
        assertEquals("string", messages[0].getCustomPayload().get("myStringKey"));
        assertEquals(1.0, messages[0].getCustomPayload().get("myNumberKey"));
        assertEquals(true, messages[0].getCustomPayload().get("myBooleanKey"));
        assertEquals("myMessageId2", messages[1].getMessageId());
        assertEquals(MoMessage.Status.SUCCESS, messages[1].getStatus());
        assertEquals("Message sent", messages[1].getStatusMessage());
        assertEquals("myDestination2", messages[1].getDestination());
        assertEquals("myText2", messages[1].getText());
        assertEquals("string2", messages[1].getCustomPayload().get("myStringKey"));
        assertEquals(2.0, messages[1].getCustomPayload().get("myNumberKey"));
        assertEquals(false, messages[1].getCustomPayload().get("myBooleanKey"));
    }
}
