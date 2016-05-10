package org.infobip.mobile.messaging.tasks;

/**
 * @author sslavin
 * @since 25.04.2016.
 */
public class SeenStatusReportResult {
    private final String messageIDs[];

    public SeenStatusReportResult(String[] messageIDs) {
        this.messageIDs = messageIDs;
    }

    public String[] getMessageIDs() {
        return messageIDs;
    }
}
