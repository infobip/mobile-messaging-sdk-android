/*
 * RequestInterceptor.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.http.client;

/**
 * @author sslavin
 * @since 27/11/2017.
 */

public interface RequestInterceptor {
    Request intercept(Request request);
}
