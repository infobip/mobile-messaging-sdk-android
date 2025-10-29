/*
 * ApiClient.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.http.client;

import org.infobip.mobile.messaging.api.support.Tuple;

import java.util.Collection;
import java.util.Map;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
public interface ApiClient {
    <B, R> R execute(HttpMethod method, String uri, String apiKey, Tuple<String, String> credentials, Map<String, Collection<Object>> queryParams, Map<String, Collection<Object>> headers, B body, Class<R> returnType);
}
