/*
 * MobileApiBaseUrl.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
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
@HttpRequest("/mobile/{version}/baseurl")
@Version("1")
public interface MobileApiBaseUrl {

    @HttpRequest(method = HttpMethod.GET)
    BaseUrlResponse getBaseUrl();
}