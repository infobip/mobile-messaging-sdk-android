/*
 * VersionCheckResult.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.version;

import org.infobip.mobile.messaging.api.version.LatestReleaseResponse;
import org.infobip.mobile.messaging.mobileapi.UnsuccessfulResult;

/**
 * @author sslavin
 * @since 04/10/2016.
 */

class VersionCheckResult extends UnsuccessfulResult {

    private String version;
    private String updateUrl;

    VersionCheckResult(Throwable exception) {
        super(exception);
    }

    VersionCheckResult(LatestReleaseResponse latestReleaseResponse) {
        super(null);
        version = latestReleaseResponse.getLibraryVersion();
        updateUrl = latestReleaseResponse.getUpdateUrl();
    }

    public String getVersion() {
        return version;
    }

    String getUpdateUrl() {
        return updateUrl;
    }
}
