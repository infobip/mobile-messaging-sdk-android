package org.infobip.mobile.messaging.chat.core;

import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.CustomUserDataValue;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.chat.repository.MJSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.HashMap;

import static junit.framework.Assert.assertEquals;

/**
 * @author sslavin
 * @since 10/10/2017.
 */
@RunWith(AndroidJUnit4.class)
public class ObjectMapperTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void should_map_push_message_to_chat_message() throws Exception {
        Message givenMessage = new Message(
                "messageId",
                "title",
                "body",
                "sound",
                true,
                "icon",
                true,
                "category",
                "from",
                123L,
                456L,
                789L,
                MJSONObject.create()
                        .add("chatId", "chatId")
                        .add("sender", "senderId")
                        .add("senderFirstName", "firstName")
                        .add("senderLastName", "lastName")
                        .add("senderMiddleName", "middleName")
                        .add("senderEmail", "sender@email.com")
                        .add("senderGsm", "senderGsm")
                        .add("senderData", MJSONObject.create().add("key", "value"))
                        .add("someKey", "someValue")
                        .add("isChat", true),
                "{'key2':'value2'}",
                "destination",
                Message.Status.ERROR,
                "statusMessage",
                "contentUrl",
                null
        );

        ChatMessage message = objectMapper.fromBaseMessage(givenMessage, true);
        assertEquals(givenMessage.getMessageId(), message.getId());
        assertEquals(givenMessage.getBody(), message.getBody());
        assertEquals(789L, message.getCreatedAt());
        assertEquals(123L, message.getReceivedAt());
        assertEquals(456L, message.getReadAt());
        assertEquals("chatId", message.getChatId());
        assertEquals("category", message.getCategory());
        assertEquals("contentUrl", message.getContentUrl());
        assertEquals(Message.Status.ERROR, message.getStatus());
        JSONAssert.assertEquals(MJSONObject.create().add("someKey", "someValue"), message.getCustomData(), true);

        ChatParticipant participant = message.getAuthor();
        assertEquals("senderId", participant.getId());
        assertEquals("firstName", participant.getFirstName());
        assertEquals("lastName", participant.getLastName());
        assertEquals("firstName lastName", participant.getUserName());
        assertEquals("middleName", participant.getMiddleName());
        assertEquals("sender@email.com", participant.getEmail());
        assertEquals("senderGsm", participant.getGsm());
        JSONAssert.assertEquals(MJSONObject.create().add("key", "value"), participant.getCustomData(), true);
    }

    @Test
    public void should_map_chat_message_to_push_message() throws Exception {
        ChatParticipant givenParticipant = new ChatParticipant(
                "participantId",
                "firstName",
                "lastName",
                "middleName",
                "participant@email.com",
                "gsm",
                MJSONObject.create().add("key", "value"));

        ChatMessage givenChatMessage = new ChatMessage(
                "messageId",
                "body",
                "chatId",
                123L,
                456L,
                789L,
                "category",
                "contentUrl",
                givenParticipant,
                Message.Status.SUCCESS,
                MJSONObject.create().add("key", "value"),
                true);

        Message message = objectMapper.toBaseMessage(givenChatMessage);
        assertEquals(givenChatMessage.getId(), message.getMessageId());
        assertEquals(givenChatMessage.getBody(), message.getBody());
        assertEquals(givenChatMessage.getChatId(), message.getCustomPayload().getString("chatId"));
        assertEquals(givenChatMessage.getCreatedAt(), message.getSentTimestamp());
        assertEquals(givenChatMessage.getReceivedAt(), message.getReceivedTimestamp());
        assertEquals(givenChatMessage.getReadAt(), message.getSeenTimestamp());
        assertEquals(givenChatMessage.getStatus(), message.getStatus());
        assertEquals(givenChatMessage.getCategory(), message.getCategory());
        assertEquals(givenChatMessage.getContentUrl(), message.getContentUrl());
        assertEquals("value", message.getCustomPayload().getString("key"));

        assertEquals("participantId", message.getCustomPayload().getString("sender"));
        assertEquals("firstName", message.getCustomPayload().getString("senderFirstName"));
        assertEquals("lastName", message.getCustomPayload().getString("senderLastName"));
        assertEquals("middleName", message.getCustomPayload().getString("senderMiddleName"));
        assertEquals("participant@email.com", message.getCustomPayload().getString("senderEmail"));
        assertEquals("gsm", message.getCustomPayload().getString("senderGsm"));
        assertEquals(true, message.getCustomPayload().getBoolean("isChat"));
        assertEquals("{\"key\":\"value\"}", message.getCustomPayload().getString("senderData"));
    }

    @Test
    public void should_map_user_data_to_participant() throws Exception {
        UserData givenUserData = new UserData() {{
            setExternalUserId("userId");
            setFirstName("firstName");
            setLastName("lastName");
            setMiddleName("middleName");
            setEmail("email@email.com");
            setMsisdn("msisdn");
            setCustomUserData(new HashMap<String, CustomUserDataValue>() {{
                put("chatCustomData", new CustomUserDataValue(
                        MJSONObject.create().add("key", "value").toString()));
            }});
        }};

        ChatParticipant participant = objectMapper.fromUserData(givenUserData);
        assertEquals("userId", participant.getId());
        assertEquals("firstName", participant.getFirstName());
        assertEquals("lastName", participant.getLastName());
        assertEquals("middleName", participant.getMiddleName());
        assertEquals("email@email.com", participant.getEmail());
        assertEquals("msisdn", participant.getGsm());
        JSONAssert.assertEquals(MJSONObject.create().add("key", "value"), participant.getCustomData(), false);
    }

    @Test
    public void should_map_participant_to_user_data() throws Exception {
        ChatParticipant givenParticipant = new ChatParticipant(
                "participantId",
                "firstName",
                "lastName",
                "middleName",
                "participant@email.com",
                "gsm",
                MJSONObject.create().add("key", "value"));

        UserData userData = objectMapper.toUserData(givenParticipant);

        assertEquals("participantId", userData.getExternalUserId());
        assertEquals("firstName", userData.getFirstName());
        assertEquals("lastName",userData.getLastName());
        assertEquals("middleName", userData.getMiddleName());
        assertEquals("participant@email.com", userData.getEmail());
        assertEquals("gsm", userData.getMsisdn());
        assertEquals("{\"key\":\"value\"}", userData.getCustomUserData().get("chatCustomData").stringValue());
    }
}
