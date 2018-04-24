package org.infobip.mobile.messaging.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.chat.repository.MJSONObject;
import org.json.JSONException;
import org.mockito.ArgumentMatcher;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Objects;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.argThat;

/**
 * @author sslavin
 * @since 10/10/2017.
 */

public abstract class TestBase {

    protected Intent eqIntentWith(final ChatEvent action, final Bundle givenBundle) {
        return argThat(new ArgumentMatcher<Intent>() {
            @Override
            public boolean matches(Object argument) {
                Intent intent = (Intent) argument;
                return checkEquals(intent.getExtras(), givenBundle)
                        && action.getKey().equals(intent.getAction());
            }
        });
    }

    @NonNull
    protected Bundle givenBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        return bundle;
    }

    @NonNull
    protected ChatMessage givenChatMessage() {
        return new ChatMessage(
                "messageId",
                "body",
                "chatId",
                1L,
                2L,
                3L,
                "category",
                "contentUrl",
                givenChatParticipant(),
                org.infobip.mobile.messaging.Message.Status.UNKNOWN,
                MJSONObject.create("{'key':'value'}"),
                true);
    }

    @NonNull
    protected ChatParticipant givenChatParticipant() {
        return new ChatParticipant(
                "participantId",
                "firstName",
                "lastName",
                "middleName",
                "email@email.com",
                "1234567",
                MJSONObject.create("{'key':'value'}"));
    }

    protected Intent givenIntentWithUserData(UserData userData) {
        return new Intent(org.infobip.mobile.messaging.Event.USER_DATA_REPORTED.getKey())
                .putExtra(BroadcastParameter.EXTRA_USER_DATA, userData.toString());
    }

    protected void assertMessageEquals(ChatMessage expected, ChatMessage actual) throws JSONException {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getBody(), actual.getBody());
        assertEquals(expected.getChatId(), actual.getChatId());
        assertEquals(expected.getCreatedAt(), actual.getCreatedAt());
        assertEquals(expected.getReceivedAt(), actual.getReceivedAt());
        assertEquals(expected.getReadAt(), actual.getReadAt());
        assertParticipantEquals(expected.getAuthor(), actual.getAuthor());
        assertEquals(expected.getStatus(), actual.getStatus());
        JSONAssert.assertEquals(expected.getCustomData(), actual.getCustomData(), true);
    }

    // region internal methods

    private void assertParticipantEquals(ChatParticipant expected, ChatParticipant actual) throws JSONException {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getMiddleName(), actual.getMiddleName());
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expected.getGsm(), actual.getGsm());
        JSONAssert.assertEquals(expected.getCustomData(), actual.getCustomData(), true);
    }

    private boolean checkEquals(Bundle first, Bundle second) {
        Set<String> aks = first.keySet();
        Set<String> bks = second.keySet();

        if (!aks.containsAll(bks)) {
            return false;
        }

        for (String key : aks) {
            if (!Objects.equals(first.get(key), second.get(key))) {
                return false;
            }
        }

        return true;
    }

    // endregion
}
