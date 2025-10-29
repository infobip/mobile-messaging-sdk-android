/*
 * ApiError.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.http.client.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private ApiServiceException serviceException;
}
