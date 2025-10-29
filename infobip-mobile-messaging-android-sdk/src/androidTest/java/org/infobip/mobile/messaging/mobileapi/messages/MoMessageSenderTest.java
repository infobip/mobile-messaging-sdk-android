/*
 * MoMessageSenderTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.messages;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.messages.MoMessage;
import org.infobip.mobile.messaging.api.messages.MoMessageDelivery;
import org.infobip.mobile.messaging.api.messages.MoMessagesBody;
import org.infobip.mobile.messaging.api.messages.MoMessagesResponse;
import org.infobip.mobile.messaging.api.messages.MobileApiMessages;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.storage.MessageStoreWrapper;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author sslavin
 * @since 22/07/16.
 */
public class MoMessageSenderTest extends MobileMessagingTestCase {

    private MoMessageSender moMessageSender;
    private ArgumentCaptor<List> messagesListCaptor;
    private ArgumentCaptor<Message[]> messageCaptor;
    private ArgumentCaptor<MoMessagesBody> bodyCaptor;
    private MobileApiMessages apiMock;
    private MessageStoreWrapper messageStoreWrapperMock;
    private final JsonSerializer jsonSerializer = new JsonSerializer();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        MRetryPolicy policy = new MRetryPolicy.Builder().withMaxRetries(0).build();
        messagesListCaptor = forClass(List.class);
        bodyCaptor = forClass(MoMessagesBody.class);
        messageCaptor = forClass(Message[].class);
        messageStoreWrapperMock = mock(MessageStoreWrapper.class);
        apiMock = mock(MobileApiMessages.class);

