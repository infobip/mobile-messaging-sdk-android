package org.infobip.mobile.messaging.chat.repository;

import android.content.ContentValues;
import android.database.Cursor;

import org.infobip.mobile.messaging.chat.repository.db.ChatDatabaseContract;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class Message implements DatabaseContract.DatabaseObject {

    protected String id;
    String chatId;
    String body;
    Long receivedAt;
    Long createdAt;
    Long readAt;
    String category;
    String contentUrl;
    String authorId;
    String status;
    String customData;
    boolean isYours;

    @Override
    public String getTableName() {
        return ChatDatabaseContract.Tables.MESSAGES;
    }

    @Override
    public String getPrimaryKeyColumnName() {
        return ChatDatabaseContract.MessageColumns.ID;
    }

    @Override
    public void fillFromCursor(Cursor cursor) throws Exception {
        id = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.ID));
        chatId = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.CHAT_ID));
        body = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.BODY));
        receivedAt = cursor.getLong(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.RECEIVED_TIMESTAMP));
        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.CREATED_TIMESTAMP));
        readAt = cursor.getLong(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.READ_TIMESTAMP));
        category = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.CATEGORY));
        contentUrl = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.CONTENT_URL));
        authorId = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.AUTHOR_ID));
        status = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.STATUS));
        customData = cursor.getString(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.CUSTOM_DATA));
        isYours = cursor.getShort(cursor.getColumnIndexOrThrow(ChatDatabaseContract.MessageColumns.YOURS)) == 1;
    }

    @Override
    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ChatDatabaseContract.MessageColumns.ID, id);
        contentValues.put(ChatDatabaseContract.MessageColumns.CHAT_ID, chatId);
        contentValues.put(ChatDatabaseContract.MessageColumns.BODY, body);
        contentValues.put(ChatDatabaseContract.MessageColumns.RECEIVED_TIMESTAMP, receivedAt);
        contentValues.put(ChatDatabaseContract.MessageColumns.CREATED_TIMESTAMP, createdAt);
        contentValues.put(ChatDatabaseContract.MessageColumns.READ_TIMESTAMP, readAt);
        contentValues.put(ChatDatabaseContract.MessageColumns.CATEGORY, category);
        contentValues.put(ChatDatabaseContract.MessageColumns.CONTENT_URL, contentUrl);
        contentValues.put(ChatDatabaseContract.MessageColumns.AUTHOR_ID, authorId);
        contentValues.put(ChatDatabaseContract.MessageColumns.STATUS, status);
        contentValues.put(ChatDatabaseContract.MessageColumns.CUSTOM_DATA, customData);
        contentValues.put(ChatDatabaseContract.MessageColumns.YOURS, isYours ? 1 : 0);
        return contentValues;
    }
}
