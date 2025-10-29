/*
 * UserCustomAttributeHolder.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserCustomAttributeHolder extends CustomAttributeHolder {

    public UserCustomAttributeHolder(Map<String, CustomAttributeValue> customAttributes) {
        super(customAttributes);
    }

    public UserCustomAttributeHolder() {

    }

    public void setListCustomAttribute(String key, ListCustomAttributeValue customList) {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }

        super.setCustomAttribute(key, new CustomAttributeValue(customList.getListValue()));
    }

    public List<ListCustomAttributeItem> getListCustomAttributeItems(String key) {
        if (customAttributes == null) {
            return null;
        }

        CustomAttributeValue value = customAttributes.get(key);
        if (value == null) {
            return null;
        }
        List<Map<String, Object>> listMapValue = value.listMapValue();

        ListCustomAttributeItem.Builder customMapBuilder = ListCustomAttributeItem.builder();

        for (Map<String, Object> map: listMapValue) {
            for (Map.Entry<String, Object> entry: map.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    customMapBuilder.putNumber(entry.getKey(), (Number) entry.getValue());
                } else if (entry.getValue() instanceof String) {
                    customMapBuilder.putString(entry.getKey(), (String) entry.getValue());
                } else if (entry.getValue() instanceof Date) {
                    customMapBuilder.putDate(entry.getKey(), (Date) entry.getValue());
                } else if (entry.getValue() instanceof CustomAttributeValue.DateTime) {
                    customMapBuilder.putDateTime(entry.getKey(), (CustomAttributeValue.DateTime) entry.getValue());
                } else  {
                    customMapBuilder.putBoolean(entry.getKey(), (Boolean) entry.getValue());
                }
            }
        }
        List<ListCustomAttributeItem> items = new ArrayList<>();
        items.add(customMapBuilder.build());
        return items;
    }

}
