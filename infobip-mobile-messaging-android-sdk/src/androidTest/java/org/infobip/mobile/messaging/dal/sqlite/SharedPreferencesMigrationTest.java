package org.infobip.mobile.messaging.dal.sqlite;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author sslavin
 * @since 16/01/2017.
 */

public class SharedPreferencesMigrationTest extends MobileMessagingTestCase {

    private SharedPreferencesMessageStore sharedPreferencesMessageStore;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        sharedPreferencesMessageStore = new SharedPreferencesMessageStore();
        sharedPreferencesMessageStore.deleteAll(context);
    }

    @Test
    public void test_shouldMigrateAllMessagesToSqlite() throws Exception {

        UUID uuid = UUID.randomUUID();
        int numberOfMessages = 100;

        JSONObject attachment = new JSONObject();
        attachment.put("url", "http://www.some-content.com.ru.hr");

        JSONArray attachments = new JSONArray();
        attachments.put(attachment);

        JSONObject internalData = new JSONObject();
        internalData.put("atts", attachments);
        internalData.put("silent", new JSONObject());

        internalData.put("webViewUrl", "http://www.bla.com");
        internalData.put("browserUrl", "http://www.openinbrowser.com");
        internalData.put("deeplink", "app://deep/link");
        internalData.put("messageType", "some msg type");
        internalData.put("inAppOpenTitle", "in-app open title");
        internalData.put("inAppDismissTitle", "in-app dismiss title");

        for (int i = 0; i < numberOfMessages; i++) {
            sharedPreferencesMessageStore.save(context, new Message(
                    i + uuid.toString(),
                    "SomeTitle" + i,
                    "SomeBody" + i,
                    "SomeSound" + i,
                    true,
                    "SomeIcon" + i,
                    false,
                    "SomeCategory" + i,
                    "SomeFrom" + i,
                    0,
                    0,
                    0,
                    null,
                    internalData.toString(),
                    "SomeDestination" + i,
                    Message.Status.SUCCESS,
                    "SomeStatusMessage" + i,
                    "http://www.some-content.com.ru.hr",
                    null,
                    0,
                    "http://www.bla.com",
                    "http://www.openinbrowser.com",
                    "some msg type",
                    "app://deep/link",
                    "in-app open title",
                    "in-app dismiss title"
            ));
        }

        MessageStore store = new SQLiteMessageStore();
        List<Message> messages = store.findAll(context);
        HashMap<String, Message> map = new HashMap<>();
        for (Message m : messages) map.put(m.getMessageId(), m);

        // we are not removing messages from shared prefs
        // in case user still uses SharedPreferencesMessageStore
        assertEquals(numberOfMessages, sharedPreferencesMessageStore.findAll(context).size());
        assertEquals(numberOfMessages, map.size());

        for (int i = 0; i < numberOfMessages; i++) {
            String id = i + uuid.toString();
            Message message = map.get(id);
            assertEquals(id, message.getMessageId());
            assertEquals("SomeTitle" + i, message.getTitle());
            assertEquals("SomeBody" + i, message.getBody());
            assertEquals("SomeSound" + i, message.getSound());
            assertEquals(true, message.isVibrate());
            assertEquals("SomeIcon" + i, message.getIcon());
            assertEquals(false, message.isSilent());
            assertEquals("SomeCategory" + i, message.getCategory());
            assertEquals("SomeFrom" + i, message.getFrom());
            assertEquals(0, message.getReceivedTimestamp());
            assertEquals(0, message.getSeenTimestamp());
            assertEquals(null, message.getCustomPayload());
            JSONAssert.assertEquals(internalData.toString(), message.getInternalData(), false);
            assertEquals("SomeDestination" + i, message.getDestination());
            assertEquals(Message.Status.SUCCESS, message.getStatus());
            assertEquals("SomeStatusMessage" + i, message.getStatusMessage());
            assertEquals("http://www.some-content.com.ru.hr", message.getContentUrl());
            assertEquals(0, message.getInAppExpiryTimestamp());
            assertEquals("http://www.bla.com", message.getWebViewUrl());
            assertEquals("http://www.openinbrowser.com", message.getBrowserUrl());
            assertEquals("app://deep/link", message.getDeeplink());
            assertEquals("some msg type", message.getMessageType());
            assertEquals("in-app open title", message.getInAppOpenTitle());
            assertEquals("in-app dismiss title", message.getInAppDismissTitle());
        }
    }

    @Test
    public void test_shouldNotMigrateChatMessagesToSqlite() throws Exception {

        UUID uuid = UUID.randomUUID();
        int numberOfMessages = 10;

        JSONObject internalData = new JSONObject();

        internalData.put("webViewUrl", "http://www.bla.com");
        internalData.put("browserUrl", "http://www.openinbrowser.com");
        internalData.put("deeplink", "app://deep/link");
        internalData.put("messageType", "chat");
        internalData.put("inAppOpenTitle", "in-app open title");
        internalData.put("inAppDismissTitle", "in-app dismiss title");

        for (int i = 0; i < numberOfMessages; i++) {
            sharedPreferencesMessageStore.save(context, new Message(
                    i + uuid.toString(),
                    "SomeTitle" + i,
                    "SomeBody" + i,
                    "SomeSound" + i,
                    true,
                    "SomeIcon" + i,
                    false,
                    "SomeCategory" + i,
                    "SomeFrom" + i,
                    0,
                    0,
                    0,
                    null,
                    internalData.toString(),
                    "SomeDestination" + i,
                    Message.Status.SUCCESS,
                    "SomeStatusMessage" + i,
                    "http://www.some-content.com.ru.hr",
                    null,
                    0,
                    "http://www.bla.com",
                    "http://www.openinbrowser.com",
                    "chat",
                    "app://deep/link",
                    "in-app open title",
                    "in-app dismiss title"
            ));
        }

        MessageStore store = new SQLiteMessageStore();
        List<Message> messages = store.findAll(context);
        HashMap<String, Message> map = new HashMap<>();
        for (Message m : messages) map.put(m.getMessageId(), m);

        // we are not removing messages from shared prefs
        // in case user still uses SharedPreferencesMessageStore
        assertEquals(0, sharedPreferencesMessageStore.findAll(context).size());
        assertEquals(0, map.size());

        for (int i = 0; i < numberOfMessages; i++) {
            String id = i + uuid.toString();
            Message message = map.get(id);
            assertNull(message);
        }
    }
}
