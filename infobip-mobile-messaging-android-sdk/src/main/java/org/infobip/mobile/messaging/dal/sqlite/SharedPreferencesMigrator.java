package org.infobip.mobile.messaging.dal.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;

import java.util.List;

/**
 * @author sslavin
 * @since 16/01/2017.
 */

class SharedPreferencesMigrator {

    static void migrateMessages(Context context, SQLiteDatabase db) {
        SharedPreferencesMessageStore sharedPreferencesMessageStore = new SharedPreferencesMessageStore();
        List<Message> messages = sharedPreferencesMessageStore.findAll(context);
        if (messages.isEmpty()) {
            return;
        }

        for (Message message : messages) {
            db.insert(SqliteMessage.getTable(), null, SqliteMessage.save(message));
        }
    }
}
