package org.infobip.mobile.messaging.mobile.data;

import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;

/**
 * @author sslavin
 * @since 25/08/16.
 */
class SystemDataReportResult extends UnsuccessfulResult {

    private SystemData data = null;
    private boolean postponed = false;

    SystemDataReportResult(SystemData data) {
        super(null);
        this.data = data;
        this.postponed = false;
    }

    SystemDataReportResult(SystemData data, boolean postponed) {
        super(null);
        this.data = data;
        this.postponed = postponed;
    }

    SystemDataReportResult(Throwable exception) {
        super(exception);
    }

    public SystemData getData() {
        return data;
    }

    boolean isPostponed() {
        return postponed;
    }
}
