package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.api.support.MapModel;

import java.util.HashMap;
import java.util.Map;

import static org.infobip.mobile.messaging.UserMapper.customAttsToBackend;

public class CustomAttributeHolder extends MapModel {

    private Map<String, CustomAttributeValue> customAttributes;

    public CustomAttributeHolder() {

    }

    public CustomAttributeHolder(Map<String, CustomAttributeValue> customAttributes) {
        this.customAttributes = customAttributes;
    }

    /// region CUSTOM ATTRIBUTES

    public void setCustomAttributes(Map<String, CustomAttributeValue> customAttributes) {
        this.customAttributes = customAttributes;
        setField(UserAtts.customAttributes, customAttributes != null ? customAttsToBackend(customAttributes) : null);
    }

    public Map<String, CustomAttributeValue> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributeElement(String key, CustomAttributeValue customAttributeValue) {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }
        customAttributes.put(key, customAttributeValue);
        setCustomAttField(key, customAttributeValue);
    }

    public CustomAttributeValue getCustomAttributeValue(String key) {
        if (customAttributes == null) {
            return null;
        }

        return customAttributes.get(key);
    }

    public void removeCustomAttributeElement(String key) {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }
        customAttributes.put(key, null);
        setCustomAttField(key, null);
    }

    /// endregion

    /// region PRIVATE METHODS

    private void setCustomAttField(String key, CustomAttributeValue customAttributeValue) {
        Map<String, Object> customAtts = getField(UserAtts.customAttributes);
        if (customAtts == null) {
            customAtts = new HashMap<>();
        }
        customAtts.put(key, UserMapper.customValueToBackend(customAttributeValue));
        setField(UserAtts.customAttributes, customAtts);
    }

    /// endregion PRIVATE METHODS
}
