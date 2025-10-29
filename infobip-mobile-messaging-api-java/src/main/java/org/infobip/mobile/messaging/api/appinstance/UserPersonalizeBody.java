/*
 * UserPersonalizeBody.java
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
public class UserPersonalizeBody {

    private static final JsonSerializer serializer = new JsonSerializer(false);

    private Map<String, Object> userIdentity;
    private Map<String, Object> userAttributes;

    @Override
    public String toString() {
        return serializer.serialize(this);
    }
}
