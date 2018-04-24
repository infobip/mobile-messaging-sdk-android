package org.infobip.mobile.messaging.chat;

import java.util.List;

/**
 * Set of interfaces to access chat messages stored on a device
 *
 * @author sslavin
 * @since 17/10/2017.
 */

public interface ChatMessageStorage {

    /**
     * Set of callback methods for chat message storage
     */
    interface Listener {
        /**
         * Called when new message is added to the storage.
         * Invoked after {@link ChatMessageStorage#save(ChatMessage)}} for new message.
         * @param message new message
         */
        void onNew(ChatMessage message);

        /**
         * Called when message is updated.
         * Invoked after {@link ChatMessageStorage#save(ChatMessage)}}.
         * @param message message that was updated in storage
         */
        void onUpdated(ChatMessage message);

        /**
         * Called when messages are deleted in storage.
         * Invoked after {@link ChatMessageStorage#delete(String)}.
         * Is not invoked after {@link ChatMessageStorage#deleteAll()}}.
         * @param messageId ids of messages that were deleted
         */
        void onDeleted(String messageId);

        /**
         * Called when all messages are deleted in message storage.
         * Invoked after {@link ChatMessageStorage#deleteAll()}}.
         */
        void onAllDeleted();
    }

    /**
     * Returnes all received chat messages
     * @return list if messages
     */
    List<ChatMessage> findAllMessages();

    /**
     * Returns number of messages stored locally
     * @return number of messages
     */
    long countAllMessages();

    /**
     * Returns number of all unread messages stored locally
     * @return number of messages
     */
    long countAllUnreadMessages();

    /**
     * Returns message based on id
     * @param id id of message
     * @return chat message
     */
    ChatMessage findMessage(String id);

    /**
     * Saves message to storage
     * @param message message ot save
     */
    void save(ChatMessage message);

    /**
     * Deletes message by id
     * @param id id of message
     */
    void delete(String id);

    /**
     * Deletes all messages
     */
    void deleteAll();

    /**
     * Registers listener for message storage updates
     * @param listener listener callback
     */
    void registerListener(Listener listener);

    /**
     * Unregisters listener from message storage updates
     * @param listener listener callback
     */
    void unregisterListener(Listener listener);
}
