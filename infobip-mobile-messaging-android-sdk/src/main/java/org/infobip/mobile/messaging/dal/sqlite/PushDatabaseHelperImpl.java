package org.infobip.mobile.messaging.dal.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract.MessageColumns;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract.Tables;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sslavin
 * @since 12/01/2017.
 */

public class PushDatabaseHelperImpl extends BaseDatabaseHelper {

    static final int VER_2017_JAN_12 = 1; // Initial version
    static final int VER_2017_FEB_14 = 2; // Added separate table for geo messages
    static final int VER_2017_MAY_15 = 3; // Added "content_url" column to messages/geo_messages table
    static final int VER_2017_AUG_25 = 4; // Added "sendDateTime" to internal data (must be present for all messages)
    static final int VER_2019_JAN_21 = 5; // Added "inAppStyle" to internal data
    private static final int VER_CURRENT = VER_2019_JAN_21;

    @SuppressWarnings("WeakerAccess")
    static final String DATABASE_NAME = "mm_infobip_database.db";

    private static final String SQL_CREATE_MESSAGES_TABLE = "CREATE TABLE " + Tables.MESSAGES + " (" +
            MessageColumns.MESSAGE_ID + " TEXT PRIMARY KEY NOT NULL ON CONFLICT FAIL, " +
            MessageColumns.TITLE + " TEXT, " +
            MessageColumns.BODY + " TEXT, " +
            MessageColumns.SOUND + " TEXT, " +
            MessageColumns.VIBRATE + " INTEGER NOT NULL DEFAULT 1, " +
            MessageColumns.ICON + " TEXT, " +
            MessageColumns.SILENT + " INTEGER NOT NULL DEFAULT 0, " +
            MessageColumns.CATEGORY + " TEXT, " +
            MessageColumns.FROM + " TEXT, " +
            MessageColumns.RECEIVED_TIMESTAMP + " INTEGER, " +
            MessageColumns.SEEN_TIMESTAMP + " INTEGER, " +
            MessageColumns.INTERNAL_DATA + " TEXT, " +
            MessageColumns.CUSTOM_PAYLOAD + " TEXT, " +
            MessageColumns.DESTINATION + " TEXT, " +
            MessageColumns.STATUS + " TEXT," +
            MessageColumns.STATUS_MESSAGE + " TEXT)";

    private static final String SQL_ALTER_TABLE_MESSAGES_WITH_CONTENT_URL = "ALTER TABLE "
            + Tables.MESSAGES + " ADD COLUMN " + MessageColumns.CONTENT_URL + " TEXT;";

    private static final String SQL_ALTER_TABLE_MESSAGES_WITH_IN_APP_STYLE = "ALTER TABLE "
            + Tables.MESSAGES + " ADD COLUMN " + MessageColumns.IN_APP_STYLE + " TEXT;";

    public PushDatabaseHelperImpl(Context context) {
        super(context, DATABASE_NAME, VER_CURRENT);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        db.execSQL(SQL_CREATE_MESSAGES_TABLE);
        db.execSQL(SQL_ALTER_TABLE_MESSAGES_WITH_CONTENT_URL);
        db.execSQL(SQL_ALTER_TABLE_MESSAGES_WITH_IN_APP_STYLE);
        db.setTransactionSuccessful();
        db.endTransaction();
        SharedPreferencesMigrator.migrateMessages(context, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int version = oldVersion;
        if (version <= VER_2017_JAN_12) {
            version = VER_2017_FEB_14;
        }

        if (version <= VER_2017_FEB_14) {
            db.execSQL(SQL_ALTER_TABLE_MESSAGES_WITH_CONTENT_URL);
            version = VER_2017_MAY_15;
        }

        if (version <= VER_2017_MAY_15) {
            setSendDateTimeToReceivedTimeIfAbsent(db);
            version = VER_2017_AUG_25;
        }

        if (version <= VER_2017_AUG_25) {
            db.execSQL(SQL_ALTER_TABLE_MESSAGES_WITH_IN_APP_STYLE);
            version = VER_2019_JAN_21;
        }

        if (version != VER_CURRENT) {
            MobileMessagingLogger.w("SQLite DB version is not what expected: " + VER_CURRENT);
        }
    }

    private void setSendDateTimeToReceivedTimeIfAbsent(SQLiteDatabase db) {
        // Read existing data from database
        class Message {
            private String id;
            private String title;
            private String body;
            private String sound;
            private int vibrate;
            private String icon;
            private short silent;
            private String category;
            private String from;
            private long receivedTimestamp;
            private long seenTimestamp;
            private String customPayload;
            private String internalData;
            private String contentUrl;
            private String destination;
            private String status;
            private String statusMessage;
        }

        Cursor cursor = db.rawQuery("SELECT * FROM messages", new String[0]);
        List<Message> messages = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                try {
                    Message message = new Message();
                    message.id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                    message.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                    message.body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    message.sound = cursor.getString(cursor.getColumnIndexOrThrow("sound"));
                    message.vibrate = cursor.getInt(cursor.getColumnIndexOrThrow("vibrate"));
                    message.icon = cursor.getString(cursor.getColumnIndexOrThrow("icon"));
                    message.silent = cursor.getShort(cursor.getColumnIndexOrThrow("silent"));
                    message.category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                    message.from = cursor.getString(cursor.getColumnIndexOrThrow("_from"));
                    message.receivedTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("received_timestamp"));
                    message.seenTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("seen_timestamp"));
                    message.customPayload = cursor.getString(cursor.getColumnIndexOrThrow("custom_payload"));
                    message.internalData = cursor.getString(cursor.getColumnIndexOrThrow("internal_data"));
                    message.contentUrl = cursor.getString(cursor.getColumnIndexOrThrow("content_url"));
                    message.destination = cursor.getString(cursor.getColumnIndexOrThrow("destination"));
                    message.status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                    message.statusMessage = cursor.getString(cursor.getColumnIndexOrThrow("status_message"));
                    messages.add(message);
                } catch (Exception e) {
                    MobileMessagingLogger.e("Could not load message.", e);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        JsonSerializer serializer = new JsonSerializer(false);
        for (Message m : messages) {

            // Set sendDateTime in internalData if absent
            Map internalDataMap = serializer.deserialize(m.internalData, HashMap.class);
            if (internalDataMap == null) {
                internalDataMap = new HashMap();
            }
            if (!internalDataMap.containsKey("sendDateTime")) {
                //noinspection unchecked
                internalDataMap.put("sendDateTime", m.receivedTimestamp);
            }
            m.internalData = serializer.serialize(internalDataMap);

            // Save new data to database
            ContentValues contentValues = new ContentValues();
            contentValues.put("id", m.id);
            contentValues.put("title", m.title);
            contentValues.put("body", m.body);
            contentValues.put("sound", m.sound);
            contentValues.put("vibrate", m.vibrate);
            contentValues.put("icon", m.icon);
            contentValues.put("silent", m.silent);
            contentValues.put("category", m.category);
            contentValues.put("_from", m.from);
            contentValues.put("received_timestamp", m.receivedTimestamp);
            contentValues.put("seen_timestamp", m.seenTimestamp);
            contentValues.put("custom_payload", m.customPayload);
            contentValues.put("internal_data", m.internalData);
            contentValues.put("content_url", m.contentUrl);
            contentValues.put("destination", m.destination);
            contentValues.put("status", m.status);
            contentValues.put("status_message", m.statusMessage);
            db.insertWithOnConflict("messages", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }
}