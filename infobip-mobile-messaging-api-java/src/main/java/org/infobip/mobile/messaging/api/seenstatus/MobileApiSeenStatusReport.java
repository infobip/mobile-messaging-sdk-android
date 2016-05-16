package org.infobip.mobile.messaging.api.seenstatus;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Seen status reporting API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiSeenStatusReport mobileApiSeenStatusReport = new Generator.Builder().build().create(MobileApiSeenStatusReport.class);
 * }</pre>
 *
 * @author sslavin
 * @see Generator
 * @see Generator.Builder
 * @since 25.03.2016.
 */
@Version("1")
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}/messages")
public interface MobileApiSeenStatusReport {
    @HttpRequest(method = HttpMethod.POST, value = "seen")
    void report(@Body() SeenMessages seenReport);
}
