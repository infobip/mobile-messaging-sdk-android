package org.infobip.mobile.messaging.dal.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;

/**
 * @author sslavin
 * @since 02/02/2017.
 */

public class SqliteMessageMigrationTest extends MobileMessagingTestCase {

    private static final int OLD_DB_VERSION = 1; // See database versions in DatabaseHelperImpl
    private static final String SQL_CREATE_OLD_MESSAGES_TABLE = "CREATE TABLE " + DatabaseContract.Tables.MESSAGES + " (" +
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
            DatabaseContract.MessageColumns.INTERNAL_DATA + " TEXT, " +
            DatabaseContract.MessageColumns.CUSTOM_PAYLOAD + " TEXT, " +
            DatabaseContract.MessageColumns.DESTINATION + " TEXT, " +
            DatabaseContract.MessageColumns.STATUS + " TEXT," +
            DatabaseContract.MessageColumns.STATUS_MESSAGE + " TEXT)";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void test_shouldCreateTableForGeoMessagesDuringMigration() throws Exception {
        // Create SQLiteOpenHelper directly to perform raw operations on database
        SQLiteOpenHelper sqLiteOpenHelper = new SQLiteOpenHelper(context, DatabaseHelperImpl.DATABASE_NAME, null, OLD_DB_VERSION) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(SQL_CREATE_OLD_MESSAGES_TABLE);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }
        };
        SQLiteDatabase db = sqLiteOpenHelper.getWritableDatabase();
        db.close();
        sqLiteOpenHelper.close();

        // Now create Database Helper
        // upgrade shall be performed internally and we shall load new message
        DatabaseHelper databaseHelper = new DatabaseHelperImpl(context);

        // Check that geo table exists
        SqliteDatabaseProvider provider = (SqliteDatabaseProvider) databaseHelper;
        SQLiteDatabase database = provider.getDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM sqlite_master WHERE name ='" + DatabaseContract.Tables.GEO_MESSAGES + "' and type='table'", null);
        assertEquals(1, cursor.getCount());
        cursor.close();
    }
}