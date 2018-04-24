package org.infobip.mobile.messaging.chat.broadcast;

import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatParticipant;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public interface ChatBroadcaster {

    /**
     * Sends broadcast when chat message is received
     *
     * @param message message to send broadcast for
     */
    void chatMessageReceived(ChatMessage message);

    /**
     * Sends broadcast when chat message is sent out
     *
     * @param message message to send broadcast for
     */
    void chatMessageSent(ChatMessage message);

    /**
     * Sends broadcast when chat message is tapped
     *
     * @param message message to send broadcast for
     */
    void chatMessageTapped(ChatMessage message);

    /**
     * Sends broadcast when action tapped
     *
     * @param message message to send broadcast for
     * @param actionId tapped action id
     */
    void chatMessageViewActionTapped(ChatMessage message, String actionId);

    /**
     * Sends broadcast when user information is saved on server
     *
     * @param participant user information
     */
    void userInfoSynchronized(ChatParticipant participant);
}
