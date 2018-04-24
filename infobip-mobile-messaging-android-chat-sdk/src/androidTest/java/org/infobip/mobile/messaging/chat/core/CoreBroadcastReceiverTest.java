package org.infobip.mobile.messaging.chat.core;

import android.content.Context;
import android.content.Intent;
import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.chat.TestBase;
import org.infobip.mobile.messaging.chat.broadcast.ChatBroadcaster;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * @author sslavin
 * @since 10/10/2017.
 */
@RunWith(AndroidJUnit4.class)
public class CoreBroadcastReceiverTest extends TestBase {

    private CoreBroadcastReceiver coreBroadcastReceiver;

    private Context context;
    private ChatBroadcaster chatBroadcaster;
    private ObjectMapper objectMapper;
    private UserProfileManager userProfileManager;

    @Before
    public void before() {
        chatBroadcaster = mock(ChatBroadcaster.class);
        objectMapper = mock(ObjectMapper.class);
        userProfileManager = mock(UserProfileManager.class);
        context = mock(Context.class);
        coreBroadcastReceiver = new CoreBroadcastReceiver(chatBroadcaster, objectMapper, userProfileManager);
    }

    @Test
    public void should_resend_user_data_broadcast() {

        ChatParticipant givenParticipant = givenChatParticipant();
        UserData givenUserData = givenUserData(givenParticipant);
        Intent givenIntent = givenIntentWithUserData(givenUserData);
        given(objectMapper.fromUserData(any(UserData.class))).willReturn(givenParticipant);

        coreBroadcastReceiver.onReceive(context, givenIntent);

        then(userProfileManager).should(times(1)).save(givenParticipant);
        then(chatBroadcaster).should(times(1)).userInfoSynchronized(givenParticipant);
    }

    // region internal methods

    private UserData givenUserData(ChatParticipant chatParticipant) {
        return new ObjectMapper().toUserData(chatParticipant);
    }

    // endregion
}
