package org.infobip.mobile.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import org.infobip.mobile.messaging.tools.InfobipAndroidTestCase;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author sslavin
 * @since 22/07/16.
 */
public class SendMOMessageTest extends InfobipAndroidTestCase {

    private BroadcastReceiver receiver;
    private ArgumentCaptor<Intent> captor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        captor = ArgumentCaptor.forClass(Intent.class);
        receiver = Mockito.mock(BroadcastReceiver.class);
        getInstrumentation().getContext().registerReceiver(receiver, new IntentFilter(Event.MESSAGES_SENT.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        getInstrumentation().getContext().unregisterReceiver(receiver);

        super.tearDown();
    }

    public void test_sendMultipleMessages() throws Exception {

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

        Message messagesToSend[] = {new Message(), new Message()};
        mobileMessaging.sendMessages(messagesToSend);

        Mockito.verify(receiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
        assertTrue(captor.getValue().hasExtra(BroadcastParameter.EXTRA_MESSAGES));

        ArrayList<Bundle> bundles = captor.getValue().getParcelableArrayListExtra(BroadcastParameter.EXTRA_MESSAGES);
        List<Message> messages = Message.createFrom(bundles);

        assertEquals("myMessageId", messages.get(0).getMessageId());
        assertEquals(Message.Status.ERROR, messages.get(0).getStatus());
        assertEquals("Message not sent", messages.get(0).getStatusMessage());
        assertEquals("myDestination", messages.get(0).getDestination());
        assertEquals("myText", messages.get(0).getBody());
        assertEquals("string", messages.get(0).getCustomPayload().opt("myStringKey"));
        assertEquals(1.0, messages.get(0).getCustomPayload().optDouble("myNumberKey"), 0.01);
        assertEquals(true, messages.get(0).getCustomPayload().opt("myBooleanKey"));
        assertEquals("myMessageId2", messages.get(1).getMessageId());
        assertEquals(Message.Status.SUCCESS, messages.get(1).getStatus());
        assertEquals("Message sent", messages.get(1).getStatusMessage());
        assertEquals("myDestination2", messages.get(1).getDestination());
        assertEquals("myText2", messages.get(1).getBody());
        assertEquals("string2", messages.get(1).getCustomPayload().opt("myStringKey"));
        assertEquals(2.0, messages.get(1).getCustomPayload().optDouble("myNumberKey"), 0.01);
        assertEquals(false, messages.get(1).getCustomPayload().opt("myBooleanKey"));
    }
}
