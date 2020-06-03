package org.infobip.mobile.messaging.chat.core;

/**
 * Declaration of interaction with client-side.
 */
public interface InAppChatClient {

    /**
     * Send typed message to agent from user
     *
     * @param message user message
     */
    void sendChatMessage(String message);
}
