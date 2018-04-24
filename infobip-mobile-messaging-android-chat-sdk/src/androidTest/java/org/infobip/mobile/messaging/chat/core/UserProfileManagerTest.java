package org.infobip.mobile.messaging.chat.core;

import android.content.SharedPreferences;
import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.chat.TestBase;
import org.infobip.mobile.messaging.chat.repository.Participant;
import org.infobip.mobile.messaging.chat.repository.ParticipantRepository;
import org.infobip.mobile.messaging.chat.repository.RepositoryMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author sslavin
 * @since 10/10/2017.
 */
@RunWith(AndroidJUnit4.class)
public class UserProfileManagerTest extends TestBase {

    private UserProfileManager userProfileManager;
    private ParticipantRepository participantRepository;
    private RepositoryMapper repositoryMapper;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String pushRegistrationId;

    @Before
    public void before() {
        participantRepository = mock(ParticipantRepository.class);
        repositoryMapper = mock(RepositoryMapper.class);
        sharedPreferences = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);
        pushRegistrationId = "d1aca8ed-c9d3-426f-a9e7-b10a1a43c864";

        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);

        userProfileManager = new UserProfileManager(participantRepository, repositoryMapper, sharedPreferences, pushRegistrationId);
    }

    @Test
    public void should_save_participant_to_db_and_id_to_preferences() {
        ChatParticipant givenChatParticipant = givenChatParticipant();
        Participant givenParticipant = givenParticipant(givenChatParticipant);
        when(repositoryMapper.dbParticipantFromChatParticipant(any(ChatParticipant.class))).thenReturn(givenParticipant);

        userProfileManager.save(givenChatParticipant);

        verify(participantRepository, times(1)).upsert(givenParticipant);
        then(editor.putString(eq("org.infobip.mobile.messaging.chat.PARTICIPANT_ID_TAG"), eq(givenChatParticipant.getId()))).should(times(1));
    }

    @Test
    public void should_determine_if_message_is_from_local_user() throws Exception {
        Message givenMessage = givenPushMessageFromUser("localId");
        when(sharedPreferences.getString(eq("org.infobip.mobile.messaging.chat.PARTICIPANT_ID_TAG"), anyString())).thenReturn("localId");

        assertTrue(userProfileManager.isUsersMessage(givenMessage));
    }

    @Test
    public void should_fallback_to_push_reg_id_as_user_id_if_no_user_info_provided() {
        when(sharedPreferences.getString(eq("org.infobip.mobile.messaging.chat.PARTICIPANT_ID_TAG"), anyString())).thenReturn(null);

        assertEquals(pushRegistrationId, userProfileManager.get().getId());
    }

    // region internal methods

    private Participant givenParticipant(final ChatParticipant chatParticipant) {
        return new Participant(){{
            id = chatParticipant.getId();
        }};
    }

    private Message givenPushMessageFromUser(String userId) throws JSONException {
        Message message = new Message();
        message.setCustomPayload(new JSONObject().putOpt("sender", userId));
        return message;
    }

    // endregion
}
