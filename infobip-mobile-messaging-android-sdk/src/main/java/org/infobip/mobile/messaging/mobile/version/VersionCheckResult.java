package org.infobip.mobile.messaging.mobile.version;

import org.infobip.mobile.messaging.api.version.LatestReleaseResponse;
import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;

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
