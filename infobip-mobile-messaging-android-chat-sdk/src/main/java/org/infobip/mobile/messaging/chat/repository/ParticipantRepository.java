package org.infobip.mobile.messaging.chat.repository;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public interface ParticipantRepository {

    /**
     * Returns all known participants
     *
     * @return list of participants
     */
    List<Participant> findAll();

    /**
     * Returns participant based on provided id
     *
     * @param id id of participant
     * @return participant
     */
    Participant findOne(@NonNull String id);

    /**
     * Find author of message
     *
     * @param message message to look author for
     * @return participant
     */
    Participant findAuthor(@NonNull Message message);

    /**
     * Saves participant to database
     *
     * @param participant data to save
     */
    void upsert(@NonNull Participant participant);

    /**
     * Clears repository and removes all participants
     */
    void clear();
}
