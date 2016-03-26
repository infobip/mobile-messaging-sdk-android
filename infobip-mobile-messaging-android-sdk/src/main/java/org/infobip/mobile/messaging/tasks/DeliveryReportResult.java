package org.infobip.mobile.messaging.tasks;

import org.infobip.mobile.messaging.api.registration.DeliveryReportResponse;

/**
 * @author mstipanov
 * @since 24.03.2016.
 */
public class DeliveryReportResult {
    private final DeliveryReportResponse deliveryReportResponse;
    private final String[] messageIDs;

    public DeliveryReportResult(DeliveryReportResponse deliveryReportResponse, String[] messageIDs) {
        this.deliveryReportResponse = deliveryReportResponse;
        this.messageIDs = messageIDs;
    }

    public DeliveryReportResponse getDeliveryReportResponse() {
        return deliveryReportResponse;
    }

    public String[] getMessageIDs() {
        return messageIDs;
    }
}
