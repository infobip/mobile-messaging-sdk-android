package org.infobip.mobile.messaging.tasks;

/**
 * @author mstipanov
 * @since 24.03.2016.
 */
public class DeliveryReportResult extends UnsuccessfulResult {
    private final String[] messageIDs;

    public DeliveryReportResult(String[] messageIDs) {
        this.messageIDs = messageIDs;

    }

    public DeliveryReportResult(Throwable exception) {
        super(exception);
        messageIDs = new String[0];
    }

    public String[] getMessageIDs() {
        return messageIDs;
    }
}
