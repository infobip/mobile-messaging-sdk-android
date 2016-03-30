package org.infobip.mobile.messaging;

import android.os.Bundle;
import android.os.Parcel;
import junit.framework.TestCase;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

import java.util.HashMap;

/**
 * @author mstipanov
 * @since 30.03.2016.
 */
public class MessageTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test_toJSON_success() throws Exception {
        Message message = new Message(new Bundle());
        message.setMessageId("1234");
        message.setSound("some sound");
        message.setBody("lala");
        message.setTitle("title");
        message.setFrom("from");
        message.setIcon("some icon");
        message.setSilent(true);

        Bundle data = new Bundle();
        message.setData(data);
        Bundle notification = new Bundle();
        notification.putString("messageId", "1234");
        notification.putString("body", "lala");
        notification.putString("title", "title");
        notification.putShortArray("array", new short[]{1, 2, 3});
        data.putBundle("notification", notification);

        assertEquals("{\"messageId\":\"1234\",\"silent\":true,\"body\":\"lala\",\"data\":{\"notification\":{\"messageId\":\"1234\",\"body\":\"lala\",\"array\":[1,2,3],\"title\":\"title\"}},\"from\":\"from\",\"icon\":\"some icon\",\"sound\":\"some sound\",\"title\":\"title\"}",
                new JsonSerializer().serialize(message.getBundle()));
    }

    public void test_fromJSON_success() throws Exception {
        String json = "{\"messageId\":\"1234\",\"silent\":true,\"body\":\"lala\",\"data\":{\"notification\":{\"messageId\":\"1234\",\"body\":\"lala\",\"array\":[1,2,3],\"title\":\"title\"}},\"from\":\"from\",\"icon\":\"some icon\",\"sound\":\"some sound\",\"title\":\"title\"}";
        HashMap map = new JsonSerializer().deserialize(json, HashMap.class);

        Bundle bundle = new Bundle();
        Parcel parcel = Parcel.obtain();
        parcel.readMap(map, JsonSerializer.class.getClassLoader());
        Bundle.CREATOR.createFromParcel(parcel);

        Message message = new Message(bundle);
//        message.setMessageId("1234");
//        message.setSound("some sound");
//        message.setBody("lala");
//        message.setTitle("title");
//        message.setFrom("from");
//        message.setIcon("some icon");
//        message.setSilent(true);
//
//        Bundle data = new Bundle();
//        message.setData(data);
//        Bundle notification = new Bundle();
//        notification.putString("messageId", "1234");
//        notification.putString("body", "lala");
//        notification.putString("title", "title");
//        notification.putShortArray("array", new short[]{1, 2, 3});
//        data.putBundle("notification", notification);

        assertEquals("1234", message.getMessageId());
    }
}
