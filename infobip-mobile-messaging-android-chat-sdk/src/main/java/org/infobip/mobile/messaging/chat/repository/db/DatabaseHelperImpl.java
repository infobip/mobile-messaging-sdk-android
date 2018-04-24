package org.infobip.mobile.messaging.chat.repository.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.dal.sqlite.BaseDatabaseHelper;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class DatabaseHelperImpl extends BaseDatabaseHelper {

    @VisibleForTesting
    static final int VER_2018_APR_24 = 1; // Initial version
    private static final int VER_CURRENT = VER_2018_APR_24;

    @SuppressWarnings("WeakerAccess")
    static final String DATABASE_NAME = "mm_infobip_database_chat.db";

    private static final String SQL_CREATE_CHAT_MESSAGES_TABLE = "CREATE TABLE " + ChatDatabaseContract.Tables.MESSAGES + " (" +
            ChatDatabaseContract.MessageColumns.ID + " TEXT PRIMARY KEY NOT NULL ON CONFLICT FAIL, " +
            ChatDatabaseContract.MessageColumns.CHAT_ID + " TEXT, " +
            ChatDatabaseContract.MessageColumns.BODY + " TEXT, " +
            ChatDatabaseContract.MessageColumns.RECEIVED_TIMESTAMP + " INTEGER, " +
            ChatDatabaseContract.MessageColumns.CREATED_TIMESTAMP + " INTEGER, " +
            ChatDatabaseContract.MessageColumns.READ_TIMESTAMP + " INTEGER, " +
            ChatDatabaseContract.MessageColumns.CATEGORY + " TEXT, " +
            ChatDatabaseContract.MessageColumns.CONTENT_URL + " TEXT, " +
            ChatDatabaseContract.MessageColumns.AUTHOR_ID + " TEXT, " +
            ChatDatabaseContract.MessageColumns.STATUS + " TEXT, " +
            ChatDatabaseContract.MessageColumns.CUSTOM_DATA + " TEXT," +
            ChatDatabaseContract.MessageColumns.YOURS + " INTEGER)";

    private static final String SQL_CREATE_CHAT_PARTICIPANTS_TABLE = "CREATE TABLE " + ChatDatabaseContract.Tables.PARTICIPANTS + " (" +
            ChatDatabaseContract.ParticipantColumns.ID + " TEXT PRIMARY KEY NOT NULL ON CONFLICT FAIL, " +
            ChatDatabaseContract.ParticipantColumns.FIRST_NAME + " TEXT, " +
            ChatDatabaseContract.ParticipantColumns.LAST_NAME + " TEXT, " +
            ChatDatabaseContract.ParticipantColumns.MIDDLE_NAME + " TEXT, " +
            ChatDatabaseContract.ParticipantColumns.EMAIL + " TEXT, " +
            ChatDatabaseContract.ParticipantColumns.GSM + " TEXT, " +
            ChatDatabaseContract.MessageColumns.CUSTOM_DATA + " TEXT)";

    public DatabaseHelperImpl(Context context) {
        super(context, DATABASE_NAME, VER_CURRENT);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        db.execSQL(SQL_CREATE_CHAT_MESSAGES_TABLE);
        db.execSQL(SQL_CREATE_CHAT_PARTICIPANTS_TABLE);
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // has single version now, do nothing
    }
}
