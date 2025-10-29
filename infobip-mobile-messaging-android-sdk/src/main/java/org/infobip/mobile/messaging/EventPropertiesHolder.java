/*
 * EventPropertiesHolder.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserCustomEventAtts;
import org.infobip.mobile.messaging.api.support.MapModel;

import java.util.HashMap;
import java.util.Map;

import static org.infobip.mobile.messaging.EventPropertiesMapper.eventPropertiesToBackend;
import static org.infobip.mobile.messaging.EventPropertiesMapper.eventPropertyToBackend;


public class EventPropertiesHolder extends MapModel {

    private Map<String, CustomAttributeValue> properties;

    public EventPropertiesHolder() {

    }

    public EventPropertiesHolder(Map<String, CustomAttributeValue> properties) {
        this.properties = properties;
    }

    /// region EVENT PROPERTIES

    public void setProperties(Map<String, CustomAttributeValue> properties) {
        this.properties = properties;
        setField(UserCustomEventAtts.properties, properties != null ? eventPropertiesToBackend(properties) : null);
    }

    public Map<String, CustomAttributeValue> getProperties() {
        return properties;
    }

    public void setProperty(String key, CustomAttributeValue eventProperty) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, eventProperty);
        setPropertyField(key, eventProperty);
    }

    public CustomAttributeValue getPropertyValue(String key) {
        if (properties == null) {
            return null;
        }

        return properties.get(key);
    }

    public void removePropertyValue(String key) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.remove(key);
        setPropertyField(key, null);
    }

    /// endregion

    /// region PRIVATE METHODS

    private void setPropertyField(String key, CustomAttributeValue propertyValue) {
        Map<String, Object> eventProperties = getField(UserCustomEventAtts.properties);
        if (eventProperties == null) {
            eventProperties = new HashMap<>();
        }
        eventProperties.put(key, eventPropertyToBackend(propertyValue));
        setField(UserCustomEventAtts.properties, eventProperties);
    }

    /// endregion PRIVATE METHODS
}
