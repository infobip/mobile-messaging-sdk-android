package org.infobip.mobile.messaging.mobile.seen;

import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;

/**
 * @author sslavin
 * @since 25.04.2016.
 */
class SeenStatusReportResult extends UnsuccessfulResult {
    private final String messageIDs[];

    SeenStatusReportResult(String[] messageIDs) {
        super(null);
        this.messageIDs = messageIDs;
    }

    SeenStatusReportResult(Throwable exception) {
        super(exception);
        messageIDs = new String[0];
    }

    String[] getMessageIDs() {
        return messageIDs;
    }
}
