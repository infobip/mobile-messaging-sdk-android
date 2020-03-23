package org.infobip.mobile.messaging.mobile.events;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.CustomAttributeValue;
import org.infobip.mobile.messaging.api.appinstance.UserCustomEventAtts;

import java.util.Map;

public class CustomEvent extends EventPropertiesHolder {

    public CustomEvent() {
    }

    public CustomEvent(@NonNull String definitionId) {
        setDefinitionId(definitionId);
    }

    public CustomEvent(@NonNull String definitionId, Map<String, CustomAttributeValue> eventProperties) {
        super(eventProperties);
        setDefinitionId(definitionId);
    }

    public String definitionId;

    public String getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(String definitionId) {
        this.definitionId = definitionId;
        setField(UserCustomEventAtts.definitionId, definitionId);
    }
}
