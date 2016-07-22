package org.infobip.mobile.messaging.tasks;

import java.util.Map;

/**
 * @author sslavin
 * @since 15/07/16.
 */
public class SyncUserDataResult extends UnsuccessfulResult {
    private final Map<String, Object> predefined;
    private final Map<String, Object> custom;

    public SyncUserDataResult(Map<String, Object> predefined, Map<String, Object> custom) {
        super(null);
        this.predefined = predefined;
        this.custom = custom;
    }

    public SyncUserDataResult(Throwable exception) {
        super(exception);
        custom = null;
        predefined = null;
    }

    public Map<String, Object> getCustom() {
        return custom;
    }

    public Map<String, Object> getPredefined() {
        return predefined;
    }
}
