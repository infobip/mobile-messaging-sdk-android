package org.infobip.mobile.messaging;

import android.os.Bundle;

import junit.framework.TestCase;

import org.infobip.mobile.messaging.api.shaded.google.gson.JsonObject;
import org.infobip.mobile.messaging.api.shaded.google.gson.JsonParser;

import java.util.Set;

import static org.junit.Assert.*;

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

    public void test_toBundle_success() throws Exception {

        Bundle notification = new Bundle();
        notification.putShortArray("array", new short[]{1, 2, 3});

        Bundle data = new Bundle();
        data.putBundle("notification", notification);

        Message message = new Message(new Bundle());
        message.setMessageId("1234");
        message.setSound("some sound");
        message.setBody("lala");
        message.setTitle("title");
        message.setFrom("from");
        message.setIcon("some icon");
        message.setSilent(true);
        message.setData(data);

        Bundle plainBundle = message.getBundle();
        assertEquals(plainBundle.getString("gcm.notification.messageId"), "1234");
        assertEquals(plainBundle.getString("gcm.notification.sound"), "some sound");
        assertEquals(plainBundle.getString("gcm.notification.body"), "lala");
        assertEquals(plainBundle.getString("gcm.notification.title"), "title");
        assertEquals(plainBundle.getString("from"), "from");
        assertEquals(plainBundle.getString("gcm.notification.icon"), "some icon");
        assertEquals(plainBundle.getString("gcm.notification.silent"), "true");
        assertEquals(plainBundle.getBundle("data").getBundle("notification").getShortArray("array")[2], 3);
    }

    public void test_fromBundle_success() throws Exception {

        Bundle plainBundle = new Bundle();
        plainBundle.putString("gcm.notification.messageId", "1234");
        plainBundle.putString("gcm.notification.sound", "some sound");
        plainBundle.putString("gcm.notification.body", "lala");
        plainBundle.putString("gcm.notification.title", "title");
        plainBundle.putString("from", "from");
        plainBundle.putString("gcm.notification.icon", "some icon");
        plainBundle.putString("gcm.notification.silent", "false");

        Message message = Message.copyFrom(plainBundle);

        assertEquals(message.getMessageId(), "1234");
        assertEquals(message.getSound(), "some sound");
        assertEquals(message.getBody(), "lala");
        assertEquals(message.getTitle(), "title");
        assertEquals(message.getFrom(), "from");
        assertEquals(message.getIcon(), "some icon");
        assertEquals(message.isSilent(), false);
    }

    public void test_customData() throws Exception {
        Message message = new Message(new Bundle());

        Bundle dataBundle = new Bundle();
        dataBundle.putString("mykey", "mickey");
        dataBundle.putString("mykey2", "mickey2");
        dataBundle.putString("mykey3", "mickey3");
        dataBundle.putString("mykey4", "mickey4");
        dataBundle.putString("mykey5", "mickey5");

        message.setData(dataBundle);

        assertTrue(equalBundles(message.getCustomData(), dataBundle));
    }

    public void test_customDataWithGcmKeys() throws Exception {
        Message message = new Message(new Bundle());

        Bundle dataBundle = new Bundle();
        dataBundle.putString("mykey1", "mickey1");
        dataBundle.putString("mykey2", "mickey2");
        dataBundle.putString("mykey3", "mickey3");
        dataBundle.putString("mykey4", "mickey4");
        dataBundle.putString("gcm.notification.e", "1");
        dataBundle.putString("gcm.notification.messageId", "L+auqUQFlo1yqk2TEakxua/4rc6DmGgm7cM6G0GRQjU=");
        dataBundle.putString("gcm.notification.sound", "default");
        dataBundle.putString("gcm.notification.title", "notification title sender");
        dataBundle.putString("gcm.notification.body", "push text");
        dataBundle.putString("android.support.content.wakelockid", "11");
        dataBundle.putString("collapse_key", "org.infobip.mobile.messaging.showcase.dev");

        message.setData(dataBundle);

        Bundle customData = message.getCustomData();

        assertFalse(equalBundles(customData, dataBundle));
        assertTrue(dataBundle.keySet().size() == 11);
        assertTrue(customData.keySet().size() == 4);
        assertFalse(customData.containsKey("android.support.content.wakelockid"));
        assertTrue(customData.containsKey("mykey4"));
    }

    public void test_silentMessage_fromJson_withSilentData() throws Exception {

        String internalData =
        "{" +
            "\"silent\":" +
            "{" +
                "\"title\":\"silentTitle\"," +
                "\"body\":\"silentBody\"," +
                "\"sound\":\"silentSound\"" +
            "}" +
        "}";

        Bundle bundle = new Bundle();
        bundle.putString("gcm.notification.e", "1");
        bundle.putString("gcm.notification.messageId", "L+auqUQFlo1yqk2TEakxua/4rc6DmGgm7cM6G0GRQjU=");
        bundle.putString("android.support.content.wakelockid", "11");
        bundle.putString("collapse_key", "org.infobip.mobile.messaging.showcase.dev");
        bundle.putString("internalData", internalData);
        bundle.putString("gcm.notification.silent", "true");

        Message message = Message.copyFrom(bundle);

        assertEquals("silentTitle", message.getTitle());
        assertEquals("silentBody", message.getBody());
        assertEquals("silentSound", message.getSound());
    }

    public void test_silentMessage_fromJson_withoutSilentData() throws Exception {

        Bundle bundle = new Bundle();
        bundle.putString("gcm.notification.e", "1");
        bundle.putString("gcm.notification.messageId", "L+auqUQFlo1yqk2TEakxua/4rc6DmGgm7cM6G0GRQjU=");
        bundle.putString("android.support.content.wakelockid", "11");
        bundle.putString("collapse_key", "org.infobip.mobile.messaging.showcase.dev");
        bundle.putString("gcm.notification.silent", "true");
        bundle.putString("gcm.notification.title", "notSilentTitle");
        bundle.putString("gcm.notification.body", "notSilentBody");
        bundle.putString("gcm.notification.sound", "notSilentSound");

        Message message = Message.copyFrom(bundle);

        assertNotEquals("notSilentTitle", message.getTitle());
        assertNotEquals("notSilentBody", message.getBody());
        assertNotEquals("notSilentSound", message.getSound());
    }

    public void test_normalMessage_fromJson_withNormalData() throws Exception {

        String internalData =
        "{" +
            "\"silent\":" +
            "{" +
                "\"title\":\"silentTitle\"," +
                "\"body\":\"silentBody\"," +
                "\"sound\":\"silentSound\"" +
            "}" +
        "}";

        Bundle bundle = new Bundle();
        bundle.putString("gcm.notification.e", "1");
        bundle.putString("gcm.notification.messageId", "L+auqUQFlo1yqk2TEakxua/4rc6DmGgm7cM6G0GRQjU=");
        bundle.putString("android.support.content.wakelockid", "11");
        bundle.putString("collapse_key", "org.infobip.mobile.messaging.showcase.dev");
        bundle.putString("gcm.notification.title", "notSilentTitle");
        bundle.putString("gcm.notification.body", "notSilentBody");
        bundle.putString("gcm.notification.sound", "notSilentSound");
        bundle.putString("internalData", internalData);

        Message message = Message.copyFrom(bundle);

        assertEquals("notSilentTitle", message.getTitle());
        assertEquals("notSilentBody", message.getBody());
        assertEquals("notSilentSound", message.getSound());
    }

    public void test_normalMessage_fromJson_withoutNormalData() throws Exception {

        String internalData =
        "{" +
            "\"silent\":" +
            "{" +
                "\"title\":\"silentTitle\"," +
                "\"body\":\"silentBody\"," +
                "\"sound\":\"silentSound\"" +
            "}" +
        "}";

        Bundle bundle = new Bundle();
        bundle.putString("gcm.notification.e", "1");
        bundle.putString("gcm.notification.messageId", "L+auqUQFlo1yqk2TEakxua/4rc6DmGgm7cM6G0GRQjU=");
        bundle.putString("android.support.content.wakelockid", "11");
        bundle.putString("collapse_key", "org.infobip.mobile.messaging.showcase.dev");
        bundle.putString("internalData", internalData);

        Message message = Message.copyFrom(bundle);

        assertNotEquals("silentTitle", message.getTitle());
        assertNotEquals("silentBody", message.getBody());
        assertNotEquals("silentSound", message.getSound());
    }

    public void test_silentMessage_withSilentData_toJson() throws Exception {
        Message message = new Message(new Bundle());
        message.setSilent(true);
        message.setTitle("silentTitle");
        message.setBody("silentBody");
        message.setSound("silentSound");

        Bundle bundle = message.getBundle();

        assertNotNull(message.getInternalData());
        assertNotNull(bundle.getString("internalData"));

        JsonParser parser = new JsonParser();
        JsonObject silentData = parser.parse(bundle.getString("internalData")).getAsJsonObject().getAsJsonObject("silent");

        assertEquals("silentTitle", silentData.get("title").getAsString());
        assertEquals("silentBody", silentData.get("body").getAsString());
        assertEquals("silentSound", silentData.get("sound").getAsString());
    }

    public void test_normalMessage_withSilentData_toJson() throws Exception {
        Message message = new Message(new Bundle());
        message.setTitle("normalTitle");
        message.setBody("normalBody");
        message.setSound("normalSound");

        Bundle bundle = message.getBundle();

        assertNull(message.getInternalData());
        assertNull(bundle.getString("internalData"));
        assertEquals("normalTitle", bundle.getString("gcm.notification.title"));
        assertEquals("normalBody", bundle.getString("gcm.notification.body"));
        assertEquals("normalSound", bundle.getString("gcm.notification.sound"));
    }

    private boolean equalBundles(Bundle one, Bundle two) {
        if (one.size() != two.size())
            return false;

        Set<String> setOne = one.keySet();
        Object valueOne;
        Object valueTwo;

        for (String key : setOne) {
            valueOne = one.get(key);
            valueTwo = two.get(key);
            if (valueOne instanceof Bundle && valueTwo instanceof Bundle &&
                    !equalBundles((Bundle) valueOne, (Bundle) valueTwo)) {
                return false;
            } else if (valueOne == null) {
                if (valueTwo != null || !two.containsKey(key))
                    return false;
            } else if (!valueOne.equals(valueTwo))
                return false;
        }

        return true;
    }
}
