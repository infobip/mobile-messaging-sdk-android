package org.infobip.mobile.messaging.chat.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.chat.repository.Participant;
import org.infobip.mobile.messaging.chat.repository.ParticipantRepository;
import org.infobip.mobile.messaging.chat.repository.ParticipantRepositoryImpl;
import org.infobip.mobile.messaging.chat.repository.RepositoryMapper;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import static android.support.annotation.VisibleForTesting.PACKAGE_PRIVATE;

/**
 * @author sslavin
 * @since 10/10/2017.
 */

@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
public class UserProfileManager {

    private final String PARTICIPANT_ID_TAG = "org.infobip.mobile.messaging.chat.PARTICIPANT_ID_TAG";

    private final ParticipantRepository participantRepository;
    private final RepositoryMapper repositoryMapper;
    private final SharedPreferences sharedPreferences;
    private final String pushRegistrationId;

    UserProfileManager(Context context) {
        this.participantRepository = new ParticipantRepositoryImpl(context);
        this.repositoryMapper = new RepositoryMapper();
        this.sharedPreferences = PreferenceHelper.getDefaultMMSharedPreferences(context);
        this.pushRegistrationId = MobileMessaging.getInstance(context).getInstallation().getPushRegistrationId();
    }

    @VisibleForTesting
    UserProfileManager(ParticipantRepository participantRepository, RepositoryMapper repositoryMapper, SharedPreferences sharedPreferences, @NonNull String pushRegistrationId) {
        this.participantRepository = participantRepository;
        this.repositoryMapper = repositoryMapper;
        this.sharedPreferences = sharedPreferences;
        this.pushRegistrationId = pushRegistrationId;
    }

    @VisibleForTesting(otherwise = PACKAGE_PRIVATE)
    public void save(ChatParticipant profile) {
        if (profile == null || profile.getId() == null) {
            return;
        }
        participantRepository.upsert(repositoryMapper.dbParticipantFromChatParticipant(profile));
        sharedPreferences
                .edit()
                .putString(PARTICIPANT_ID_TAG, profile.getId())
                .apply();
    }

    ChatParticipant get() {
        String id = sharedPreferences.getString(PARTICIPANT_ID_TAG, null);
        if (id == null) {
            return new ChatParticipant(pushRegistrationId);
        }

        Participant participant = participantRepository.findOne(id);
        return repositoryMapper.chatParticipantFromDbParticipant(participant);
    }

    boolean isUsersMessage(Message message) {
        if (message == null || message.getCustomPayload() == null) {
            return false;
        }

        String authorId = message.getCustomPayload().optString("sender", null);
        String userId = sharedPreferences.getString(PARTICIPANT_ID_TAG, "");
        return userId.equalsIgnoreCase(authorId) || (TextUtils.isEmpty(userId) && pushRegistrationId.equalsIgnoreCase(authorId));
    }
}