        moMessageSender = new MoMessageSender(
                context,
                mobileMessagingCore,
                broadcaster,
                Executors.newSingleThreadExecutor(),
                mobileMessagingCore.getStats(),
                policy,
                apiMock,
                messageStoreWrapperMock);
    }

    @Test
    public void shouldSendMultipleMessages() throws Exception {

        // Given
        final MoMessageDelivery givenMessage1 = new MoMessageDelivery() {{
            setStatus("Message not sent");
            setStatusCode(1);
            setMessageId("myMessageId");
            setDestination("myDestination");
            setText("myText");
            setCustomPayload(new HashMap<String, Object>() {{
                put("myStringKey", "string");
                put("myBooleanKey", true);
                put("myNumberKey", 1);
            }});
        }};
        final MoMessageDelivery givenMessage2 = new MoMessageDelivery() {{
            setStatus("Message sent");
            setStatusCode(0);
            setMessageId("myMessageId2");
            setDestination("myDestination2");
            setText("myText2");
            setCustomPayload(new HashMap<String, Object>() {{
                put("myStringKey", "string2");
                put("myBooleanKey", false);
                put("myNumberKey", 2);
            }});
        }};
        MoMessagesResponse givenResponse = new MoMessagesResponse() {{
            setMessages(new MoMessageDelivery[]{givenMessage1, givenMessage2});
        }};
        given(apiMock.sendMO(any(MoMessagesBody.class)))
                .willReturn(givenResponse);

        // When
        moMessageSender.send(null, givenMessage(givenMessage1.getMessageId()), givenMessage(givenMessage2.getMessageId()));

        // Then
        verify(broadcaster, after(1000).atLeastOnce()).messagesSent(messagesListCaptor.capture());
        List<Message> messages = messagesListCaptor.getValue();
        assertEquals("myMessageId", messages.get(0).getMessageId());
        assertEquals(Message.Status.ERROR, messages.get(0).getStatus());
        assertEquals("Message not sent", messages.get(0).getStatusMessage());
        assertEquals("myDestination", messages.get(0).getDestination());
        assertEquals("myText", messages.get(0).getBody());
        assertEquals("string", messages.get(0).getCustomPayload().opt("myStringKey"));
        assertEquals(1.0, messages.get(0).getCustomPayload().optDouble("myNumberKey"), 0.01);
        assertEquals(true, messages.get(0).getCustomPayload().opt("myBooleanKey"));
        assertEquals("myMessageId2", messages.get(1).getMessageId());
        assertEquals(Message.Status.SUCCESS, messages.get(1).getStatus());
        assertEquals("Message sent", messages.get(1).getStatusMessage());
        assertEquals("myDestination2", messages.get(1).getDestination());
        assertEquals("myText2", messages.get(1).getBody());
        assertEquals("string2", messages.get(1).getCustomPayload().opt("myStringKey"));
        assertEquals(2.0, messages.get(1).getCustomPayload().optDouble("myNumberKey"), 0.01);
        assertEquals(false, messages.get(1).getCustomPayload().opt("myBooleanKey"));
    }

    @Test
    public void testCustomPayloadNestedObjects() {
        String jsonStr = "{\n" +
                "    \"messageId\": \"messageId\",\n" +
                "    \"aps\": {\n" +
                "        \"badge\": 6,\n" +
                "        \"sound\": \"default\",\n" +
                "        \"alert\": {\n" +
                "            \"bidy\":\"text\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"customPayload\": {\n" +
                "        \"key\": \"value\",\n" +
                "        \"nestedObject\": {\n" +
                "            \"key\": \"value\"\n" +
                "        }\n" +
                "    }\n" +
                "}";

        JSONObject message = null;
        JSONObject expectedCustomPayload = new JSONObject();
        try {
            message = new JSONObject(jsonStr);
            expectedCustomPayload.put("key", "value");
            expectedCustomPayload.put("nestedObject", new JSONObject().put("key", "value"));
            assertEquals(expectedCustomPayload.toString(), message.getJSONObject("customPayload").toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void shouldOnlyKeepMessagesToBeRetried() {

        // Given
        String givenMessageIdToRetry = "messageIdWithRetry";
        String givenMessageIdNoRetry = "messageIdNoRetry";
        given(apiMock.sendMO(any(MoMessagesBody.class)))
                .willThrow(new RuntimeException());

        // When
        moMessageSender.sendWithRetry(givenMessage(givenMessageIdToRetry));
        moMessageSender.send(null, givenMessage(givenMessageIdNoRetry));

        // Then
        verify(apiMock, after(200).atLeast(1)).sendMO(any(MoMessagesBody.class));
        Message[] actualMessages = getSavedMessages();
        assertEquals(1, actualMessages.length);
        assertEquals(givenMessageIdToRetry, actualMessages[0].getMessageId());

    }

    @Test
    public void shouldNotResendMessagesWhichAreTooOld() {

        // Given
        Message givenRelevantMessage = new Message();
        Message givenTooOldMessage = new Message() {{
            setReceivedTimestamp(Time.now() - TimeUnit.HOURS.toMillis(73));
        }};
        given(apiMock.sendMO(any(MoMessagesBody.class)))
                .willThrow(new RuntimeException());

        // When
        moMessageSender.sendWithRetry(givenRelevantMessage, givenTooOldMessage);

        // Then
        verify(apiMock, after(200).atLeast(1)).sendMO(bodyCaptor.capture());
        MoMessage[] actualMessages = bodyCaptor.getValue().getMessages();
        assertEquals(1, actualMessages.length);
        assertEquals(givenRelevantMessage.getMessageId(), actualMessages[0].getMessageId());
    }

    @Test
    public void shouldOnlySaveNonRetriableMessagesToStore() {
        // Given
        MobileMessagingCore.setMessageStoreClass(context, SQLiteMessageStore.class);
        Message givenMessage1 = givenMessage("someMessageId1");
        Message givenMessage2 = givenMessage("someMessageId2");
        final MoMessageDelivery givenDelivery1 = givenDelivery(givenMessage1.getMessageId());
        final MoMessageDelivery givenDelivery2 = givenDelivery(givenMessage2.getMessageId());
        given(apiMock.sendMO(any(MoMessagesBody.class)))
                .willReturn(new MoMessagesResponse(new MoMessageDelivery[]{givenDelivery1}))
                .willReturn(new MoMessagesResponse(new MoMessageDelivery[]{givenDelivery2}));

        // When
        moMessageSender.send(null, givenMessage1);
        moMessageSender.sendWithRetry(givenMessage2);

        // Then
        verify(apiMock, after(200).atLeast(1)).sendMO(any(MoMessagesBody.class));
        verify(messageStoreWrapperMock, times(1)).upsert(messageCaptor.capture());
        List<Message> storedMessages = getAllMessages(messageCaptor.getAllValues());
        assertEquals(1, storedMessages.size());
        assertEquals(givenMessage1.getMessageId(), storedMessages.get(0).getMessageId());
    }

    private List<Message> getAllMessages(List<Message[]> messagesLists) {
        List<Message> messages = new ArrayList<>();
        for (Message[] arr : messagesLists) {
            messages.addAll(asList(arr));
        }
        return messages;
    }

    @NonNull
    private MoMessageDelivery givenDelivery(final String messageId) {
        return new MoMessageDelivery() {{
            setStatus("Message sent");
            setStatusCode(0);
            setMessageId(messageId);
        }};
    }

    private Message[] getSavedMessages() {
        String[] jsons = PreferenceHelper.findStringArray(context, MobileMessagingProperty.UNSENT_MO_MESSAGES);
        List<Message> messages = new ArrayList<>(jsons.length);
        for (String json : jsons) {
            messages.add(jsonSerializer.deserialize(json, Message.class));
        }
        return messages.toArray(new Message[0]);
    }

    private Message givenMessage(String messageId) {
        return createMessage(context, messageId, false);
    }
}
