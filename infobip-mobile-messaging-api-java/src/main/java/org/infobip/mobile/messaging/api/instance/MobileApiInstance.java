package org.infobip.mobile.messaging.api.instance;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Geo-related mobile API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiGeo mobileApiGeo = new Generator.Builder().build().create(MobileApiGeo.class);
 * }</pre>
 *
 * @author sslavin
 * @see Generator
 * @see Generator.Builder
 * @since 20/06/2018.
 */
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}/instance")
@Version("1")
public interface MobileApiInstance {

    @HttpRequest(method = HttpMethod.GET)
    Instance get();

    @HttpRequest(method = HttpMethod.PUT)
    void update(@Body Instance instance);
}
