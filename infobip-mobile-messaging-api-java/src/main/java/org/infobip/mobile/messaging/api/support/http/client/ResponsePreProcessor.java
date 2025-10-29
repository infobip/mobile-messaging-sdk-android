/*
 * ResponsePreProcessor.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.http.client;

import java.util.List;
import java.util.Map;

/**
 * @author sslavin
 * @since 27/11/2017.
 */

public interface ResponsePreProcessor {
    void beforeResponse(int responseCode, Map<String, List<String>> headers);
    void beforeResponse(Exception error);
}
