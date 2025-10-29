/*
 * MobileInboxBroadcaster.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

public interface MobileInboxBroadcaster {
    /**
     * Sends broadcast that Inbox messages are fetched
     *
     * @param inbox inbox object received
     */
    void inboxFetched(Inbox inbox);

    /**
     * Sends broadcast with message ids which were reported as seen by the library
     *
     * @param messageIDs ids of inbox messages marked as seen
     */
    void seenReported(String... messageIDs);
}
