package org.infobip.mobile.messaging.cloud.firebase;

import android.os.Bundle;

import com.google.firebase.messaging.RemoteMessage;

import junit.framework.TestCase;

import org.infobip.mobile.messaging.Message;

/**
 * @author sslavin
 * @since 05/09/2018.
 */
public class FirebaseMessageMapperTest extends TestCase {

    public void test_shouldMapRemoteMessage() {

        final String ibData = dqjson("{" +
                "'messageId':'messageId'," +
                "'text':'text'," +
                "'notification': {" +
                "   'icon':'icon'," +
                "   'title':'title'," +
                "   'sound':'sound'," +
                "   'category':'category'," +
                "   'vibrate':false," +
                "   'silent':true," +
                "   'contentUrl':'contentUrl'," +
                "   'inAppStyle':0" +
                "}," +
                "'custom': {" +
                "   'key':'value'," +
                "   'key2':1," +
                "   'key3':true" +
                "}," +
                "'internal': {" +
                "   'sendDateTime':123," +
                "   'key': 'value'" +
                "}" +
                "}");

        // workaround start (RemoteMessage is final, cannot mock)
        Bundle bundle = new Bundle();
        bundle.putString("org_ib_d", ibData);
        RemoteMessage remoteMessage = new RemoteMessage(bundle);
        // workaround end

        Message message = new FirebaseMessageMapper().createMessage(remoteMessage);

        assertEquals("messageId", message.getMessageId());
        assertEquals("text", message.getBody());
        assertEquals("icon", message.getIcon());
        assertEquals("title", message.getTitle());
        assertEquals("sound", message.getSound());
        assertEquals("category", message.getCategory());
        assertEquals(false, message.isVibrate());
        assertEquals(true, message.isSilent());
        assertEquals("contentUrl", message.getContentUrl());
        assertEquals("value", message.getCustomPayload().optString("key"));
        assertEquals(1, message.getCustomPayload().optInt("key2"));
        assertEquals(true, message.getCustomPayload().optBoolean("key3"));
        assertEquals(123L, message.getSentTimestamp());
        assertEquals("{\"sendDateTime\":123,\"key\":\"value\"}", message.getInternalData());
        assertEquals(Message.InAppStyle.MODAL, message.getInAppStyle());
    }

    public void test_shouldMapInAppStyleStringInRemoteMessage() {

        final String ibData = dqjson("{" +
                "'messageId':'messageId'," +
                "'text':'text'," +
                "'notification': {" +
                "   'inAppStyle':'BANNER'" +
                "}" +
                "}");

        // workaround start (RemoteMessage is final, cannot mock)
        Bundle bundle = new Bundle();
        bundle.putString("org_ib_d", ibData);
        RemoteMessage remoteMessage = new RemoteMessage(bundle);
        // workaround end

        Message message = new FirebaseMessageMapper().createMessage(remoteMessage);

        assertEquals("messageId", message.getMessageId());
        assertEquals("text", message.getBody());
        assertEquals(Message.InAppStyle.BANNER, message.getInAppStyle());
    }

    private static String dqjson(String sqjson) {
        return sqjson.replace("'", "\"");
    }

}
