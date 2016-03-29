package org.infobip.mobile.messaging;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author mstipanov
 * @since 29.03.2016.
 */
public enum MessageStore {
    INSTANCE;

    private List<Message> messageCache = new CopyOnWriteArrayList<>();

    public void save(Message message) {
        messageCache.add(message);
    }

    public List<Message> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(messageCache));
    }

    public void deleteAll() {
        messageCache.clear();
    }

    public long countAll() {
        return messageCache.size();
    }

    public List<Message> link() {
        return new AbstractList<Message>() {
            @Override
            public Message get(int location) {
                return messageCache.get(location);
            }

            @Override
            public int size() {
                return messageCache.size();
            }
        };
    }
}
