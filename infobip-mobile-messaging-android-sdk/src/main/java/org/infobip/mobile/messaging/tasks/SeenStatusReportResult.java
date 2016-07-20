package org.infobip.mobile.messaging.tasks;

/**
 * @author sslavin
 * @since 25.04.2016.
 */
public class SeenStatusReportResult extends UnsuccessfulResult {
    private final String messageIDs[];

    public SeenStatusReportResult(String[] messageIDs) {
        this.messageIDs = messageIDs;
    }

    public SeenStatusReportResult(Throwable exception) {
        super(exception);
        messageIDs = new String[0];
    }

    public String[] getMessageIDs() {
        return messageIDs;
    }
}
