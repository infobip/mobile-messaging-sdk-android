package org.infobip.mobile.messaging.chat.broadcast;

import android.content.Context;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.chat.ChatEvent;
import org.infobip.mobile.messaging.chat.MobileChat;
import org.infobip.mobile.messaging.chat.TestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * @author sslavin
 * @since 10/10/2017.
 */
@RunWith(AndroidJUnit4.class)
public class ChatBroadcasterImplTest extends TestBase {

    private Context contextMock;
    private LocalBroadcastManagerWrapper localBroadcastManagerWrapperMock;
    private ChatBundleMapper chatBundleMapperMock;
    private ChatBroadcasterImpl implementation;

    @Before
    public void before() {
        chatBundleMapperMock = mock(ChatBundleMapper.class);
        contextMock = mock(Context.class);
        localBroadcastManagerWrapperMock = mock(LocalBroadcastManagerWrapper.class);

        implementation = new ChatBroadcasterImpl(contextMock, localBroadcastManagerWrapperMock, chatBundleMapperMock);
    }

    @Test
    public void should_call_mapper_and_broadcast_event_on_message_received() throws Exception {
        ChatMessage givenMessage = givenChatMessage();
        final Bundle givenBundle = givenBundle();
        given(chatBundleMapperMock.chatMessageToBundle(any(ChatMessage.class))).willReturn(givenBundle);

        implementation.chatMessageReceived(givenMessage);

        then(chatBundleMapperMock).should(times(1)).chatMessageToBundle(eq(givenMessage));
        then(contextMock).should(times(1)).sendBroadcast(eqIntentWith(ChatEvent.CHAT_MESSAGE_RECEIVED, givenBundle));
        then(localBroadcastManagerWrapperMock).should(times(1)).sendBroadcast(eqIntentWith(ChatEvent.CHAT_MESSAGE_RECEIVED, givenBundle));
    }

    @Test
    public void should_call_mapper_and_broadcast_event_on_message_sent() throws Exception {
        ChatMessage givenMessage = givenChatMessage();
        final Bundle givenBundle = givenBundle();
        given(chatBundleMapperMock.chatMessageToBundle(any(ChatMessage.class))).willReturn(givenBundle);

        implementation.chatMessageSent(givenMessage);

        then(chatBundleMapperMock).should(times(1)).chatMessageToBundle(eq(givenMessage));
        then(contextMock).should(times(1)).sendBroadcast(eqIntentWith(ChatEvent.CHAT_MESSAGE_SENT, givenBundle));
        then(localBroadcastManagerWrapperMock).should(times(1)).sendBroadcast(eqIntentWith(ChatEvent.CHAT_MESSAGE_SENT, givenBundle));
    }

    @Test
    public void should_call_mapper_and_broadcast_event_on_message_tapped() throws Exception {
        ChatMessage givenMessage = givenChatMessage();
        final Bundle givenBundle = givenBundle();
        given(chatBundleMapperMock.chatMessageToBundle(any(ChatMessage.class))).willReturn(givenBundle);

        implementation.chatMessageTapped(givenMessage);

        then(chatBundleMapperMock).should(times(1)).chatMessageToBundle(eq(givenMessage));
        then(contextMock).should(times(1)).sendBroadcast(eqIntentWith(ChatEvent.CHAT_MESSAGE_TAPPED, givenBundle));
        then(localBroadcastManagerWrapperMock).should(times(1)).sendBroadcast(eqIntentWith(ChatEvent.CHAT_MESSAGE_TAPPED, givenBundle));
    }

    @Test
    public void should_call_mapper_and_broadcast_event_on_action_tapped() throws Exception {
        ChatMessage givenMessage = givenChatMessage();
        String givenActionId = "actionId";
        final Bundle givenBundle = givenBundle();
        Bundle givenBundleWithAction = new Bundle(givenBundle);
        givenBundleWithAction.putString(MobileChat.EXTRA_ACTION_ID, givenActionId);
        given(chatBundleMapperMock.chatMessageToBundle(any(ChatMessage.class))).willReturn(givenBundle);

        implementation.chatMessageViewActionTapped(givenMessage, givenActionId);

        then(chatBundleMapperMock).should(times(1)).chatMessageToBundle(eq(givenMessage));
        then(contextMock).should(times(1)).sendBroadcast(eqIntentWith(ChatEvent.CHAT_MESSAGE_VIEW_ACTION_TAPPED, givenBundleWithAction));
        then(localBroadcastManagerWrapperMock).should(times(1)).sendBroadcast(eqIntentWith(ChatEvent.CHAT_MESSAGE_VIEW_ACTION_TAPPED, givenBundleWithAction));
    }

    @Test
    public void should_call_mapper_and_broadcast_event_on_user_info_synchronized() throws Exception {
        ChatParticipant givenParticipant = givenChatParticipant();
        final Bundle givenBundle = givenBundle();
        given(chatBundleMapperMock.chatParticipantToBundle(any(ChatParticipant.class))).willReturn(givenBundle);

        implementation.userInfoSynchronized(givenParticipant);

        then(chatBundleMapperMock).should(times(1)).chatParticipantToBundle(eq(givenParticipant));
        then(contextMock).should(times(1)).sendBroadcast(eqIntentWith(ChatEvent.CHAT_USER_INFO_SYNCHRONIZED, givenBundle));
        then(localBroadcastManagerWrapperMock).should(times(1)).sendBroadcast(eqIntentWith(ChatEvent.CHAT_USER_INFO_SYNCHRONIZED, givenBundle));
    }
}
