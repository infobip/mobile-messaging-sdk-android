package org.infobip.mobile.messaging.chat.repository;

import android.content.ContentValues;
import android.database.Cursor;

import org.infobip.mobile.messaging.chat.repository.db.ChatDatabaseContract;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class Participant implements DatabaseContract.DatabaseObject {

    protected String id;
    String firstName;
    String lastName;
    String middleName;
    String email;
    String gsm;
    String customData;

    @Override
    public String getTableName() {
        return ChatDatabaseContract.Tables.PARTICIPANTS;
    }

    @Override
    public String getPrimaryKeyColumnName() {
        return ChatDatabaseContract.ParticipantColumns.ID;
    }

    @Override
    public void fillFromCursor(Cursor cursor) throws Exception {
        id = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.ParticipantColumns.ID));
        firstName = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.ParticipantColumns.FIRST_NAME));
        lastName = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.ParticipantColumns.LAST_NAME));
        middleName = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.ParticipantColumns.MIDDLE_NAME));
        email = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.ParticipantColumns.EMAIL));
        gsm = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.ParticipantColumns.GSM));
        customData = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.ParticipantColumns.CUSTOM_DATA));
    }

    @Override
    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ChatDatabaseContract.ParticipantColumns.ID, id);
        contentValues.put(ChatDatabaseContract.ParticipantColumns.FIRST_NAME, firstName);
        contentValues.put(ChatDatabaseContract.ParticipantColumns.LAST_NAME, lastName);
        contentValues.put(ChatDatabaseContract.ParticipantColumns.MIDDLE_NAME, middleName);
        contentValues.put(ChatDatabaseContract.ParticipantColumns.EMAIL, email);
        contentValues.put(ChatDatabaseContract.ParticipantColumns.GSM, gsm);
        contentValues.put(ChatDatabaseContract.ParticipantColumns.CUSTOM_DATA, customData);
        return contentValues;
    }
}
