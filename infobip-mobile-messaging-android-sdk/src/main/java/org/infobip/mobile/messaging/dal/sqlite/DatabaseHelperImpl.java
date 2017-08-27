package org.infobip.mobile.messaging.dal.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract.DatabaseObject;
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

public class DatabaseHelperImpl extends SQLiteOpenHelper implements DatabaseHelper, SqliteDatabaseProvider {

    private static final Map<Class<? extends DatabaseObject>, DatabaseContract.DatabaseObject> databaseObjectsCache = new HashMap<>();

    static final int VER_2017_JAN_12 = 1; // Initial version
    static final int VER_2017_FEB_14 = 2; // Added separate table for geo messages
    static final int VER_2017_MAY_15 = 3; // Added "content_url" column to messages/geo_messages table
    static final int VER_2017_AUG_25 = 4; // Added "sendDateTime" to internal data (must be present for all messages)
    private static final int VER_CURRENT = VER_2017_AUG_25;

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

    private static final String SQL_CREATE_GEO_MESSAGES_TABLE = "CREATE TABLE " + Tables.GEO_MESSAGES + " (" +
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

    private static final String SQL_ALTER_TABLE_GEO_MESSAGES_WITH_CONTENT_URL = "ALTER TABLE "
            + Tables.GEO_MESSAGES + " ADD COLUMN " + MessageColumns.CONTENT_URL + " TEXT;";

    private final Context context;
    private SQLiteDatabase sqLiteDatabase;

    public DatabaseHelperImpl(Context context) {
        super(context, DATABASE_NAME, null, VER_CURRENT);
        this.context = context;
    }

    private SQLiteDatabase db() {
        if (sqLiteDatabase == null) {
            sqLiteDatabase = getWritableDatabase();
        }
        return sqLiteDatabase;
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> List<T> findAll(Class<T> cls) {
        Cursor cursor = db().rawQuery("SELECT * FROM " + getTableName(cls), new String[0]);
        List<T> objects = loadFromCursor(cursor, cls);
        cursor.close();
        return objects;
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> T find(Class<T> cls, @NonNull String primaryKey) {
        Cursor cursor = db().rawQuery("SELECT * FROM " + getTableName(cls) + " WHERE " + getPrimaryKeyColumn(cls) + " = ?", new String[]{primaryKey});
        List<T> objects = loadFromCursor(cursor, cls);
        cursor.close();
        return objects != null && !objects.isEmpty() ? objects.get(0) : null;
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> long countAll(Class<T> cls) {
        return DatabaseUtils.queryNumEntries(db(), getTableName(cls));
    }

    @Override
    public void save(DatabaseObject object) {
        db().insertWithOnConflict(object.getTableName(), null, object.getContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> void deleteAll(Class<T> cls) {
        db().delete(getTableName(cls), null, new String[0]);
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> void delete(Class<T> cls, @NonNull String primaryKey) {
        db().delete(getTableName(cls), getPrimaryKeyColumn(cls) + "=?", new String[]{primaryKey});
    }

    @Override
    public <T extends DatabaseObject> void delete(Class<T> cls, String[] primaryKeys) {
        db().delete(getTableName(cls), getPrimaryKeyColumn(cls) +
                " IN (" + new String(new char[primaryKeys.length - 1]).replace("\0", "?,") + "?)", primaryKeys);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        db.execSQL(SQL_CREATE_MESSAGES_TABLE);
        db.execSQL(SQL_ALTER_TABLE_MESSAGES_WITH_CONTENT_URL);
        db.execSQL(SQL_CREATE_GEO_MESSAGES_TABLE);
        db.execSQL(SQL_ALTER_TABLE_GEO_MESSAGES_WITH_CONTENT_URL);
        db.setTransactionSuccessful();
        db.endTransaction();
        SharedPreferencesMigrator.migrateMessages(context, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int version = oldVersion;
        if (version <= VER_2017_JAN_12) {
            db.execSQL(SQL_CREATE_GEO_MESSAGES_TABLE);
            version = VER_2017_FEB_14;
        }

        if (version <= VER_2017_FEB_14) {
            db.execSQL(SQL_ALTER_TABLE_MESSAGES_WITH_CONTENT_URL);
            db.execSQL(SQL_ALTER_TABLE_GEO_MESSAGES_WITH_CONTENT_URL);
            version = VER_2017_MAY_15;
        }

        if (version <= VER_2017_MAY_15) {
            setSendDateTimeToReceivedTImeIfAbsent(db);
            version = VER_2017_AUG_25;
        }

        if (version != VER_CURRENT) {
            MobileMessagingLogger.e("SQLite DB version is not what expected: " + VER_CURRENT);
        }
    }

    private void setSendDateTimeToReceivedTImeIfAbsent(SQLiteDatabase db) {
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
                    MobileMessagingLogger.e(Log.getStackTraceString(e));
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

    private DatabaseObject emptyDatabaseObject(Class<? extends DatabaseObject> cls) {
        DatabaseObject emptyInstance = databaseObjectsCache.get(cls);
        if (emptyInstance != null) {
            return emptyInstance;
        }

        try {
            emptyInstance = cls.newInstance();
        } catch (Exception e) {
            MobileMessagingLogger.e(Log.getStackTraceString(e));
            throw new RuntimeException(e);
        }
        databaseObjectsCache.put(cls, emptyInstance);
        return emptyInstance;
    }

    private <T extends DatabaseContract.DatabaseObject> List<T> loadFromCursor(Cursor cursor, Class<T> cls) {
        if (cursor.getCount() == 0) {
            cursor.close();
            return new ArrayList<>();
        }

        List<T> objects = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                try {
                    T object = cls.newInstance();
                    object.fillFromCursor(cursor);
                    objects.add(object);
                } catch (Exception e) {
                    MobileMessagingLogger.e(Log.getStackTraceString(e));
                }
            } while (cursor.moveToNext());
        }
        return objects;
    }

    private String getTableName(Class<? extends DatabaseObject> cls) {
        DatabaseObject o = emptyDatabaseObject(cls);
        return o != null ? o.getTableName() : null;
    }

    private String getPrimaryKeyColumn(Class<? extends DatabaseObject> cls) {
        DatabaseObject o = emptyDatabaseObject(cls);
        return o != null ? o.getPrimaryKeyColumnName() : null;
    }

    @Override
    public SQLiteDatabase getDatabase() {
        return db();
    }

    @Override
    public void deleteDatabase() {
        if (sqLiteDatabase != null) {
            sqLiteDatabase.close();
            sqLiteDatabase = null;
        }
        context.deleteDatabase(DATABASE_NAME);
    }
}