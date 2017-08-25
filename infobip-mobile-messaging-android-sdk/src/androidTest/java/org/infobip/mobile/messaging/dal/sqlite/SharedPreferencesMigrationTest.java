package org.infobip.mobile.messaging.dal.sqlite;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.shaded.google.gson.JsonArray;
import org.infobip.mobile.messaging.api.shaded.google.gson.JsonObject;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * @author sslavin
 * @since 16/01/2017.
 */

public class SharedPreferencesMigrationTest extends MobileMessagingTestCase {

    private SharedPreferencesMessageStore sharedPreferencesMessageStore;
    private JsonSerializer jsonSerializer = new JsonSerializer();

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

        JsonObject attachment = new JsonObject();
        attachment.addProperty("url", "http://www.some-content.com.ru.hr");

        JsonArray attachments = new JsonArray();
        attachments.add(attachment);

        JsonObject internalData = new JsonObject();
        internalData.add("atts", attachments);
        internalData.add("silent", new JsonObject());

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
                    "http://www.some-content.com.ru.hr"
            ));
        }

        MessageStore store = new SQLiteMessageStore();
        List<Message> messages = store.findAll(context);
        HashMap<String, Message> map = new HashMap<>();
        for (Message m : messages) map.put(m.getMessageId(), m);

        // we are not removing messages from shared prefs
        // in case user still uses SharedPreferencesMessageStore
        Assert.assertEquals(numberOfMessages, sharedPreferencesMessageStore.findAll(context).size());
        Assert.assertEquals(numberOfMessages, map.size());

        for (int i = 0; i < numberOfMessages; i++) {
            String id = i + uuid.toString();
            Assert.assertEquals(id, map.get(id).getMessageId());
            Assert.assertEquals("SomeTitle" + i, map.get(id).getTitle());
            Assert.assertEquals("SomeBody" + i, map.get(id).getBody());
            Assert.assertEquals("SomeSound" + i, map.get(id).getSound());
            Assert.assertEquals(true, map.get(id).isVibrate());
            Assert.assertEquals("SomeIcon" + i, map.get(id).getIcon());
            Assert.assertEquals(false, map.get(id).isSilent());
            Assert.assertEquals("SomeCategory" + i, map.get(id).getCategory());
            Assert.assertEquals("SomeFrom" + i, map.get(id).getFrom());
            Assert.assertEquals(0, map.get(id).getReceivedTimestamp());
            Assert.assertEquals(0, map.get(id).getSeenTimestamp());
            Assert.assertEquals(null, map.get(id).getCustomPayload());
            JSONAssert.assertEquals(internalData.toString(), map.get(id).getInternalData(), false);
            Assert.assertEquals("SomeDestination" + i, map.get(id).getDestination());
            Assert.assertEquals(Message.Status.SUCCESS, map.get(id).getStatus());
            Assert.assertEquals("SomeStatusMessage" + i, map.get(id).getStatusMessage());
            Assert.assertEquals("http://www.some-content.com.ru.hr", map.get(id).getContentUrl());
        }
    }
}
