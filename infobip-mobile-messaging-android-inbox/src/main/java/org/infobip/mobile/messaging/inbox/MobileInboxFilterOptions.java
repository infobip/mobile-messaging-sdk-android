package org.infobip.mobile.messaging.inbox;

import java.util.Date;

public class MobileInboxFilterOptions {
    private Date fromDateTime;
    private Date toDateTime;
    private String topic;
    private int limit;

    public MobileInboxFilterOptions(Date fromDateTime,
                                    Date toDateTime,
                                    String topic,
                                    int limit) {
        this.fromDateTime = fromDateTime;
        this.toDateTime = toDateTime;
        this.topic = topic;
        this.limit = limit;
    }

    public Date getFromDateTime() {
        return fromDateTime;
    }

    public void setFromDateTime(Date fromDateTime) {
        this.fromDateTime = fromDateTime;
    }

    public Date getToDateTime() {
        return toDateTime;
    }

    public void setToDateTime(Date toDateTime) {
        this.toDateTime = toDateTime;
    }

    public String getTopic() { return topic; }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
