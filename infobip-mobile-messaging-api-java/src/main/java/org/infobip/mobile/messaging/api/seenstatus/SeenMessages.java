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
            this.messageId = messageId;
            this.seenDate = (double) System.currentTimeMillis();
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
