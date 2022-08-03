package org.infobip.mobile.messaging.inbox;

import java.util.Date;

/**
 * Class with filtering options for fetching inbox
 */

public class MobileInboxFilterOptions {
    /**
     * fromDateTime - date time from which Inbox will be fetched
     */
    private Date fromDateTime;
    /**
     * toDateTime - date time to which Inbox will be fetched
     */
    private Date toDateTime;
    /**
     * topic - messages' topic to be fetched
     */
    private String topic;
    /**
     * limit - number of messages to be fetched
     */
    private Integer limit;

    public MobileInboxFilterOptions(Date fromDateTime,
                                    Date toDateTime,
                                    String topic,
                                    Integer limit) {
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

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
