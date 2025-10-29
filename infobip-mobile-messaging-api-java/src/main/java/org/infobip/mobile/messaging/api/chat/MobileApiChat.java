/*
 * MobileApiChat.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.chat;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Chat-related mobile API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiChat mobileApiChat = new Generator.Builder().build().create(MobileApiChat.class);
 * }</pre>
 *
 * @see Generator
 * @see Generator.Builder
 */
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}/chat")
@Version("1")
public interface MobileApiChat {

    @HttpRequest(method = HttpMethod.GET, value = "widget")
    WidgetInfo getWidgetConfiguration();
}