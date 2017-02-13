package org.infobip.mobile.messaging.dal.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.Message;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

/**
 * @author sslavin
 * @since 02/02/2017.
 */

public class SqliteMessageMigrationTest extends InstrumentationTestCase {

    private Context context;

    private static final int OLD_DB_VERSION = 1; // See database versions in DatabaseHelperImpl
    private static final String SQL_CREATE_OLD_TABLE = "CREATE TABLE " + DatabaseContract.Tables.MESSAGES + " (" +
            DatabaseContract.MessageColumns.MESSAGE_ID + " TEXT PRIMARY KEY NOT NULL ON CONFLICT FAIL, " +
            DatabaseContract.MessageColumns.TITLE + " TEXT, " +
            DatabaseContract.MessageColumns.BODY + " TEXT, " +
            DatabaseContract.MessageColumns.SOUND + " TEXT, " +
            DatabaseContract.MessageColumns.VIBRATE + " INTEGER NOT NULL DEFAULT 1, " +
            DatabaseContract.MessageColumns.ICON + " TEXT, " +
            DatabaseContract.MessageColumns.SILENT + " INTEGER NOT NULL DEFAULT 0, " +
            DatabaseContract.MessageColumns.CATEGORY + " TEXT, " +
            DatabaseContract.MessageColumns.FROM + " TEXT, " +
            DatabaseContract.MessageColumns.RECEIVED_TIMESTAMP + " INTEGER, " +
            DatabaseContract.MessageColumns.SEEN_TIMESTAMP + " INTEGER, " +
            DatabaseContract.MessageColumns.Deprecated.INTERNAL_DATA + " TEXT, " +
            DatabaseContract.MessageColumns.CUSTOM_PAYLOAD + " TEXT, " +
            DatabaseContract.MessageColumns.DESTINATION + " TEXT, " +
            DatabaseContract.MessageColumns.STATUS + " TEXT," +
            DatabaseContract.MessageColumns.STATUS_MESSAGE + " TEXT)";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext();
        context.deleteDatabase(DatabaseHelperImpl.DATABASE_NAME);
    }

    public void test_shouldMigrateMessages() throws Exception {
        // Create SQLiteOpenHelper directly to perform raw operations on database
        SQLiteOpenHelper sqLiteOpenHelper = new SQLiteOpenHelper(context, DatabaseHelperImpl.DATABASE_NAME, null, OLD_DB_VERSION) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(SQL_CREATE_OLD_TABLE);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }
        };

        // Create content values for old message structure
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseContract.MessageColumns.MESSAGE_ID, "SomeMessageId");
        contentValues.put(DatabaseContract.MessageColumns.TITLE, "SomeMessageTitle");
        contentValues.put(DatabaseContract.MessageColumns.BODY, "SomeMessageBody");
        contentValues.put(DatabaseContract.MessageColumns.SOUND, "SomeMessageSound");
        contentValues.put(DatabaseContract.MessageColumns.VIBRATE, 1);
        contentValues.put(DatabaseContract.MessageColumns.ICON, "SomeMessageIcon");
        contentValues.put(DatabaseContract.MessageColumns.SILENT, 1);
        contentValues.put(DatabaseContract.MessageColumns.CATEGORY, "SomeMessageCategory");
        contentValues.put(DatabaseContract.MessageColumns.FROM, "SomeMessageFrom");
        contentValues.put(DatabaseContract.MessageColumns.RECEIVED_TIMESTAMP, 1234L);
        contentValues.put(DatabaseContract.MessageColumns.SEEN_TIMESTAMP, 5678L);
        contentValues.put(DatabaseContract.MessageColumns.Deprecated.INTERNAL_DATA,
                "{" +
                        " 'geo': [" +
                        "  {" +
                        "   'radiusInMeters': 11000," +
                        "   'latitude': 59.95," +
                        "   'id': 'E763E42AAB76E5AE7CB67C7AFA63107D'," +
                        "   'title': 'Saint Petersburg'," +
                        "   'favorite': false," +
                        "   'longitude': 30.3" +
                        "  }" +
                        " ]," +
                        " 'silent': {" +
                        "  'sound': 'default'," +
                        "  'vibrate': true," +
                        "  'body': 'geo test'" +
                        " }," +
                        " 'messageType': 'geo'," +
                        " 'campaignId': '37446'," +
                        " 'expiryTime': '2017-02-10T00:00:00+00:00'," +
                        " 'startTime': '2017-02-02T17:00:00+00:00'" +
                        "}");
        contentValues.put(DatabaseContract.MessageColumns.CUSTOM_PAYLOAD,
                "{" +
                        "  'stringValue':'SomeString'," +
                        "  'numberValue':1," +
                        "  'booleanValue':true" +
                        "}");
        contentValues.put(DatabaseContract.MessageColumns.DESTINATION, "SomeMessageDestination");
        contentValues.put(DatabaseContract.MessageColumns.STATUS, Message.Status.SUCCESS.name());
        contentValues.put(DatabaseContract.MessageColumns.STATUS_MESSAGE, "SomeStatusMessage");

        // Get writable
        SQLiteDatabase db = sqLiteOpenHelper.getWritableDatabase();
        db.insert(DatabaseContract.Tables.MESSAGES, null, contentValues);
        db.close();

        // Now create Database Helper
        // upgrade shall be performed internally and we shall load new message
        DatabaseHelper databaseHelper = new DatabaseHelperImpl(context);
        List<SqliteMessage> messages = databaseHelper.findAll(SqliteMessage.class);

        // Assert
        assertEquals(1, messages.size());
        assertEquals("SomeMessageId", messages.get(0).getMessageId());
        assertEquals("SomeMessageTitle", messages.get(0).getTitle());
        assertEquals("SomeMessageBody", messages.get(0).getBody());
        assertEquals("SomeMessageSound", messages.get(0).getSound());
        assertEquals(true, messages.get(0).isVibrate());
        assertEquals("SomeMessageIcon", messages.get(0).getIcon());
        assertEquals(true, messages.get(0).isSilent());
        assertEquals("SomeMessageCategory", messages.get(0).getCategory());
        assertEquals("SomeMessageFrom", messages.get(0).getFrom());
        assertEquals(1234L, messages.get(0).getReceivedTimestamp());
        assertEquals(5678L, messages.get(0).getSeenTimestamp());
        assertNotNull(messages.get(0).getGeo());
        assertNotNull(messages.get(0).getGeo().getAreasList());
        assertEquals(1, messages.get(0).getGeo().getAreasList().size());
        assertEquals(11000, messages.get(0).getGeo().getAreasList().get(0).getRadius().intValue());
        assertEquals(59.95, messages.get(0).getGeo().getAreasList().get(0).getLatitude(), 0.01);
        assertEquals(30.3, messages.get(0).getGeo().getAreasList().get(0).getLongitude(), 0.01);
        assertEquals("E763E42AAB76E5AE7CB67C7AFA63107D", messages.get(0).getGeo().getAreasList().get(0).getId());
        assertEquals("Saint Petersburg", messages.get(0).getGeo().getAreasList().get(0).getTitle());
        JSONAssert.assertEquals("{" +
                "  'stringValue':'SomeString'," +
                "  'numberValue':1," +
                "  'booleanValue':true" +
                "}", messages.get(0).getCustomPayload(), true);
        assertEquals("SomeMessageDestination", messages.get(0).getDestination());
        assertEquals(Message.Status.SUCCESS, messages.get(0).getStatus());
        assertEquals("SomeStatusMessage", messages.get(0).getStatusMessage());
    }
}