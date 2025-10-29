/*
 * CustomEvent.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserCustomEventAtts;

import java.util.Map;

import androidx.annotation.NonNull;

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
