package org.infobip.mobile.messaging.chat.core;

import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.TestBase;
import org.infobip.mobile.messaging.chat.repository.Message;
import org.infobip.mobile.messaging.chat.repository.MessageRepository;
import org.infobip.mobile.messaging.chat.repository.Participant;
import org.infobip.mobile.messaging.chat.repository.ParticipantRepository;
import org.infobip.mobile.messaging.chat.repository.RepositoryMapper;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * @author sslavin
 * @since 17/10/2017.
 */
@RunWith(AndroidJUnit4.class)
public class ChatMessageStorageImplTest extends TestBase {

    private ChatMessageStorageImpl chatMessageStorage;
    private MessageRepository messageRepository;
    private RepositoryMapper repositoryMapper;

    @Before
    public void before() {
        messageRepository = mock(MessageRepository.class);
        repositoryMapper = mock(RepositoryMapper.class);
        chatMessageStorage = new ChatMessageStorageImpl(messageRepository, mock(ParticipantRepository.class), repositoryMapper);
    }

    @Test
    public void should_find_all_messages_and_participants_in_repository() throws JSONException {
        ChatMessage givenMessage = givenChatMessage();
        given(messageRepository.findAll()).willReturn(givenRepositoryMessages());
        given(repositoryMapper.chatMessageFromDbMessageAndParticipant(any(Message.class), any(Participant.class)))
                .willReturn(givenMessage);

        List<ChatMessage> actualMessages = chatMessageStorage.findAllMessages();

        assertEquals(1, actualMessages.size());
        assertMessageEquals(givenMessage, actualMessages.get(0));
    }

    @Test
    public void should_count_all_messages_in_repository() {
        long givenCount = 10;
        given(messageRepository.countAll()).willReturn(givenCount);

        long actualCount = chatMessageStorage.countAllMessages();

        assertEquals(givenCount, actualCount);
    }

    @Test
    public void should_find_one_message_in_repository() throws JSONException {
        ChatMessage givenMessage = givenChatMessage();
        String givenMessageId = "messageId";
        given(messageRepository.findOne(anyString())).willReturn(givenRepositoryMessage());
        given(repositoryMapper.chatMessageFromDbMessageAndParticipant(any(Message.class), any(Participant.class)))
                .willReturn(givenMessage);

        ChatMessage actualMessage = chatMessageStorage.findMessage(givenMessageId);

        then(messageRepository).should(times(1)).findOne(eq(givenMessageId));
        assertMessageEquals(givenMessage, actualMessage);
    }

    @Test
    public void should_delete_by_id() {
        String givenId = "messageId";

        chatMessageStorage.delete(givenId);

        then(messageRepository).should(times(1)).remove(eq("messageId"));
    }

    @Test
    public void should_delete_all() {
        chatMessageStorage.deleteAll();

        then(messageRepository).should(times(1)).clear();
    }

    // region private methods

    private List<Message> givenRepositoryMessages() {
        return new ArrayList<Message>() {{
            add(givenRepositoryMessage());
        }};
    }

    private Message givenRepositoryMessage() {
        return new Message();
    }

    // endregion
}
