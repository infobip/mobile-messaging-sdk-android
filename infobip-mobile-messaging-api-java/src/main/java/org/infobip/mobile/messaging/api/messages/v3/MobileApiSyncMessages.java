package org.infobip.mobile.messaging.api.messages.v3;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Synchronizing messages with API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiMessages mobileApiSeenStatusReport = new Generator.Builder().build().create(MobileApiMessages.class);
 * }</pre>
 *
 * @author pandric on 09/09/16.
 * @see Generator
 * @see Generator.Builder
 */
@Version("3")
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}/messages")
public interface MobileApiSyncMessages {
    @HttpRequest(method = HttpMethod.POST)
    @Query(name = "platformType", value = "${platform.type:GCM}")
    SyncMessagesResponse syncMessages(@Query(name = "deviceApplicationInstanceId") String deviceApplicationInstanceId,
                                      @Body() SyncMessagesBody pushMessagesBody);
}
