/*
 * InAppChatBroadcaster.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core;

import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView;

public interface InAppChatBroadcaster {

    /**
     * Sends broadcast that In-app chat widget configuration is synced
     */
    void chatConfigurationSynced();

    /**
     * Called whenever a new chat push message arrives, contains current unread message counter value
     * @param unreadMessagesCount new unread message count
     */
    void unreadMessagesCounterUpdated(int unreadMessagesCount);

    /**
     * Sends broadcast when In-app chat widget view is changed
     */
    void chatViewChanged(LivechatWidgetView view);

    /**
     * Sends broadcast when Livechat registration id is updated
     */
    void livechatRegistrationIdUpdated(String livechatRegistrationId);

    /**
     * Sends broadcast with new In-app chat's availability
     * @param isChatAvailable true if In-app chat is ready to be presented to the user, false otherwise
     */
    void chatAvailabilityUpdated(boolean isChatAvailable);
}
