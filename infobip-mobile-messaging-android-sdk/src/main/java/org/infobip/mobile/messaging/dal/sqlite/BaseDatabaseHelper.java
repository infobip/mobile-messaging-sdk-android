package org.infobip.mobile.messaging.dal.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import android.util.Log;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.util.ArrayList;
import java.util.Collections;
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
        String tableName = requireValidIdentifier(getTableName(cls));
        Cursor cursor = db().query(tableName, null, null, null, null, null, null);
        List<T> objects = loadFromCursor(cursor, cls);
        cursor.close();
        return objects;
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> T find(Class<T> cls, @NonNull String primaryKey) {
        String tableName = requireValidIdentifier(getTableName(cls));
        String primaryKeyColumn = requireValidIdentifier(getPrimaryKeyColumn(cls));
        Cursor cursor = db().query(tableName, null, primaryKeyColumn + " = ?", new String[]{primaryKey}, null, null, null);
        List<T> objects = loadFromCursor(cursor, cls);
        cursor.close();
        return !objects.isEmpty() ? objects.get(0) : null;
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> long countAll(Class<T> cls) {
        String tableName = requireValidIdentifier(getTableName(cls));
        return DatabaseUtils.queryNumEntries(db(), tableName);
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> long countAll(Class<T> cls, String sqlWhereCondition) {
        String tableName = requireValidIdentifier(getTableName(cls));
        return DatabaseUtils.queryNumEntries(db(), tableName, sqlWhereCondition);
    }

    @Override
    public void save(DatabaseContract.DatabaseObject object) {
        String tableName = requireValidIdentifier(object.getTableName());
        db().insertWithOnConflict(tableName, null, object.getContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public void insert(DatabaseContract.DatabaseObject object) throws PrimaryKeyViolationException {
        try {
            String tableName = requireValidIdentifier(object.getTableName());
            db().insertOrThrow(tableName, null, object.getContentValues());
        } catch (SQLException ignored) {
            throw new PrimaryKeyViolationException();
        }
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> void deleteAll(Class<T> cls) {
        String tableName = requireValidIdentifier(getTableName(cls));
        db().delete(tableName, null, new String[0]);
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> void delete(Class<T> cls, @NonNull String primaryKey) {
        String tableName = requireValidIdentifier(getTableName(cls));
        String primaryKeyColumn = requireValidIdentifier(getPrimaryKeyColumn(cls));
        db().delete(tableName, primaryKeyColumn + "=?", new String[]{primaryKey});
    }

    @Override
    public <T extends DatabaseContract.DatabaseObject> void delete(Class<T> cls, String[] primaryKeys) {
        if (primaryKeys == null || primaryKeys.length == 0) {
            return; // nothing to delete
        }
        String tableName = requireValidIdentifier(getTableName(cls));
        String primaryKeyColumn = requireValidIdentifier(getPrimaryKeyColumn(cls));
        String placeholders = String.join(",", Collections.nCopies(primaryKeys.length, "?"));
        String whereClause = primaryKeyColumn + " IN (" + placeholders + ")";
        db().delete(tableName, whereClause, primaryKeys);
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

    @NonNull
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

    private boolean isValidIdentifier(String identifier) {
        return identifier != null && identifier.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    private String requireValidIdentifier(String identifier) {
        if (!isValidIdentifier(identifier)) {
            throw new IllegalArgumentException("Invalid identifier: " + identifier);
        }
        return identifier;
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
