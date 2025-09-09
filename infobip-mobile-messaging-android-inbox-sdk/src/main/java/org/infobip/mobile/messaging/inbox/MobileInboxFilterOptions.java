package org.infobip.mobile.messaging.inbox;

import java.util.Date;
import java.util.List;

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
     * topics - messages' topics to be fetched
     */
    private List<String> topics;
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

    public MobileInboxFilterOptions(Date fromDateTime,
                                    Date toDateTime,
                                    List<String> topics,
                                    Integer limit) {
        this.fromDateTime = fromDateTime;
        this.toDateTime = toDateTime;
        this.topics = topics;
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
        this.topics = null;
    }

    public List<String> getTopics() { return topics; }

    public void setTopics(List<String> topics) {
        this.topics = topics;
        this.topic = null;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
