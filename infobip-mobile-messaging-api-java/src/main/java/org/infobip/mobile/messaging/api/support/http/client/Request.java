/*
 * Request.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.http.client;

import org.infobip.mobile.messaging.api.support.Tuple;

import java.util.Collection;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author sslavin
 * @since 27/11/2017.
 */

@Data
@AllArgsConstructor
public class Request {
    protected HttpMethod httpMethod;
    protected String uri;
    protected String apiKey;
    protected Tuple<String, String> credentials;
    protected Map<String, Collection<Object>> headers;
    protected Map<String, Collection<Object>> queryParams;
    protected Object body;
}
