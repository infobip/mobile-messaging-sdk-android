/*
 * SQLiteMessageStore.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.storage;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.SqliteMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores messages in SQLite database
 * <pre>
 * {@code
 * public class MyActivity extends AppCompatActivity {
 *        protected void onCreate(Bundle savedInstanceState) {
 *            super.onCreate(savedInstanceState);
 *
 *            new MobileMessaging.Builder(this)
 *                .withMessageStore(SQLiteMessageStore.class);
 *                .build();
 *        }
 *    }}
 * </pre>
 * @author sslavin
 * @since 29/12/2016.
 */

public class SQLiteMessageStore implements MessageStore {

    public void save(Context context, Message... messages) {
        DatabaseHelper helper = MobileMessagingCore.getDatabaseHelper(context);
        for (Message message : messages) {
            helper.save(new SqliteMessage(message));
        }
    }

    public List<Message> findAll(Context context) {
        return new ArrayList<Message>(MobileMessagingCore.getDatabaseHelper(context).findAll(SqliteMessage.class));
    }

    public Message findById(Context context, String messageId) {
        return MobileMessagingCore.getDatabaseHelper(context).find(SqliteMessage.class, messageId);
    }

    public long countAll(Context context) {
        return MobileMessagingCore.getDatabaseHelper(context).countAll(SqliteMessage.class);
    }

    public void deleteAll(Context context) {
        MobileMessagingCore.getDatabaseHelper(context).deleteAll(SqliteMessage.class);
    }

    public void deleteById(Context context, String messageId) {
        MobileMessagingCore.getDatabaseHelper(context).delete(SqliteMessage.class, messageId);
    }
}