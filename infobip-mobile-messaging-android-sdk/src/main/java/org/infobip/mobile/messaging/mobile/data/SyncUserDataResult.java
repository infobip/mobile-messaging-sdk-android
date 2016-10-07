package org.infobip.mobile.messaging.mobile.data;

import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;

import java.util.Map;

/**
 * @author sslavin
 * @since 15/07/16.
 */
class SyncUserDataResult extends UnsuccessfulResult {
    private final Map<String, Object> predefined;
    private final Map<String, Object> custom;

    SyncUserDataResult(Map<String, Object> predefined, Map<String, Object> custom) {
        super(null);
        this.predefined = predefined;
        this.custom = custom;
    }

    SyncUserDataResult(Throwable exception) {
        super(exception);
        custom = null;
        predefined = null;
    }

    Map<String, Object> getCustom() {
        return custom;
    }

    Map<String, Object> getPredefined() {
        return predefined;
    }
}
