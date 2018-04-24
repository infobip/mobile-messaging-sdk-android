package org.infobip.mobile.messaging.chat.repository;

import android.content.Context;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.chat.repository.db.DatabaseHelperImpl;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;

import java.util.List;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class ParticipantRepositoryImpl implements ParticipantRepository {

    private final Context context;
    private DatabaseHelper databaseHelper;

    public ParticipantRepositoryImpl(Context context) {
        this.context = context;
    }

    @Override
    public List<Participant> findAll() {
        return databaseHelper().findAll(Participant.class);
    }

    @Override
    public Participant findOne(@NonNull String id) {
        return databaseHelper().find(Participant.class, id);
    }

    @Override
    public Participant findAuthor(@NonNull Message message) {
        if (message.authorId == null) {
            return null;
        }
        return databaseHelper().find(Participant.class, message.authorId);
    }

    @Override
    public void upsert(@NonNull Participant participant) {
        databaseHelper().save(participant);
    }

    @Override
    public void clear() {
        databaseHelper().deleteAll(Participant.class);
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
