/*
 * TokenBody.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.rtc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TokenBody {
    private final String identity;
    private final Long timeToLive;

    @Override
    public String toString() {
        return "TokenBody{" +
                "identity='" + identity + '\'' +
                ", timeToLive=" + timeToLive +
                '}';
    }
}
