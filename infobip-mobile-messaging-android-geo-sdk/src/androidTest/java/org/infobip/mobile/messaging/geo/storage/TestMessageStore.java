package org.infobip.mobile.messaging.geo.storage;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.storage.MessageStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sslavin
 * @since 19/01/2017.
 */


@SuppressWarnings("WeakerAccess")
public class TestMessageStore implements MessageStore {

    Map<String, Message> messages = new HashMap<>();

    @Override
    public List<Message> findAll(Context context) {
        return new ArrayList<>(messages.values());
    }

    @Override
    public long countAll(Context context) {
        return messages.values().size();
    }

    @Override
    public void save(Context context, Message... messages) {
        for (Message message : messages) {
            this.messages.put(message.getMessageId(), message);
        }
    }

    @Override
    public void deleteAll(Context context) {
        messages.clear();
    }
}