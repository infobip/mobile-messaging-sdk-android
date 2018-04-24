package org.infobip.mobile.messaging.chat.broadcast;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.dal.bundle.BundleMapper;

import static android.support.annotation.VisibleForTesting.PACKAGE_PRIVATE;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class ChatBundleMapper extends BundleMapper {

    private static final String BUNDLED_MESSAGE_TAG = ChatBundleMapper.class.getName() + ".message";
    private static final String BUNDLED_PARTICIPANT_TAG = ChatBundleMapper.class.getName() + ".participant";

    /**
     * Converts chat message to bundle
     * @param message chat message
     * @return resulting bundle
     */
    @NonNull
    public Bundle chatMessageToBundle(@NonNull ChatMessage message) {
        return objectToBundle(message, BUNDLED_MESSAGE_TAG);
    }

    /**
     * Converts bundle to chat message
     * @param bundle bundle with data
     * @return chat message
     */
    @Nullable
    public ChatMessage chatMessageFromBundle(Bundle bundle) {
        return objectFromBundle(bundle, BUNDLED_MESSAGE_TAG, ChatMessage.class);
    }

    /**
     * Converts chat participant to bundle
     * @param participant participant information
     * @return resuting bundle
     */
    @NonNull
    @VisibleForTesting(otherwise = PACKAGE_PRIVATE)
    public Bundle chatParticipantToBundle(@NonNull ChatParticipant participant) {
        return objectToBundle(participant, BUNDLED_PARTICIPANT_TAG);
    }

    /**
     * Converts bundle to chat participant
     * @param bundle bundle with data
     * @return chat participant
     */
    @Nullable
    public ChatParticipant chatParticipantFromBundle(Bundle bundle) {
        return objectFromBundle(bundle, BUNDLED_PARTICIPANT_TAG, ChatParticipant.class);
    }
}
