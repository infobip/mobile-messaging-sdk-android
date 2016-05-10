package org.infobip.mobile.messaging.tasks;

/**
 * @author sslavin
 * @since 25.04.2016.
 */
public class SeenMessage {
    private final String messageId;
    private final String supplementaryId;
    private final Double seenDate;

    public SeenMessage(String messageId) {
        this(messageId, "", (double) System.currentTimeMillis());
    }

    public SeenMessage(String messageId, long seenDate) {
        this(messageId, "", (double) seenDate);
    }

    public SeenMessage(String messageId, String supplementaryId, Double seenDate) {
        this.messageId = messageId;
        this.supplementaryId = supplementaryId;
        this.seenDate = seenDate;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getSupplementaryId() {
        return supplementaryId;
    }

    public Double getSeenDate() {
        return seenDate;
    }
}
