package org.infobip.mobile.messaging.dal.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public abstract class BaseDatabaseHelper extends SQLiteOpenHelper implements DatabaseHelper, SqliteDatabaseProvider {

    private static final Map<Class<? extends DatabaseContract.DatabaseObject>, DatabaseContract.DatabaseObject> databaseObjectsCache = new HashMap<>();

    protected final Context context;
    private SQLiteDatabase sqLiteDatabase;

    public BaseDatabaseHelper(Context context, String databaseName, int currentVersion) {
        super(context, databaseName, null, currentVersion);
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
    public <T extends DatabaseContract.DatabaseObject> long countAll(Class<T> cls, String sqlWhereCondition) {
        return DatabaseUtils.queryNumEntries(db(), getTableName(cls), sqlWhereCondition);
    }

    @Override
    public void save(DatabaseContract.DatabaseObject object) {
        db().insertWithOnConflict(object.getTableName(), null, object.getContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public void insert(DatabaseContract.DatabaseObject object) throws PrimaryKeyViolationException {
        try {
            db().insertOrThrow(object.getTableName(), null, object.getContentValues());
        } catch (SQLException ignored) {
            throw new PrimaryKeyViolationException();
        }
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
    public <T extends DatabaseContract.DatabaseObject> void delete(Class<T> cls, String[] primaryKeys) {
        db().delete(getTableName(cls), getPrimaryKeyColumn(cls) +
                " IN (" + new String(new char[primaryKeys.length - 1]).replace("\0", "?,") + "?)", primaryKeys);
    }

    private DatabaseContract.DatabaseObject emptyDatabaseObject(Class<? extends DatabaseContract.DatabaseObject> cls) {
        DatabaseContract.DatabaseObject emptyInstance = databaseObjectsCache.get(cls);
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

    private String getTableName(Class<? extends DatabaseContract.DatabaseObject> cls) {
        DatabaseContract.DatabaseObject o = emptyDatabaseObject(cls);
        return o != null ? o.getTableName() : null;
    }

    private String getPrimaryKeyColumn(Class<? extends DatabaseContract.DatabaseObject> cls) {
        DatabaseContract.DatabaseObject o = emptyDatabaseObject(cls);
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
        context.deleteDatabase(getDatabaseName());
    }
}
