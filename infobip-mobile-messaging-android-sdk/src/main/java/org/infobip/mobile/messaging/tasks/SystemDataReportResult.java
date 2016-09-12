package org.infobip.mobile.messaging.tasks;

import org.infobip.mobile.messaging.SystemData;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemDataReportResult extends UnsuccessfulResult {

    SystemData data = null;
    boolean postponed = false;

    public SystemDataReportResult(SystemData data) {
        super(null);
        this.data = data;
        this.postponed = false;
    }

    public SystemDataReportResult(SystemData data, boolean postponed) {
        super(null);
        this.data = data;
        this.postponed = postponed;
    }

    public SystemDataReportResult(Throwable exception) {
        super(exception);
    }

    public SystemData getData() {
        return data;
    }

    public boolean isPostponed() {
        return postponed;
    }
}
