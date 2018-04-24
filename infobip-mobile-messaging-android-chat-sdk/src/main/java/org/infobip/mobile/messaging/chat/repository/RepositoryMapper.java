package org.infobip.mobile.messaging.chat.repository;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatParticipant;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class RepositoryMapper {

    @NonNull
    public ChatMessage chatMessageFromDbMessageAndParticipant(@NonNull Message message, @Nullable Participant participant) {
        return new ChatMessage(
                message.id,
                message.body,
                message.chatId,
                message.createdAt,
                message.receivedAt,
                message.readAt,
                message.category,
                message.contentUrl,
                chatParticipantFromDbParticipant(participant),
                org.infobip.mobile.messaging.Message.Status.valueOf(message.status),
                MJSONObject.create(message.customData),
                message.isYours);
    }

    @Nullable
    public ChatParticipant chatParticipantFromDbParticipant(@Nullable Participant participant) {
        return participant == null ? null : new ChatParticipant(
                participant.id,
                participant.firstName,
                participant.lastName,
                participant.middleName,
                participant.email,
                participant.gsm,
                MJSONObject.create(participant.customData));
    }

    @NonNull
    public Message dbMessageFromChatMessage(@NonNull ChatMessage chatMessage) {
        Message message = new Message();
        message.id = chatMessage.getId();
        message.body = chatMessage.getBody();
        message.chatId = chatMessage.getChatId();
        message.createdAt = chatMessage.getCreatedAt();
        message.receivedAt = chatMessage.getReceivedAt();
        message.readAt = chatMessage.getReadAt();
        message.category = chatMessage.getCategory();
        message.contentUrl = chatMessage.getContentUrl();
        message.authorId = chatMessage.getAuthor() == null ? null : chatMessage.getAuthor().getId();
        message.status = chatMessage.getStatus() == null ? null : chatMessage.getStatus().name();
        message.customData = chatMessage.getCustomData() == null ? null : chatMessage.getCustomData().toString();
        message.isYours = chatMessage.isYours();
        return message;
    }

    @NonNull
    public Participant dbParticipantFromChatParticipant(@NonNull ChatParticipant chatParticipant) {
        Participant participant = new Participant();
        participant.id = chatParticipant.getId();
        participant.firstName = chatParticipant.getFirstName();
        participant.lastName = chatParticipant.getLastName();
        participant.middleName = chatParticipant.getMiddleName();
        participant.email = chatParticipant.getEmail();
        participant.gsm = chatParticipant.getGsm();
        participant.customData = chatParticipant.getCustomData() == null ? null : chatParticipant.getCustomData().toString();
        return participant;
    }
}
