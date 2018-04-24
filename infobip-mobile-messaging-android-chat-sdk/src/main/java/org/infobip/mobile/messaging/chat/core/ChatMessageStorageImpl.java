package org.infobip.mobile.messaging.chat.core;

import android.text.TextUtils;

import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatMessageStorage;
import org.infobip.mobile.messaging.chat.repository.Message;
import org.infobip.mobile.messaging.chat.repository.MessageRepository;
import org.infobip.mobile.messaging.chat.repository.Participant;
import org.infobip.mobile.messaging.chat.repository.ParticipantRepository;
import org.infobip.mobile.messaging.chat.repository.RepositoryMapper;
import org.infobip.mobile.messaging.dal.sqlite.PrimaryKeyViolationException;
import org.infobip.mobile.messaging.mobile.common.MAsyncTask;
import org.infobip.mobile.messaging.platform.Time;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author sslavin
 * @since 17/10/2017.
 */

public class ChatMessageStorageImpl implements ChatMessageStorage {

    private final MessageRepository messageRepository;
    private final ParticipantRepository participantRepository;
    private final RepositoryMapper repositoryMapper;
    private final Set<Listener> listeners = new HashSet<>();

    ChatMessageStorageImpl(MessageRepository messageRepository, ParticipantRepository participantRepository, RepositoryMapper repositoryMapper) {
        this.messageRepository = messageRepository;
        this.participantRepository = participantRepository;
        this.repositoryMapper = repositoryMapper;
    }

    @Override
    public List<ChatMessage> findAllMessages() {
        List<Message> repositoryMessages = messageRepository.findAll();
        List<ChatMessage> chatMessages = new ArrayList<>(repositoryMessages.size());
        for (Message message : repositoryMessages) {
            chatMessages.add(repositoryMapper.chatMessageFromDbMessageAndParticipant(message, participantRepository.findAuthor(message)));
        }
        return chatMessages;
    }

    @Override
    public long countAllMessages() {
        return messageRepository.countAll();
    }

    @Override
    public long countAllUnreadMessages() {
        return messageRepository.countAllUnread();
    }

    @Override
    public ChatMessage findMessage(String id) {
        Message message = messageRepository.findOne(id);
        if (message == null) {
            return null;
        }

        Participant participant = participantRepository.findAuthor(message);
        return repositoryMapper.chatMessageFromDbMessageAndParticipant(message, participant);
    }

    @Override
    public void save(ChatMessage message) {

        if (message.getAuthor() != null && !TextUtils.isEmpty(message.getAuthor().getId())) {
            Participant participant = repositoryMapper.dbParticipantFromChatParticipant(message.getAuthor());
            participantRepository.upsert(participant);
        }

        Message repositoryMessage = repositoryMapper.dbMessageFromChatMessage(message);
        try {
            messageRepository.insert(repositoryMessage);
            invokeOnNew(message);
        } catch (PrimaryKeyViolationException e) {
            messageRepository.upsert(repositoryMessage);
            invokeOnUpdated(message);
        }
    }

    @Override
    public void delete(String id) {
        messageRepository.remove(id);
        invokeOnDeleted(id);
    }

    @Override
    public void deleteAll() {
        messageRepository.clear();
        invokeOnAllDeleted();
    }

    @Override
    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    void markRead(String messageId) {
        Message message = messageRepository.markRead(messageId, Time.now());
        if (message == null) {
            return;
        }

        Participant participant = participantRepository.findAuthor(message);
        invokeOnUpdated(repositoryMapper.chatMessageFromDbMessageAndParticipant(message, participant));
    }

    Set<String> markAllRead() {
        Set<String> ids = messageRepository.markAllMessagesRead(Time.now());
        for (String id : ids) {
            markRead(id);
        }
        return ids;
    }

    // region private methods

    private void invokeOnNew(final ChatMessage message) {
        invokeOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Listener listener : listeners) {
                    listener.onNew(message);
                }
            }
        });
    }

    private void invokeOnUpdated(final ChatMessage message) {
        invokeOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Listener listener : listeners) {
                    listener.onUpdated(message);
                }
            }
        });
    }

    private void invokeOnDeleted(final String id) {
        invokeOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Listener listener : listeners) {
                    listener.onDeleted(id);
                }
            }
        });
    }

    private void invokeOnAllDeleted() {
        invokeOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Listener listener : listeners) {
                    listener.onAllDeleted();
                }
            }
        });
    }

    private void invokeOnUiThread(final Runnable runnable) {
        new MAsyncTask<Void, Void>() {

            @Override
            public Void run(Void[] objects) {
                return null;
            }

            @Override
            public void after(Void o) {
                runnable.run();
            }
        }.execute();
    }

    // endregion
}
