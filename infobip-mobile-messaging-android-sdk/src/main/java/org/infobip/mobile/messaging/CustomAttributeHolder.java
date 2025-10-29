/*
 * CustomAttributeHolder.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.api.support.MapModel;

import java.util.HashMap;
import java.util.Map;

import static org.infobip.mobile.messaging.CustomAttributesMapper.customAttsToBackend;
import static org.infobip.mobile.messaging.CustomAttributesMapper.customValueToBackend;


public class CustomAttributeHolder extends MapModel {

    protected Map<String, CustomAttributeValue> customAttributes;

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

    public void setCustomAttribute(String key, CustomAttributeValue customAttributeValue) {
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

    public void removeCustomAttribute(String key) {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }
        customAttributes.put(key, null);
        setCustomAttField(key, null);
    }

    /// endregion

    /// region PRIVATE METHODS

    protected void setCustomAttField(String key, CustomAttributeValue customAttributeValue) {
        Map<String, Object> customAtts = getField(UserAtts.customAttributes);
        if (customAtts == null) {
            customAtts = new HashMap<>();
        }
        customAtts.put(key, customValueToBackend(customAttributeValue));
        setField(UserAtts.customAttributes, customAtts);
    }

    /// endregion PRIVATE METHODS
}
