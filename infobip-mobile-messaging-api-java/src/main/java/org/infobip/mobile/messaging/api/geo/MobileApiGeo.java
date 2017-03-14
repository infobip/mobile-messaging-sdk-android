package org.infobip.mobile.messaging.api.geo;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Geo-related mobile API.
 * <p/>
 * Usage:
 * <pre>{@code
 * MobileApiGeo mobileApiGeo = new Generator.Builder().build().create(MobileApiGeo.class);
 * }</pre>
 *
 * @author sslavin
 * @see Generator
 * @see Generator.Builder
 * @since 19.10.2016.
 */
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}/geo")
@Version("4")
public interface MobileApiGeo {

    @HttpRequest(method = HttpMethod.POST, value = "event")
    EventReportResponse report(@Body() EventReportBody reports);
}