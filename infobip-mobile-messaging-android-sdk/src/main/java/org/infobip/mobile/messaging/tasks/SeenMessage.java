package org.infobip.mobile.messaging.tasks;

import java.util.Date;

/**
 * @auhtor sslavin
 * @since 25.04.2016.
 */
public class SeenMessage {
    private final String messageId;
    private final String supplementaryId;
    private final Double seenDate;

    public SeenMessage(String messageId) {
        this(messageId, "", (double) (new Date()).getTime());
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
