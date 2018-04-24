package org.infobip.mobile.messaging.chat.repository.db;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public interface ChatDatabaseContract {

    interface Tables {
        /**
         * Table for messages
         */
        String MESSAGES = "messages";

        /**
         * Table for participants
         */
        String PARTICIPANTS = "participants";
    }

    interface MessageColumns {
        /**
         * Unique string identifier, typically UUID, primary key
         *  [TEXT]
         */
        String ID = "id";

        /**
         * Unique string identifier of chat
         *  [TEXT]
         */
        String CHAT_ID = "chat_id";

        /**
         * Message body text
         *  [TEXT]
         */
        String BODY = "body";

        /**
         * Timestamp when message was received
         *  [INTEGER]
         */
        String RECEIVED_TIMESTAMP  = "received_timestamp";

        /**
         * Timestamp when message was created
         *  [INTEGER]
         */
        String CREATED_TIMESTAMP = "created_timestamp";

        /**
         * Timestamp when message was seen
         *  [INTEGER]
         */
        String READ_TIMESTAMP = "read_timestamp";

        /**
         * Actionable category
         *  [TEXT]
         */
        String CATEGORY = "category";

        /**
         * Actionable category
         *  [TEXT]
         */
        String CONTENT_URL = "content_url";


        /**
         * ID of author of message (Participant)
         *  [TEXT]
         */
        String AUTHOR_ID = "author_id";

        /**
         * Message status name
         *  [TEXT]
         */
        String STATUS = "status";

        /**
         * Custom data json
         *  [TEXT]
         */
        String CUSTOM_DATA = "custom_data";

        /**
         * 1 if message belongs to current user, 0 otherwise
         *  [INTEGER]
         */
        String YOURS = "yours";
    }

    interface ParticipantColumns {
        /**
         * Unique string identifier, typically UUID, primary key
         *  [TEXT]
         */
        String ID = "id";

        /**
         * First name of participant
         *  [TEXT]
         */
        String FIRST_NAME = "first_name";

        /**
         * Last name of participant
         *  [TEXT]
         */
        String LAST_NAME = "last_name";

        /**
         * Middle name of participant
         *  [TEXT]
         */
        String MIDDLE_NAME = "middle_name";

        /**
         * Email of participant
         *  [TEXT]
         */
        String EMAIL = "email";

        /**
         * GSM number of participant
         *  [TEXT]
         */
        String GSM = "gsm";

        /**
         * Custom data json
         *  [TEXT]
         */
        String CUSTOM_DATA = "custom_data";
    }
}
