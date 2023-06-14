package org.infobip.mobile.messaging.chat.core;

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
    void chatViewChanged(InAppChatWidgetView view);

}
