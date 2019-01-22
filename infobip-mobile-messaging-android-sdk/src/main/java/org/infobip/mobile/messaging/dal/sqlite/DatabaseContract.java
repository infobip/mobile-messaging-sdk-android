package org.infobip.mobile.messaging.dal.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * @author sslavin
 * @since 12/01/2017.
 */

public interface DatabaseContract {

    interface Tables {
        /**
         * Table for messages
         */
        String MESSAGES = "messages";

        /**
         * Table for geo messages
         */
        String  GEO_MESSAGES = "geo_messages";
    }

    interface MessageColumns {
        /**
         * Unique string identifier, typically UUID, primary key
         *  [TEXT]
         */
        String MESSAGE_ID = "id";

        /**
         * Title of message
         *  [TEXT]
         */
        String TITLE = "title";

        /**
         * Message body text
         *  [TEXT]
         */
        String BODY = "body";

        /**
         * Custom sound of a message
         *  [TEXT]
         */
        String SOUND = "sound";

        /**
         * Flag that indicates if message notification should produce vibration
         *  [INTEGER]
         */
        String VIBRATE = "vibrate";

        /**
         * Custom icon identifier for message notification
         *  [TEXT]
         */
        String ICON = "icon";

        /**
         * Flag that indicates if message should be received silently, i.e. no message notification should appear
         *  [INTEGER]
         */
        String SILENT = "silent";

        /**
         * Message category for actionable notifications
         *  [TEXT]
         */
        String CATEGORY = "category";

        /**
         * Sender of a message
         *  [TEXT]
         */
        String FROM = "_from";

        /**
         * Timestamp when message was received
         *  [INTEGER]
         */
        String RECEIVED_TIMESTAMP  = "received_timestamp";

        /**
         * Timestamp when message was seen/read
         *  [INTEGER]
         */
        String SEEN_TIMESTAMP = "seen_timestamp";

        /**
         * Internal data json
         *  [TEXT]
         */
        String INTERNAL_DATA = "internal_data";

        /**
         * Custom payload json
         *  [TEXT]
         */
        String CUSTOM_PAYLOAD = "custom_payload";

        /**
         * MO message destination
         *  [TEXT]
         */
        String DESTINATION = "destination";

        /**
         * MO message status
         *  [TEXT]
         */
        String STATUS = "status";

        /**
         * MO message status text
         *  [TEXT]
         */
        String STATUS_MESSAGE = "status_message";

        /**
         * url of media content
         *  [TEXT]
         */
        String CONTENT_URL = "content_url";

        /**
         * style of in app notificiation
         *  [TEXT]
         */
        String IN_APP_STYLE = "in_app_style";
    }

    interface DatabaseObject {

        /**
         * Get table name for object
         * @return table name
         */
        String getTableName();

        /**
         * Gets the name of primary key column
         * @return column name
         */
        String getPrimaryKeyColumnName();

        /**
         * Fills object from database row
         * @param cursor for current database row
         */
        void fillFromCursor(Cursor cursor) throws Exception;

        /**
         * Serializes object to content values
         * @return content values
         */
        ContentValues getContentValues();
    }
}