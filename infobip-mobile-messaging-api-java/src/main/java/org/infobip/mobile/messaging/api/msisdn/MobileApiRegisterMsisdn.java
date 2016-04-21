package org.infobip.mobile.messaging.api.msisdn;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Delivery reporting API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiRegisterMsisdn mobileApiRegisterMsisdn = new Generator.Builder().build().create(MobileApiRegisterMsisdn.class);
 * }</pre>
 *
 * @author mstipanov
 * @see Generator
 * @see Generator.Builder
 * @since 17.03.2016.
 */
@Version("1")
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}")
public interface MobileApiRegisterMsisdn {

    @HttpRequest(method = HttpMethod.POST, value = "msisdn")
    void registerMsisdn(@Query(name = "deviceApplicationInstanceId") String deviceApplicationInstanceId, @Query(name = "msisdn") long msisdn);
}
