package org.infobip.mobile.messaging.tasks;

import org.infobip.mobile.messaging.SystemData;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemDataReportResult extends UnsuccessfulResult {

    SystemData data = null;

    public SystemDataReportResult(SystemData data) {
        super(null);
        this.data = data;
    }

    public SystemDataReportResult(Throwable exception) {
        super(exception);
    }

    public SystemData getData() {
        return data;
    }
}
