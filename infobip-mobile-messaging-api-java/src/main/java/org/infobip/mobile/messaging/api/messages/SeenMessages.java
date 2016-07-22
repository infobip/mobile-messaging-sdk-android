package org.infobip.mobile.messaging.api.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
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
         * Seen timestamp (since 1970 in UTC).
         */
        private final Double seenDate;

        /**
         * Constructs message using only the message Id, timestamp is set to current time.
         *
         * @param messageId - id of a message to report.
         */
        public Message(String messageId) {
            this.messageId = messageId;
            this.seenDate = (double) System.currentTimeMillis();
        }
    }

    /**
     * Array of messages to report.
     */
    private Message messages[];
}
