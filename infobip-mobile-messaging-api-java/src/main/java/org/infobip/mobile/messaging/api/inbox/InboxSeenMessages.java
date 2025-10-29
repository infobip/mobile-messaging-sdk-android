/*
 * InboxSeenMessages.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.inbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Seen messages body for Inbox
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboxSeenMessages {

    private String externalUserId;
    private Message[] messages;

    public static class Message {
        /**
         * Id of a message.
         */
        private final String messageId;
        /**
         * Delta timestamp (now - seenTimestamp) - delta between timestamp in seconds between now (time when seen status is sent to the backend)
         * and seenTimestamp (when seen timestamp was actually recorded).
         */
        private final long timestampDelta;
        /**
         * Flag for inbox - all messages that have this param set to true are stored in Inbox - "true" by default.
         */
        private final boolean inbox;

        /**
         * Constructs message using only the message Id, timestamp is set to current time, inbox to true.
         *
         * @param messageId - id of a message to report.
         */
        public Message(String messageId, long timestampDelta) {
            this.messageId = messageId;
            this.timestampDelta = timestampDelta;
            this.inbox = true;
        }
    }
}
