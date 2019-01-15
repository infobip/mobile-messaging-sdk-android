package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.api.support.MapModel;

import java.util.HashMap;
import java.util.Map;

import static org.infobip.mobile.messaging.UserDataMapper.customAttsToBackend;

public class CustomAttributeHolder extends MapModel {

    private Map<String, CustomUserDataValue> customAttributes;

    public CustomAttributeHolder() {

    }

    public CustomAttributeHolder(Map<String, CustomUserDataValue> customAttributes) {
        this.customAttributes = customAttributes;
    }

    /// region CUSTOM ATTRIBUTES

    public void setCustomAttributes(Map<String, CustomUserDataValue> customAttributes) {
        this.customAttributes = customAttributes;
        setField(UserAtts.customAttributes, customAttributes != null ? customAttsToBackend(customAttributes) : null);
    }

    public Map<String, CustomUserDataValue> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomUserDataElement(String key, CustomUserDataValue customUserDataValue) {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }
        customAttributes.put(key, customUserDataValue);
        setCustomAttField(key, customUserDataValue);
    }

    public CustomUserDataValue getCustomUserDataValue(String key) {
        if (customAttributes == null) {
            return null;
        }

        return customAttributes.get(key);
    }

    public void removeCustomUserDataElement(String key) {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }
        customAttributes.put(key, null);
        setCustomAttField(key, null);
    }

    /// endregion

    /// region PRIVATE METHODS

    private void setCustomAttField(String key, CustomUserDataValue customUserDataValue) {
        Map<String, Object> customAtts = getField(UserAtts.customAttributes);
        if (customAtts == null) {
            customAtts = new HashMap<>();
        }
        customAtts.put(key, UserDataMapper.customValueToBackend(customUserDataValue));
        setField(UserAtts.customAttributes, customAtts);
    }

    /// endregion PRIVATE METHODS
}
