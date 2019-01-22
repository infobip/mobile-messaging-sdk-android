package org.infobip.mobile.messaging.chat.core;

import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.CustomAttributeValue;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.chat.repository.MJSONObject;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static android.support.annotation.VisibleForTesting.PACKAGE_PRIVATE;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

@VisibleForTesting(otherwise = PACKAGE_PRIVATE)
public class ObjectMapper {

    public ChatMessage fromBaseMessage(Message message, boolean isYours) {
        JSONObject originalData = message.getCustomPayload();
        JSONObject data = MJSONObject.copy(message.getCustomPayload(), "chatId", "sender", "senderFirstName",
                "senderLastName", "senderMiddleName", "senderEmail",  "senderGsm", "senderData", "isChat");
        return new ChatMessage(
                message.getMessageId(),
                message.getBody(),
                originalData.optString("chatId", null),
                message.getSentTimestamp(),
                message.getReceivedTimestamp(),
                message.getSeenTimestamp(),
                message.getCategory(),
                message.getContentUrl(),
                new ChatParticipant(
                        originalData.optString("sender", null),
                        originalData.optString("senderFirstName", null),
                        originalData.optString("senderLastName", null),
                        originalData.optString("senderMiddleName", null),
                        originalData.optString("senderEmail", null),
                        originalData.optString("senderGsm", null),
                        MJSONObject.create(originalData.optString("senderData", null))),
                message.getStatus(),
                data,
                isYours
        );
    }

    public Message toBaseMessage(ChatMessage message) {
        MJSONObject customData = MJSONObject.copy(message.getCustomData());
        if (customData == null) customData = MJSONObject.create();
        if (message.getAuthor() != null) {
            ChatParticipant author = message.getAuthor();
            customData
                    .add("sender", author.getId())
                    .add("senderFirstName", author.getFirstName())
                    .add("senderLastName", author.getLastName())
                    .add("senderMiddleName", author.getMiddleName())
                    .add("senderEmail", author.getEmail())
                    .add("senderGsm", author.getGsm())
                    .add("senderData", author.getCustomData() == null ? null : author.getCustomData().toString())
                    .add("isChat", true);
        }
        customData.add("chatId", message.getChatId());
        return new Message(
                message.getId(),
                null,
                message.getBody(),
                null,
                true,
                null,
                false,
                message.getCategory(),
                null,
                message.getReceivedAt(),
                message.getReadAt(),
                message.getCreatedAt(),
                customData,
                null,
                null,
                message.getStatus(),
                null,
                message.getContentUrl());
    }

    public ChatParticipant fromUserData(User user) {
        MJSONObject data = null;
        Map<String, CustomAttributeValue> customData = user.getCustomAttributes();
        if (customData != null) {
            CustomAttributeValue value = customData.get("chatCustomData");
            if (value != null) {
                data = MJSONObject.create(value.stringValue());
            }
        }
        return new ChatParticipant(
                user.getExternalUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getMiddleName(),
                null,
                null,
                data);
    }

    public User toUserData(ChatParticipant participant) {
        Map<String, CustomAttributeValue> customData = new HashMap<>();
        if (participant.getCustomData() != null) {
            customData.put("chatCustomData", new CustomAttributeValue(participant.getCustomData().toString()));
        }
        User user = new User();
        user.setExternalUserId(participant.getId());
        user.setFirstName(participant.getFirstName());
        user.setLastName(participant.getLastName());
        user.setMiddleName(participant.getMiddleName());
//        user.setEmail(participant.getEmail());
//        user.setMsisdn(participant.getGsm());
        user.setCustomAttributes(customData);
        return user;
    }
}
