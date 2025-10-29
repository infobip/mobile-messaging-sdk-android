/*
 * MobileApiInbox.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.inbox;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
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
@HttpRequest("/mobile/{version}")
public interface MobileApiInbox {
    @Version("1")
    @HttpRequest(method = HttpMethod.GET, value = "user/{externalUserId}/inbox/gcm/messages")
    FetchInboxResponse fetchInbox(@Path(name = "externalUserId") String externalUserId,
                                  @Header(name = "Authorization") String accessToken,
                                  @Query(name = "dateTimeFrom") String from,
                                  @Query(name = "dateTimeTo") String to,
                                  @Query(name = "messageTopic") String topic,
                                  @Query(name = "limit") Integer limit);

    @Version("1")
    @HttpRequest(method = HttpMethod.GET, value = "user/{externalUserId}/inbox/{cloudType}/messages")
    FetchInboxResponse fetchInbox(@Path(name = "externalUserId") String externalUserId,
                                  @Header(name = "Authorization") String accessToken,
                                  @Query(name = "dateTimeFrom") String from,
                                  @Query(name = "dateTimeTo") String to,
                                  @Query(name = "messageTopic") String topic,
                                  @Query(name = "limit") Integer limit,
                                  @Path(name = "cloudType") String cloudType);

    @Version("2")
    @ApiKey("${api.key}")
    @HttpRequest(method = HttpMethod.POST, value = "messages/seen")
    void reportSeen(@Body() InboxSeenMessages seenReport);
}
