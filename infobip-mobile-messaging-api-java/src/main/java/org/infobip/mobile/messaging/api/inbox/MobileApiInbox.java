package org.infobip.mobile.messaging.api.inbox;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.Header;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Path;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;


/**
 * Inbox-related mobile API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiInbox mobileApiInbox = new Generator.Builder().build().create(MobileApiInbox.class);
 * }</pre>
 *
 * @see Generator
 * @see Generator.Builder
 */
@HttpRequest("/mobile/{version}/user/{externalUserId}/inbox/gcm/messages")
@Version("1")
public interface MobileApiInbox {
    @HttpRequest(method = HttpMethod.GET)
    FetchInboxResponse fetchInbox(@Path(name="externalUserId") String externalUserId,
                                  @Header(name="Authorization") String accessToken,
                                  @Query(name="from") String from,
                                  @Query(name="to") String to,
                                  @Query(name="topic") String topic,
                                  @Query(name="limit") int limit);
}
