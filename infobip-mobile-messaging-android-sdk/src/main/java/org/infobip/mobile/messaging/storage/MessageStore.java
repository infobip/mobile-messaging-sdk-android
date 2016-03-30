package org.infobip.mobile.messaging.storage;

import android.content.Context;
import org.infobip.mobile.messaging.Message;

import java.util.List;

/**
 * @author mstipanov
 * @since 29.03.2016.
 */
public interface MessageStore {

    void save(Context context, Message message);

    List<Message> findAll(Context context);

    void deleteAll(Context context);

    long countAll(Context context);

    List<Message> link(final Context context);
}
