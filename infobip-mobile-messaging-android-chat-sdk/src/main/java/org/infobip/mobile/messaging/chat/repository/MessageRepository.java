package org.infobip.mobile.messaging.chat.repository;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.infobip.mobile.messaging.dal.sqlite.PrimaryKeyViolationException;

import java.util.List;
import java.util.Set;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public interface MessageRepository {

    /**
     * Returns all messages
     * @return list of all messages
     */
    @NonNull
    List<Message> findAll();

    /**
     * Returns count of all stored messages
     * @return number of stored messages
     */
    long countAll();

    /**
     * Returns count of all unread messages
     * @return number of unread messages
     */
    long countAllUnread();

    /**
     * Finds one message by id
     * @param id message id
     * @return message or null
     */
    @Nullable
    Message findOne(@NonNull String id);

    /**
     * Saves provided messages to database
     * @param message message to save to repository
     */
    void upsert(Message message);

    /**
     * Inserts new message or throws exception
     * @param message message to insert
     * @throws PrimaryKeyViolationException exception when same message already exists in database
     */
    void insert(Message message) throws PrimaryKeyViolationException;

    /**
     * Finds message and marks it read with the provided time if message is present
     * @param id message id
     * @param time timestamp ms
     * @return updated message
     */
    @Nullable
    Message markRead(String id, long time);

    /**
     * Finds all messages and marks them read with the provided time.
     * Will keep existing read timestamps untouched.
     * @param time timestamp ms
     * @return returns set of ids of messages that were marked read
     */
    Set<String> markAllMessagesRead(long time);

    /**
     * Removes messages from database using provided ids
     * @param ids set of ids to remove messages for
     */
    void remove(String... ids);

    /**
     * Clears repository and removes all messages
     */
    void clear();
}
