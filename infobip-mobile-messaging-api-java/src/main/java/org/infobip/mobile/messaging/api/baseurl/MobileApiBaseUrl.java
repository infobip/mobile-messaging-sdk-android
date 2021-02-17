package org.infobip.mobile.messaging.api.baseurl;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Base URL-related mobile API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiBaseUrl mobileApiBaseUrl = new Generator.Builder().build().create(MobileApiBaseUrl.class);
 * }</pre>
 *
 * @see Generator
 * @see Generator.Builder
 */
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}/baseUrl")
@Version("1")
public interface MobileApiBaseUrl {

    @HttpRequest(method = HttpMethod.GET)
    BaseUrlResponse getBaseUrl();
}