package org.infobip.mobile.messaging.dal.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract.MessageColumns;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract.Tables;
import org.json.JSONObject;

/**
 * @author sslavin
 * @since 09/01/2017.
 */

public class SqliteMessage extends Message implements DatabaseContract.DatabaseObject {

    public SqliteMessage() {
        super(null, null, null, null, true, null, false, null, null, 0, 0, 0, null, null, null, Status.UNKNOWN, null, null, null);
    }

    public SqliteMessage(Message m) {
        super(
                m.getMessageId(),
                m.getTitle(),
                m.getBody(),
                m.getSound(),
                m.isVibrate(),
                m.getIcon(),
                m.isSilent(),
                m.getCategory(),
                m.getFrom(),
                m.getReceivedTimestamp(),
                m.getSeenTimestamp(),
                m.getSentTimestamp(),
                m.getCustomPayload(),
                m.getInternalData(),
                m.getDestination(),
                m.getStatus(),
                m.getStatusMessage(),
                m.getContentUrl(),
                m.getInAppStyle()
        );
    }

    public static ContentValues save(Message message) {
        return new SqliteMessage(message).getContentValues();
    }

    public static Message load(Cursor cursor) throws Exception {
        SqliteMessage sqliteMessage = new SqliteMessage();
        sqliteMessage.fillFromCursor(cursor);
        return sqliteMessage;
    }

    public static String getTable() {
        return new SqliteMessage().getTableName();
    }

    @Override
    public void fillFromCursor(Cursor cursor) throws Exception {
        setMessageId(cursor.getString(cursor.getColumnIndexOrThrow(MessageColumns.MESSAGE_ID)));
        setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MessageColumns.TITLE)));
        setBody(cursor.getString(cursor.getColumnIndexOrThrow(MessageColumns.BODY)));
        setSound(cursor.getString(cursor.getColumnIndexOrThrow(MessageColumns.SOUND)));
        setVibrate(cursor.getInt(cursor.getColumnIndexOrThrow(MessageColumns.VIBRATE)) != 0);
        setIcon(cursor.getString(cursor.getColumnIndexOrThrow(MessageColumns.ICON)));
        setSilent(cursor.getShort(cursor.getColumnIndexOrThrow(MessageColumns.SILENT)) != 0);
        setCategory(cursor.getString(cursor.getColumnIndexOrThrow(MessageColumns.CATEGORY)));
        setFrom(cursor.getString(cursor.getColumnIndexOrThrow(MessageColumns.FROM)));
        setReceivedTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(MessageColumns.RECEIVED_TIMESTAMP)));
        setSeenTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(MessageColumns.SEEN_TIMESTAMP)));
        setContentUrl(cursor.getString(cursor.getColumnIndexOrThrow(MessageColumns.CONTENT_URL)));

        String dataJson = cursor.getString(cursor.getColumnIndexOrThrow(MessageColumns.INTERNAL_DATA));
        setInternalData(dataJson);
        setSentTimestamp(InternalDataMapper.getInternalDataSendDateTime(dataJson));

        dataJson = cursor.getString(cursor.getColumnIndexOrThrow(MessageColumns.CUSTOM_PAYLOAD));
        setCustomPayload(dataJson == null ? null : new JSONObject(dataJson));

        setDestination(cursor.getString(cursor.getColumnIndex(MessageColumns.DESTINATION)));
        String statusName = cursor.getString(cursor.getColumnIndex(MessageColumns.STATUS));
        setStatus(statusName != null ? Status.valueOf(statusName) : null);
        setStatusMessage(cursor.getString(cursor.getColumnIndex(MessageColumns.STATUS_MESSAGE)));
        try {
            String inAppStyle = cursor.getString(cursor.getColumnIndex(MessageColumns.IN_APP_STYLE));
            setInAppStyle(InAppStyle.valueOf(inAppStyle));
        } catch (Exception ignored) {

        }
    }

    @Override
    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageColumns.MESSAGE_ID, getMessageId());
        contentValues.put(MessageColumns.TITLE, getTitle());
        contentValues.put(MessageColumns.BODY, getBody());
        contentValues.put(MessageColumns.SOUND, getSound());
        contentValues.put(MessageColumns.VIBRATE, isVibrate() ? 1 : 0);
        contentValues.put(MessageColumns.ICON, getIcon());
        contentValues.put(MessageColumns.SILENT, isSilent() ? 1 : 0);
        contentValues.put(MessageColumns.CATEGORY, getCategory());
        contentValues.put(MessageColumns.FROM, getFrom());
        contentValues.put(MessageColumns.RECEIVED_TIMESTAMP, getReceivedTimestamp());
        contentValues.put(MessageColumns.SEEN_TIMESTAMP, getSeenTimestamp());
        contentValues.put(MessageColumns.INTERNAL_DATA, InternalDataMapper.createInternalDataBasedOnMessageContents(this));
        contentValues.put(MessageColumns.CUSTOM_PAYLOAD, getCustomPayload() != null ? getCustomPayload().toString() : null);
        contentValues.put(MessageColumns.DESTINATION, getDestination());
        contentValues.put(MessageColumns.STATUS, getStatus() != null ? getStatus().name() : null);
        contentValues.put(MessageColumns.STATUS_MESSAGE, getStatusMessage());
        contentValues.put(MessageColumns.CONTENT_URL, getContentUrl());
        contentValues.put(MessageColumns.IN_APP_STYLE, getInAppStyle() != null ? getInAppStyle().name() : null);
        return contentValues;
    }

    @Override
    public String getTableName() {
        return Tables.MESSAGES;
    }

    @Override
    public String getPrimaryKeyColumnName() {
        return MessageColumns.MESSAGE_ID;
    }
}