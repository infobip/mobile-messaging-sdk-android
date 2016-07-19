package org.infobip.mobile.messaging.api.deliveryreports;

/**
 * Created by tjuric on 19/07/16.
 */
public class DeliveryReport {

    /**
     * Array of messageIDs to report.
     */
    private String messageIDs[];

    public DeliveryReport(String... messageIDs) {
        this.messageIDs = messageIDs;
    }

}
