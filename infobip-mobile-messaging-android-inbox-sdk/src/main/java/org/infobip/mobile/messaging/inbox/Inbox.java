/*
 * Inbox.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import android.os.Bundle;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

import java.util.List;

/**
 * The class encapsulates user inbox data.
 */
public class Inbox {
    private static final JsonSerializer serializer = new JsonSerializer(false);

    private int countTotal;

    private int countUnread;

    private Integer countTotalFiltered;

    private Integer countUnreadFiltered;

    private List<InboxMessage> messages;

    /**
     * Total number of messages available in the Inbox. Maximum is limited to 100 messages.
     */
    public int getCountTotal() {
        return countTotal;
    }

    public void setCountTotal(int countTotal) {
        this.countTotal = countTotal;
    }

    /**
     * Number of messages that not yet marked as seen/read. See {@link MobileInbox#setSeen(String, String[], MobileMessaging.ResultListener)}.
     */
    public int getCountUnread() {
        return countUnread;
    }

    public void setCountUnread(int countUnread) {
        this.countUnread = countUnread;
    }

    /**
     * Total number of messages that belong to filtered inbox slice.
     */
    public Integer getCountTotalFiltered() {
        return countTotalFiltered;
    }

    public void setCountTotalFiltered(Integer countTotalFiltered) {
        this.countTotalFiltered = countTotalFiltered;
    }

    /**
     * Number of unread messages that belong to filtered inbox slice.
     */
    public Integer getCountUnreadFiltered() {
        return countUnreadFiltered;
    }

    public void setCountUnreadFiltered(Integer countUnreadFiltered) {
        this.countUnreadFiltered = countUnreadFiltered;
    }

    /**
     * Array of inbox messages ordered by message send date-time.
     */
    public List<InboxMessage> getMessages() {
        return messages;
    }

    void setMessages(List<InboxMessage> messages) {
        this.messages = messages;
    }

    public static Inbox createFrom(Bundle bundle) {
        return InboxBundleMapper.inboxFromBundle(bundle);
    }

    @Override
    public String toString() {
        return serializer.serialize(this);
    }

    public Inbox fromString(String inboxDataString) {
        return serializer.deserialize(inboxDataString, Inbox.class);
    }
}
