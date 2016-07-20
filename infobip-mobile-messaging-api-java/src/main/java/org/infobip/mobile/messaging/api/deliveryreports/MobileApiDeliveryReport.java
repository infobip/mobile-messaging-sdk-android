package org.infobip.mobile.messaging.api.deliveryreports;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Delivery reporting API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiDeliveryReport mobileApiDeliveryReport = new Generator.Builder().build().create(MobileApiDeliveryReport.class);
 * }</pre>
 *
 * @author mstipanov
 * @see Generator
 * @see Generator.Builder
 * @since 17.03.2016.
 */
@Version("2")
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}")
public interface MobileApiDeliveryReport {

    @HttpRequest(method = HttpMethod.POST, value = "deliveryreports")
    void report(@Body() DeliveryReport deliveryReport);
}
