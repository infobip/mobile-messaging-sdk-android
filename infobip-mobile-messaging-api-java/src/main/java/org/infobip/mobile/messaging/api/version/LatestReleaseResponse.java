/*
 * LatestReleaseResponse.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.version;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Latest release version response.
 *
 * @author sslavin
 * @see MobileApiVersion
 * @see MobileApiVersion#getLatestRelease()
 * @since 03.10.2016.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatestReleaseResponse {
    String platformType;
    String libraryVersion;
    String updateUrl;
}
