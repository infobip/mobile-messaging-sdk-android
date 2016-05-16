package org.infobip.mobile.messaging.api.seenstatus;

/**
 *
 * Seen messages report body encapsulation.
 *
 * @author sslavin
 * @see MobileApiSeenStatusReport
 * @see MobileApiSeenStatusReport#report(SeenMessages)
 * @since 25.04.2016.
 */
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
            this(messageId, System.currentTimeMillis());
        }

        /**
         * Constructs message with the supplied Id and timestamp in long format.
         *
         * @param messageId - id of a message.
         * @param seenDate - tmestamp in long.
         */
        public Message(String messageId, long seenDate) {
            this(messageId, (double) seenDate);
        }

        /**
         * Constructs message with the supplied id and timestamp in Double format.
         *
         * @param messageId - id of a message.
         * @param seenDate - timestamp in Double.
         */
        public Message(String messageId, Double seenDate) {
            this.messageId = messageId;
            this.seenDate = seenDate;
        }
    }

    /**
     * Default constructor
     * @param messages - one or more messages to report
     */

    public SeenMessages(Message... messages) {
        this.messages = messages;
    }

    /**
     * Array of messages to report.
     */
    private Message messages[];
}
