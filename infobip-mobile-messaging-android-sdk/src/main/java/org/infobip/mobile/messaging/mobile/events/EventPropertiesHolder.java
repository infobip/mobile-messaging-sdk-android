package org.infobip.mobile.messaging.mobile.events;

import org.infobip.mobile.messaging.CustomAttributeValue;
import org.infobip.mobile.messaging.UserMapper;
import org.infobip.mobile.messaging.api.appinstance.UserCustomEventAtts;
import org.infobip.mobile.messaging.api.support.MapModel;

import java.util.HashMap;
import java.util.Map;

import static org.infobip.mobile.messaging.UserMapper.customAttsToBackend;

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
        setField(UserCustomEventAtts.properties, properties != null ? customAttsToBackend(properties) : null);
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
        properties.put(key, null);
        setPropertyField(key, null);
    }

    /// endregion

    /// region PRIVATE METHODS

    private void setPropertyField(String key, CustomAttributeValue propertyValue) {
        Map<String, Object> eventProperties = getField(UserCustomEventAtts.properties);
        if (eventProperties == null) {
            eventProperties = new HashMap<>();
        }
        eventProperties.put(key, UserMapper.customValueToBackend(propertyValue));
        setField(UserCustomEventAtts.properties, eventProperties);
    }

    /// endregion PRIVATE METHODS
}
