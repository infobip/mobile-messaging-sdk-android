package org.infobip.mobile.messaging.chat;

/**
 * Contains all chat-related events produced by the library.
 *
 * @author sslavin
 * @since 06/10/2017.
 */

public enum ChatEvent {

    /**
     * Triggered when chat message is received from the server.
     * <pre>
     * {@code
     * ChatMessage message = ChatMessage.createFrom(intent);
     * }
     * </pre>
     */
    CHAT_MESSAGE_RECEIVED("org.infobip.mobile.messaging.chat.CHAT_MESSAGE_RECEIVED"),

    /**
     * Triggered when chat message is successfully sent to the server.
     * <pre>
     * {@code
     * ChatMessage message = ChatMessage.createFrom(intent);
     * }
     * </pre>
     */
    CHAT_MESSAGE_SENT("org.infobip.mobile.messaging.chat.CHAT_MESSAGE_SENT"),

    /**
     * Triggered when chat message is tapped.
     * <pre>
     * {@code
     * ChatMessage message = ChatMessage.createFrom(intent);
     * }
     * </pre>
     */
    CHAT_MESSAGE_TAPPED("org.infobip.mobile.messaging.chat.CHAT_MESSAGE_TAPPED"),

    /**
     * Triggered when action tapped inside chat view.
     * <pre>
     * {@code
     * ChatMessage message = ChatMessage.createFrom(intent);
     * String actionId = intent.getStringExtra(MobileChat.EXTRA_ACTION_ID);
     * }
     * </pre>
     */
    CHAT_MESSAGE_VIEW_ACTION_TAPPED("org.infobip.mobile.messaging.chat.CHAT_MESSAGE_VIEW_ACTION_TAPPED"),

    /**
     * Triggered when information of current user is saved on server.
     * <pre>
     * {@code
     * ChatParticipant info = ChatParticipant.createFrom(intent);
     * }
     * </pre>
     */
    CHAT_USER_INFO_SYNCHRONIZED("org.infobip.mobile.messaging.chat.CHAT_USER_INFO_SYNCHRONIZED");

    private final String key;

    ChatEvent(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
