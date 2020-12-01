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
