package org.infobip.mobile.messaging.inbox;

public enum MobileInboxEvent {
    /**
     * It is triggered when Inbox messages fetched
     */
    INBOX_MESSAGES_FETCHED("org.infobip.mobile.messaging.inbox.INBOX_MESSAGES_FETCHED"),
    INBOX_COUNT_UNREAD("org.infobip.mobile.messaging.inbox.INBOX_COUNT_UNREAD"),
    INBOX_COUNT_TOTAL("org.infobip.mobile.messaging.inbox.INBOX_COUNT_TOTAL");

    private final String key;

    MobileInboxEvent(String key) { this.key = key; }

    public String getKey() { return key; }
}
