/*
 * MobileInboxFilterOptions.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
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
     * Nonempty lists are filtered client-side over up to 1,000 fetched messages,
     * including single-element lists. Use {@link #setTopic(String)} for backend filtering
     * and cursor pagination.
     */
    private List<String> topics;
    /**
     * limit - number of messages to be fetched
     */
    private Integer limit;
    /**
     * Opaque cursor returned as {@link Inbox#getNextCursor()} by the previous fetch.
     */
    private String cursor;

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
        setTopics(topics);
        this.limit = limit;
    }

    /**
     * Creates server-side filter options for a page of inbox messages.
     * Keep {@code fromDateTime}, {@code toDateTime}, and {@code topic} unchanged between pages.
     *
     * @param cursor {@link Inbox#getNextCursor()} from the previous page, or {@code null} for the first page
     */
    public MobileInboxFilterOptions(Date fromDateTime,
                                    Date toDateTime,
                                    String topic,
                                    Integer limit,
                                    String cursor) {
        this(fromDateTime, toDateTime, topic, limit);
        this.cursor = cursor;
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

    /**
     * Sets legacy client-side topic filtering, even when the list contains only one topic.
     * A null or nonpositive limit does not truncate the locally filtered results.
     * Use {@link #setTopic(String)} instead to enable backend filtering and cursor pagination.
     */
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

    public String getCursor() {
        return cursor;
    }

    /**
     * Sets the cursor for backend pagination. Null, empty, or whitespace-only cursors
     * request the first page. Nonblank cursors are sent unchanged as opaque values.
     * A nonblank cursor cannot be combined with
     * a nonempty {@link #getTopics()} list; use the singular {@link #setTopic(String)} option.
     */
    public void setCursor(String cursor) {
        this.cursor = cursor;
    }
}
