package org.infobip.mobile.messaging.api.registration;

import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * @author mstipanov
 * @since 17.03.2016.
 */
public interface MobileApiDeliveryReport {

    @HttpRequest(method = HttpMethod.POST, value = "/mobile/{version}/deliveryreports")
    DeliveryReportResponse report(@Query(name = "messageIDs") String... messageIDs);
}
