package org.infobip.mobile.messaging.geo.storage;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseHelper;
import org.infobip.mobile.messaging.storage.MessageStore;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 13/02/2017.
 */

public class GeoSQLiteMessageStore implements MessageStore {

    public void save(Context context, Message... messages) {
        DatabaseHelper helper = MobileMessagingCore.getDatabaseHelper(context);
        for (Message message : messages) {
            helper.save(new SQLiteGeoMessage(message));
        }
    }

    public List<Message> findAll(Context context) {
        return new ArrayList<Message>(MobileMessagingCore.getDatabaseHelper(context).findAll(SQLiteGeoMessage.class));
    }

    public Message findById(Context context, String messageId) {
        return MobileMessagingCore.getDatabaseHelper(context).find(SQLiteGeoMessage.class, messageId);
    }

    public long countAll(Context context) {
        return MobileMessagingCore.getDatabaseHelper(context).countAll(SQLiteGeoMessage.class);
    }

    public void deleteAll(Context context) {
        MobileMessagingCore.getDatabaseHelper(context).deleteAll(SQLiteGeoMessage.class);
    }

    public void deleteById(Context context, String messageId) {
        MobileMessagingCore.getDatabaseHelper(context).delete(SQLiteGeoMessage.class, messageId);
    }

    public void deleteByIds(Context context, String[] messageIds) {
        MobileMessagingCore.getDatabaseHelper(context).delete(SQLiteGeoMessage.class, messageIds);
    }
}
