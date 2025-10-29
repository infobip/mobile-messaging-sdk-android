/*
 * UserCustomEventBody.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.appinstance;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCustomEventBody {

    private static final JsonSerializer serializer = new JsonSerializer(false);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomEvent {
        private String definitionId;
        private String date;
        private Map<String, Object> properties;
    }

    /**
     * Array of custom events to report.
     */
    private CustomEvent[] events;

    @Override
    public String toString() {
        return serializer.serialize(this);
    }
}
