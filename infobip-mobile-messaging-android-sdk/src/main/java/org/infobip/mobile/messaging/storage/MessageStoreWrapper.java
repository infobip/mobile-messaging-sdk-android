package org.infobip.mobile.messaging.storage;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.Message;

/**
 * Wrappper over existing message store interface.
 * <br>
 * This wrapper is needed to keep calls clean of Context object
 * and to keep compatibility with old interface.
 * Used internally in the library.
 *
 * @author sslavin
 * @since 29/08/2017.
 */

public interface MessageStoreWrapper {
    /**
     * Saves or creates message in message store.
     * Replaces message on conflict.
     *
     * @param message message to save or create.
     */
    void upsert(@NonNull Message message);

    /**
     * Saves or creates multiple messages in message store.
     * Replaces message on conflict.
     *
     * @param messages messages to save or create.
     */
    void upsert(@NonNull Message[] messages);
}