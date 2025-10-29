/*
 * ListCustomAttributeValue.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;


import java.util.List;

public class ListCustomAttributeValue {

    private List<ListCustomAttributeItem> listValue;

    public ListCustomAttributeValue(List<ListCustomAttributeItem> list) {
        this.listValue = list;
    }

    public List<ListCustomAttributeItem> getListValue() {
        return listValue;
    }

}
