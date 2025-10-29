/*
 * MobileApiAppInstance.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.appinstance;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Path;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

import java.util.Map;

/**
 * User and app instance related mobile API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiAppInstance mobileApiAppInstance = new Generator.Builder().build().create(MobileApiAppInstance.class);
 * }</pre>
 *
 * @see Generator
 * @see Generator.Builder
 */
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}/appinstance")
@Version("1")
public interface MobileApiAppInstance {

    @HttpRequest(method = HttpMethod.POST)
    @Query(name = "ri", value = "true")
    AppInstance createInstance(@Body AppInstance instance);

    @HttpRequest(method = HttpMethod.PATCH, value = "{regId}")
    void patchInstance(@Path(name = "regId") String regId,
                       @Body Map instance);

    @HttpRequest(method = HttpMethod.GET, value = "{regId}")
    AppInstance getInstance(@Path(name = "regId") String regId);

    @HttpRequest(method = HttpMethod.POST, value = "{regId}/depersonalize")
    void depersonalize(@Path(name = "regId") String regId);

    @HttpRequest(method = HttpMethod.POST, value = "{regId}/user/events/session")
    void sendUserSessionReport(@Path(name = "regId") String regId,
                               @Body UserSessionEventBody userSessionEventBody);

    @HttpRequest(method = HttpMethod.POST, value = "{regId}/user/events/custom")
    void sendUserCustomEvents(@Path(name = "regId") String regId,
                              @Query(name = "validate", value = "false") boolean validate,
                              @Body UserCustomEventBody userCustomEventBody);

    @HttpRequest(method = HttpMethod.GET, value = "{regId}/user/livechatinfo")
    LivechatContactInformation getLivechatContactInformation(@Path(name = "regId") String regId);
}
