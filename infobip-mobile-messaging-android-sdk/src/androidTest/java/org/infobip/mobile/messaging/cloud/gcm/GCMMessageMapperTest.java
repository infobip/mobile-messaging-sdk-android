package org.infobip.mobile.messaging.cloud.gcm;

import android.os.Bundle;

import junit.framework.TestCase;

import org.infobip.mobile.messaging.Message;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.Assert.assertNotEquals;

/**
 * @author sslavin
 * @since 29/12/2016.
 */

public class GCMMessageMapperTest extends TestCase {

    public void test_message_should_be_possible_to_construct_message_from_bundle() throws Exception {
        // Given
        Bundle bundle = new Bundle();
        bundle.putString("gcm.notification.messageId", "SomeMessageId");
        bundle.putString("gcm.notification.title", "SomeMessageTitle");
        bundle.putString("gcm.notification.body", "SomeMessageBody");
        bundle.putString("gcm.notification.sound", "SomeMessageSound");
        bundle.putString("gcm.notification.sound2", "SomeMessageSound");
        bundle.putString("gcm.notification.vibrate", "true");
        bundle.putString("gcm.notification.icon", "SomeMessageIcon");
        bundle.putString("gcm.notification.silent", "false");
        bundle.putString("gcm.notification.category", "SomeMessageCategory");
        bundle.putString("from", "SomeMessageFrom");
        bundle.putLong("received_timestamp", 1234L);
        bundle.putLong("seen_timestamp", 5678L);
        bundle.putString("internalData", "{}");
        bundle.putString("customPayload", "{}");
        bundle.putString("destination", "SomeDestination");
        bundle.putString("status", "SUCCESS");
        bundle.putString("status_message", "SomeStatusMessage");

        // When
        Message message = GCMMessageMapper.fromCloudBundle(bundle);

        // Then
        assertEquals("SomeMessageId", message.getMessageId());
        assertEquals("SomeMessageTitle", message.getTitle());
        assertEquals("SomeMessageBody", message.getBody());
        assertEquals("SomeMessageSound", message.getSound());
        assertEquals(true, message.isVibrate());
        assertEquals("SomeMessageIcon", message.getIcon());
        assertEquals(false, message.isSilent());
        assertEquals("SomeMessageCategory", message.getCategory());
        assertEquals("SomeMessageFrom", message.getFrom());
        assertEquals(1234L, message.getReceivedTimestamp());
        assertEquals(5678L, message.getSeenTimestamp());
        assertEquals(0, message.getCustomPayload().length());
        assertEquals("SomeDestination", message.getDestination());
        assertEquals(Message.Status.SUCCESS, message.getStatus());
        assertEquals("SomeStatusMessage", message.getStatusMessage());
    }

    public void test_toBundle_success() throws Exception {

        Message message = new Message();
        message.setTitle("lala");
        message.setFrom("from");

        Bundle plainBundle = GCMMessageMapper.toCloudBundle(message);
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

        Message message = GCMMessageMapper.fromCloudBundle(plainBundle);

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

        Message message = GCMMessageMapper.fromCloudBundle(bundle);

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

        Message message = GCMMessageMapper.fromCloudBundle(bundle);

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

        Message message = GCMMessageMapper.fromCloudBundle(bundle);

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

        Message message = GCMMessageMapper.fromCloudBundle(bundle);

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
                        "}," +
                        "\"inApp\":false" +
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

        Message message = GCMMessageMapper.fromCloudBundle(bundle);

        assertEquals("notSilentTitle", message.getTitle());
        assertEquals("notSilentBody", message.getBody());
        assertEquals("notSilentSound", message.getSound());
        assertEquals("notSilentCategory", message.getCategory());
        assertNull(message.getInAppStyle());
    }

    public void test_inApp_mapping_fromJson_to_message_inAppStyle() throws Exception {

        String internalData =
                "{" +
                        "\"inApp\":true" +
                        "}";

        Bundle bundle = new Bundle();
        bundle.putString("internalData", internalData);

        Message message = GCMMessageMapper.fromCloudBundle(bundle);

        assertEquals(Message.InAppStyle.MODAL, message.getInAppStyle());
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

        Message message = GCMMessageMapper.fromCloudBundle(bundle);

        assertNotEquals("silentTitle", message.getTitle());
        assertNotEquals("silentBody", message.getBody());
        assertNotEquals("silentSound", message.getSound());
        assertNotEquals("silentCategory", message.getCategory());
    }

    public void test_bundleWithAttachments_intoMessageWithContentUrl() throws Exception {

        String internalData =
                "{" +
                        "\"atts\" : [{" +
                            "\"url\":\"someUrl\"" +
                        "}]" +
                        "}";

        Bundle bundle = new Bundle();
        bundle.putString("internalData", internalData);

        Message message = GCMMessageMapper.fromCloudBundle(bundle);

        assertEquals("someUrl", message.getContentUrl());
    }

    public void test_firstAttachment_shouldMapIntoContentUrl_whenMultipleAttachments() throws Exception {

        String internalData =
                "{" +
                    "\"atts\" : [" +
                        "{" +
                            "\"url\":\"someUrl1\"" +
                        "}," +
                        "{" +
                            "\"url\":\"someUrl2\"," +
                            "\"t\":\"someType2\"" +
                        "}," +
                        "{" +
                            "\"url\":\"someUrl3\"," +
                            "\"t\":\"someType3\"" +
                        "}," +
                    "]" +
                "}";

        Bundle bundle = new Bundle();
        bundle.putString("internalData", internalData);

        Message message = GCMMessageMapper.fromCloudBundle(bundle);

        assertEquals("someUrl1", message.getContentUrl());
    }
}
