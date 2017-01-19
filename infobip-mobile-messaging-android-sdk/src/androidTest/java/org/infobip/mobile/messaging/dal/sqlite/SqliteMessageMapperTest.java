package org.infobip.mobile.messaging.dal.sqlite;

import android.content.Context;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

/**
 * @author sslavin
 * @since 12/01/2017.
 */

public class SqliteMessageMapperTest extends InstrumentationTestCase {

    private DatabaseHelper databaseHelper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Context context = getInstrumentation().getContext();
        databaseHelper = MobileMessagingCore.getDatabaseHelper(context);
        databaseHelper.deleteAll(SqliteMessageMapper.class);
    }

    public void test_message_toFromSqlite() throws Exception {

        Message message = new Message(
                "SomeMessageId",
                "SomeTitle",
                "SomeBody",
                "SomeSound",
                true,
                "SomeIcon",
                false,
                "SomeCategory",
                "SomeFrom",
                1234L,
                5678L,
                new JSONObject() {{
                    put("stringValue", "StringValue1");
                    put("numberValue", 1);
                    put("booleanValue", false);
                }},
                new JSONObject() {{
                    put("stringValue", "StringValue2");
                    put("numberValue", 2);
                    put("booleanValue", true);
                }},
                null,
                "SomeDestination",
                Message.Status.SUCCESS,
                "SomeStatusMessage"
        );

        databaseHelper.save(new SqliteMessageMapper(message));

        List<SqliteMessageMapper> messages = databaseHelper.findAll(SqliteMessageMapper.class);
        assertEquals(1, messages.size());

        Message m = messages.get(0);
        assertNotSame(message, m);
        assertEquals("SomeMessageId", m.getMessageId());
        assertEquals("SomeTitle", m.getTitle());
        assertEquals("SomeBody", m.getBody());
        assertEquals("SomeSound", m.getSound());
        assertEquals(true, m.isVibrate());
        assertEquals("SomeIcon", m.getIcon());
        assertEquals(1234L, m.getReceivedTimestamp());
        assertEquals(5678L, m.getSeenTimestamp());
        JSONAssert.assertEquals("{" +
                "'stringValue': 'StringValue1'," +
                "'numberValue': 1," +
                "'booleanValue':false" +
        "}", m.getInternalData(), true);
        JSONAssert.assertEquals("{" +
                "'stringValue': 'StringValue2'," +
                "'numberValue': 2," +
                "'booleanValue':true" +
                "}", m.getCustomPayload(), true);
        assertEquals("SomeDestination", message.getDestination());
        assertEquals(Message.Status.SUCCESS, message.getStatus());
        assertEquals("SomeStatusMessage", message.getStatusMessage());
    }

    public void test_message_shouldNotSaveMessageWithNullId() throws Exception {
        Message message = new Message();
        message.setMessageId(null);

        long countBefore = databaseHelper.countAll(SqliteMessageMapper.class);

        databaseHelper.save(new SqliteMessageMapper(message));

        assertEquals(countBefore, databaseHelper.countAll(SqliteMessageMapper.class));
    }
}