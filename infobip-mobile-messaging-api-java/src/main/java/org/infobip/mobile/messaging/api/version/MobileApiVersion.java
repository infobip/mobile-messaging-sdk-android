/*
 * MobileApiVersion.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.version;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Mobile API to retrieve details of latest library release.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiVersion mobileApiLibraryVersion = new Generator.Builder().build().create(MobileApiVersion.class);
 * }</pre>
 *
 * @author sslavin
 * @see Generator
 * @see Generator.Builder
 * @since 03.10.2016.
 */
@Version("3")
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}")
public interface MobileApiVersion {

    /**
     * Retrieves latest library release version from the server.
     *
     * @return {@link LatestReleaseResponse}
     */
    @HttpRequest(method = HttpMethod.GET, value = "version")
    @Query(name = "platformType", value = "${platform.type:GCM}")
    LatestReleaseResponse getLatestRelease();
}
