package org.infobip.mobile.messaging.api.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Seen messages report body encapsulation.
 *
 * @author sslavin
 * @see MobileApiMessages
 * @see MobileApiMessages#reportSeen(SeenMessages)
 * @since 25.04.2016.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeenMessages {

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
         * Constructs message using only the message Id, timestamp is set to current time.
         *
         * @param messageId - id of a message to report.
         */
        public Message(String messageId, long timestampDelta) {
            this.messageId = messageId;
            this.timestampDelta = timestampDelta;
        }
    }

    /**
     * Array of messages to report.
     */
    private Message[] messages;
}
