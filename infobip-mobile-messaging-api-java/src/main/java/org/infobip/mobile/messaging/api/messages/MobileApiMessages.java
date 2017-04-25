package org.infobip.mobile.messaging.api.messages;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Message-related mobile API.
 * <p/>
 * Usage:
 * <pre>{@code
 * MobileApiMessages mobileApiMessages = new Generator.Builder().build().create(MobileApiMessages.class);
 * }</pre>
 *
 * @author sslavin
 * @see Generator
 * @see Generator.Builder
 * @since 25.03.2016.
 */
@ApiKey("${api.key}")
public interface MobileApiMessages {

    @HttpRequest(method = HttpMethod.POST, value = "/api/v2/dr/push")
    void reportSeen(@Body() SeenBody seenReport);

    @HttpRequest(method = HttpMethod.POST, value = "/mobile/1/messages/mo")
    @Query(name = "platformType", value = "${platform.type:GCM}")
    MoMessagesResponse sendMO(@Body() MoMessagesBody moMessagesBody);

    @HttpRequest(method = HttpMethod.POST, value = "/mobile/5/messages")
    @Query(name = "platformType", value = "${platform.type:GCM}")
    SyncMessagesResponse sync(@Body() SyncMessagesBody pushMessagesBody);
}
