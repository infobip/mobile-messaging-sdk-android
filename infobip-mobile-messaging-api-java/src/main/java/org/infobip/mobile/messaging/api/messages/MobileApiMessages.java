package org.infobip.mobile.messaging.api.messages;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Seen status reporting API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiMessages mobileApiSeenStatusReport = new Generator.Builder().build().create(MobileApiMessages.class);
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
public interface MobileApiMessages {
    @HttpRequest(method = HttpMethod.POST, value = "seen")
    void reportSeen(@Body() SeenMessages seenReport);

    @HttpRequest(method = HttpMethod.POST, value = "mo")
    @Query(name = "platformType", value = "${platform.type:GCM}")
    MoMessagesResponse sendMO(@Body() MoMessagesBody moMessagesBody);
}
