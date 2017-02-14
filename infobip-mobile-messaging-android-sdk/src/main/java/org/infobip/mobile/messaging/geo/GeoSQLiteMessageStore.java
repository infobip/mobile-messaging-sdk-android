package org.infobip.mobile.messaging.geo;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.dal.sqlite.SqliteMessage;
import org.infobip.mobile.messaging.storage.MessageStore;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 13/02/2017.
 */

public class GeoSQLiteMessageStore implements MessageStore {
    @SuppressWarnings("WeakerAccess")
    public static class SqliteGeoMessage extends SqliteMessage {

        public SqliteGeoMessage() {
            super();
        }

        public SqliteGeoMessage(Message m) {
            super(m);
        }

        @Override
        public String getTableName() {
            return DatabaseContract.Tables.GEO_MESSAGES;
        }
    }


    public void save(Context context, Message... messages) {
        DatabaseHelper helper = MobileMessagingCore.getDatabaseHelper(context);
        for (Message message : messages) {
            helper.save(new SqliteGeoMessage(message));
        }
    }

    public List<Message> findAll(Context context) {
        return new ArrayList<Message>(MobileMessagingCore.getDatabaseHelper(context).findAll(SqliteGeoMessage.class));
    }

    public Message findById(Context context, String messageId) {
        return MobileMessagingCore.getDatabaseHelper(context).find(SqliteGeoMessage.class, messageId);
    }

    public long countAll(Context context) {
        return MobileMessagingCore.getDatabaseHelper(context).countAll(SqliteGeoMessage.class);
    }

    public void deleteAll(Context context) {
        MobileMessagingCore.getDatabaseHelper(context).deleteAll(SqliteGeoMessage.class);
    }

    public void deleteById(Context context, String messageId) {
        MobileMessagingCore.getDatabaseHelper(context).delete(SqliteGeoMessage.class, messageId);
    }
}
