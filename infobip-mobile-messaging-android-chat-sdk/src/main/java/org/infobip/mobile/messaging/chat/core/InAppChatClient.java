package org.infobip.mobile.messaging.chat.core;

import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachment;

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

    /**
     * Send typed message and attachment
     * @param message user message
     * @param attachment IMAGE, VIDEO, DOCUMENT
     */
    void sendChatMessage(String message, InAppChatAttachment attachment);
}
