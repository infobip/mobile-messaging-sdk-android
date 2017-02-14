package org.infobip.mobile.messaging.dal.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.support.util.StringUtils;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract.DatabaseObject;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract.MessageColumns;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract.Tables;

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

    private static final int VER_2017_JAN_12 = 1; // Initial version
    private static final int VER_2017_FEB_2 = 2;  // Removed internal data and added geo column
    private static final int VER_CURRENT = VER_2017_FEB_2;

    @SuppressWarnings("WeakerAccess")
    static final String DATABASE_NAME = "mm_infobip_database.db";

    static final String SQL_CREATE_MESSAGES_TABLE = "CREATE TABLE " + Tables.MESSAGES + " (" +
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
            MessageColumns.GEO + " TEXT, " +
            MessageColumns.CUSTOM_PAYLOAD + " TEXT, " +
            MessageColumns.DESTINATION + " TEXT, " +
            MessageColumns.STATUS + " TEXT," +
            MessageColumns.STATUS_MESSAGE + " TEXT)";

    static final String SQL_CREATE_GEO_MESSAGES_TABLE = "CREATE TABLE " + Tables.GEO_MESSAGES + " (" +
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
            MessageColumns.GEO + " TEXT, " +
            MessageColumns.CUSTOM_PAYLOAD + " TEXT, " +
            MessageColumns.DESTINATION + " TEXT, " +
            MessageColumns.STATUS + " TEXT," +
            MessageColumns.STATUS_MESSAGE + " TEXT)";

    private final Context context;
    private final SQLiteDatabase db;

    public DatabaseHelperImpl(Context context) {
        super(context, DATABASE_NAME, null, VER_CURRENT);
        this.context = context;
        this.db = getWritableDatabase();
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> List<T> findAll(Class<T> cls) {
        Cursor cursor = db.rawQuery("SELECT * FROM " + getTableName(cls), new String[0]);
        List<T> objects = loadFromCursor(cursor, cls);
        cursor.close();
        return objects;
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> T find(Class<T> cls, @NonNull String primaryKey) {
        Cursor cursor = db.rawQuery("SELECT * FROM " + getTableName(cls) + " WHERE " + getPrimaryKeyColumn(cls) + " = ?", new String[]{primaryKey});
        List<T> objects = loadFromCursor(cursor, cls);
        cursor.close();
        return objects != null && !objects.isEmpty() ? objects.get(0) : null;
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> long countAll(Class<T> cls) {
        return DatabaseUtils.queryNumEntries(db, getTableName(cls));
    }

    @Override
    public void save(DatabaseObject object) {
        db.insertWithOnConflict(object.getTableName(), null, object.getContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> void deleteAll(Class<T> cls) {
        db.delete(getTableName(cls), null, new String[0]);
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> void delete(Class<T> cls, @NonNull String primaryKey) {
        db.delete(getTableName(cls), getPrimaryKeyColumn(cls) + "=?", new String[]{primaryKey});
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_MESSAGES_TABLE);
        db.execSQL(SQL_CREATE_GEO_MESSAGES_TABLE);
        SharedPreferencesMigrator.migrateMessages(context, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int version = oldVersion;
        if (version <= VER_2017_JAN_12) {

            String messageColumns = StringUtils.join(",", MessageColumns.MESSAGE_ID, MessageColumns.TITLE,
                    MessageColumns.BODY, MessageColumns.SOUND, MessageColumns.VIBRATE, MessageColumns.ICON,
                    MessageColumns.SILENT, MessageColumns.CATEGORY, MessageColumns.FROM, MessageColumns.RECEIVED_TIMESTAMP,
                    MessageColumns.SEEN_TIMESTAMP, MessageColumns.GEO, MessageColumns.CUSTOM_PAYLOAD, MessageColumns.DESTINATION,
                    MessageColumns.STATUS, MessageColumns.STATUS_MESSAGE
            );

            String oldMessageColumns = StringUtils.join(",", MessageColumns.MESSAGE_ID, MessageColumns.TITLE,
                    MessageColumns.BODY, MessageColumns.SOUND, MessageColumns.VIBRATE, MessageColumns.ICON,
                    MessageColumns.SILENT, MessageColumns.CATEGORY, MessageColumns.FROM, MessageColumns.RECEIVED_TIMESTAMP,
                    MessageColumns.SEEN_TIMESTAMP, MessageColumns.Deprecated.INTERNAL_DATA, MessageColumns.CUSTOM_PAYLOAD, MessageColumns.DESTINATION,
                    MessageColumns.STATUS, MessageColumns.STATUS_MESSAGE
            );

            db.beginTransaction();
            db.execSQL("ALTER TABLE " + Tables.MESSAGES + " RENAME TO tmp_" + Tables.MESSAGES);
            db.execSQL(SQL_CREATE_MESSAGES_TABLE);
            db.execSQL("INSERT INTO " + Tables.MESSAGES + "(" + messageColumns + ")" +
                    " SELECT " + oldMessageColumns +
                    " FROM tmp_" + Tables.MESSAGES);
            db.execSQL("DROP TABLE tmp_" + Tables.MESSAGES);
            db.execSQL(SQL_CREATE_GEO_MESSAGES_TABLE);
            db.setTransactionSuccessful();
            db.endTransaction();

            version = VER_2017_FEB_2;
        }

        if (version != VER_CURRENT) {
            MobileMessagingLogger.e("SQLite DB version is not what expected: " + VER_CURRENT);
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
        return db;
    }
}