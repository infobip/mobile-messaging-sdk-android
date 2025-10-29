/*
 * Tuple.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support;

import lombok.Data;

/**
 * @author mstipanov
 * @since 17.03.2016.
 */
@Data
public class Tuple<L,R> {
    private final L left;
    private final R right;
}
