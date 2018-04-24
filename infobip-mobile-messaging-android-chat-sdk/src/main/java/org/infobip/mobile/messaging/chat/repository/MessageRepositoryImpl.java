package org.infobip.mobile.messaging.chat.repository;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.chat.repository.db.ChatDatabaseContract;
import org.infobip.mobile.messaging.chat.repository.db.DatabaseHelperImpl;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.PrimaryKeyViolationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class MessageRepositoryImpl implements MessageRepository {

    private final Context context;
    private DatabaseHelper databaseHelper;

    public MessageRepositoryImpl(@NonNull Context context) {
        this.context = context;
    }

    @VisibleForTesting
    public MessageRepositoryImpl(@NonNull Context context, @NonNull DatabaseHelper databaseHelper) {
        this.context = context;
        this.databaseHelper = databaseHelper;
    }

    @NonNull
    @Override
    public List<Message> findAll() {
        return databaseHelper().findAll(Message.class);
    }

    @Override
    public long countAll() {
        return databaseHelper().countAll(Message.class);
    }

    @Override
    public long countAllUnread() {
        return databaseHelper().countAll(Message.class,
                ChatDatabaseContract.MessageColumns.READ_TIMESTAMP + "IS NOT NULL AND " + ChatDatabaseContract.MessageColumns.READ_TIMESTAMP + "IS NOT 0");
    }

    @Nullable
    @Override
    public Message findOne(@NonNull String id) {
        return databaseHelper().find(Message.class, id);
    }

    @Override
    public void upsert(Message message) {
        databaseHelper().save(message);
    }

    @Override
    public void insert(Message message) throws PrimaryKeyViolationException {
        databaseHelper().insert(message);
    }

    @Override
    public Message markRead(String id, long time) {
        Message message = databaseHelper().find(Message.class, id);
        if (message == null) {
            return null;
        }

        message.readAt = time;
        databaseHelper().save(message);
        return message;
    }

    @Override
    public Set<String> markAllMessagesRead(long time) {
        Set<String> ids = new HashSet<>();
        for (Message message : databaseHelper().findAll(Message.class)) {
            if (message.readAt != null && message.readAt > 0) {
                continue;
            }

            message.readAt = time;
            databaseHelper().save(message);
            ids.add(message.id);
        }
        return ids;
    }

    @Override
    public void remove(String... ids) {
        databaseHelper().delete(Message.class, ids);
    }

    @Override
    public void clear() {
        databaseHelper().deleteAll(Message.class);
    }

    // region private methods

    private DatabaseHelper databaseHelper() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelperImpl(context);
        }
        return databaseHelper;
    }

    // endregion
}
