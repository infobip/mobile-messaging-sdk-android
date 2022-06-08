package org.infobip.mobile.messaging.inbox;

public interface MobileInboxBroadcaster {
    /**
     * Sends broadcast that Inbox messages are fetched
     */
    void inboxFetched(Inbox inbox);
}
