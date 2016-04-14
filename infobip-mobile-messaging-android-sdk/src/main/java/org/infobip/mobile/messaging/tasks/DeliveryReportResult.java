package org.infobip.mobile.messaging.tasks;

/**
 * @author mstipanov
 * @since 24.03.2016.
 */
public class DeliveryReportResult {
    private final String[] messageIDs;

    public DeliveryReportResult(String[] messageIDs) {
        this.messageIDs = messageIDs;
    }

    public String[] getMessageIDs() {
        return messageIDs;
    }
}
