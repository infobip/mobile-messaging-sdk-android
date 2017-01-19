package org.infobip.mobile.messaging;

import android.os.Bundle;

import junit.framework.TestCase;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.bundle.BundleMessageMapper;
import org.infobip.mobile.messaging.geo.Geo;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Date;

import static org.junit.Assert.assertNotEquals;

/**
 * @author mstipanov
 * @since 30.03.2016.
 */
public class MessageTest extends TestCase {

    private class GeoTest extends Geo {
        GeoTest() {
            super(null, null, null);
        }

        Date getExpiry() {
            return super.getExpiryDate();
        }
    }

    public void test_toBundle_success() throws Exception {

        Message message = Message.createFrom(new Bundle());
        message.setTitle("lala");
        message.setFrom("from");

        Bundle plainBundle = BundleMessageMapper.toBundle(message);
        assertEquals(plainBundle.getString("gcm.notification.title"), "lala");
        assertEquals(plainBundle.getString("from"), "from");
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

        Message message = Message.createFrom(plainBundle);

        assertEquals(message.getMessageId(), "1234");
        assertEquals(message.getSound(), "some sound");
        assertEquals(message.getBody(), "lala");
        assertEquals(message.getTitle(), "title");
        assertEquals(message.getFrom(), "from");
        assertEquals(message.getIcon(), "some icon");
        assertEquals(message.isSilent(), false);
    }

    public void test_customData() throws Exception {
        String customPayload =
        "{" +
            "\"key1\":\"value1\"," +
            "\"key2\":\"value2\"," +
            "\"key3\":\"value3\"" +
        "}";

        Bundle bundle = new Bundle();
        bundle.putString("customPayload", customPayload);

        Message message = Message.createFrom(bundle);

        JSONAssert.assertEquals(customPayload, message.getCustomPayload(), true);
    }

    public void test_customDataWithGcmKeys() throws Exception {

        String customPayload =
        "{" +
            "\"key1\":\"value1\"," +
            "\"key2\":\"value2\"," +
            "\"key3\":\"value3\"" +
        "}";

        Bundle bundle = new Bundle();
        bundle.putString("customPayload", customPayload);
        bundle.putString("gcm.notification.e", "1");
        bundle.putString("gcm.notification.messageId", "L+auqUQFlo1yqk2TEakxua/4rc6DmGgm7cM6G0GRQjU=");
        bundle.putString("gcm.notification.sound", "default");
        bundle.putString("gcm.notification.title", "notification title sender");
        bundle.putString("gcm.notification.body", "push text");
        bundle.putString("android.support.content.wakelockid", "11");
        bundle.putString("collapse_key", "org.infobip.mobile.messaging.showcase.dev");

        Message message = Message.createFrom(bundle);

        JSONAssert.assertEquals(customPayload, message.getCustomPayload(), true);
    }

    public void test_silentMessage_fromJson_withSilentData() throws Exception {

        String internalData =
        "{" +
            "\"silent\":" +
            "{" +
                "\"title\":\"silentTitle\"," +
                "\"body\":\"silentBody\"," +
                "\"sound\":\"silentSound\"," +
                "\"category\":\"silentCategory\"" +
            "}" +
        "}";

        Bundle bundle = new Bundle();
        bundle.putString("gcm.notification.e", "1");
        bundle.putString("gcm.notification.messageId", "L+auqUQFlo1yqk2TEakxua/4rc6DmGgm7cM6G0GRQjU=");
        bundle.putString("android.support.content.wakelockid", "11");
        bundle.putString("collapse_key", "org.infobip.mobile.messaging.showcase.dev");
        bundle.putString("internalData", internalData);
        bundle.putString("gcm.notification.silent", "true");

        Message message = Message.createFrom(bundle);

        assertEquals("silentTitle", message.getTitle());
        assertEquals("silentBody", message.getBody());
        assertEquals("silentSound", message.getSound());
        assertEquals("silentCategory", message.getCategory());
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

        Message message = Message.createFrom(bundle);

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
                "\"sound\":\"silentSound\"," +
                "\"category\":\"silentCategory\"" +
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
        bundle.putString("gcm.notification.category", "notSilentCategory");
        bundle.putString("internalData", internalData);

        Message message = Message.createFrom(bundle);

        assertEquals("notSilentTitle", message.getTitle());
        assertEquals("notSilentBody", message.getBody());
        assertEquals("notSilentSound", message.getSound());
        assertEquals("notSilentCategory", message.getCategory());
    }

    public void test_normalMessage_fromJson_withoutNormalData() throws Exception {

        String internalData =
        "{" +
            "\"silent\":" +
            "{" +
                "\"title\":\"silentTitle\"," +
                "\"body\":\"silentBody\"," +
                "\"sound\":\"silentSound\"," +
                "\"category\":\"silentCategory\"" +
            "}" +
        "}";

        Bundle bundle = new Bundle();
        bundle.putString("gcm.notification.e", "1");
        bundle.putString("gcm.notification.messageId", "L+auqUQFlo1yqk2TEakxua/4rc6DmGgm7cM6G0GRQjU=");
        bundle.putString("android.support.content.wakelockid", "11");
        bundle.putString("collapse_key", "org.infobip.mobile.messaging.showcase.dev");
        bundle.putString("internalData", internalData);

        Message message = Message.createFrom(bundle);

        assertNotEquals("silentTitle", message.getTitle());
        assertNotEquals("silentBody", message.getBody());
        assertNotEquals("silentSound", message.getSound());
        assertNotEquals("silentCategory", message.getCategory());
    }

    public void test_geofence_expiryTime() throws Exception {
        String geofence =
        "{" +
            "\"expiryTime\":\"2016-08-06T12:20:16+03:00\"" +
        "}";

        GeoTest geo = new JsonSerializer().deserialize(geofence, GeoTest.class);

        assertNotNull(geo.getExpiry());
        assertNotEquals(0L, geo.getExpiry().getTime());

        geofence =
        "{" +
            "\"expiryTime\":\"2016-12-06T13:20:16+0300\"" +
        "}";

        geo = new JsonSerializer().deserialize(geofence, GeoTest.class);

        assertNotNull(geo.getExpiry());
        assertNotEquals(0L, geo.getExpiry().getTime());

        geofence =
        "{" +
            "\"expiryTime\":\"2016-08-31T14:20:16+03\"" +
        "}";

        geo = new JsonSerializer().deserialize(geofence, GeoTest.class);

        assertNotNull(geo.getExpiry());
        assertNotEquals(0L, geo.getExpiry().getTime());

        geofence =
        "{" +
            "\"expiryTime\":\"2016-08-31T14:20:16Z\"" +
        "}";

        geo = new JsonSerializer().deserialize(geofence, GeoTest.class);

        assertNotNull(geo.getExpiry());
        assertNotEquals(0L, geo.getExpiry().getTime());
    }
}
