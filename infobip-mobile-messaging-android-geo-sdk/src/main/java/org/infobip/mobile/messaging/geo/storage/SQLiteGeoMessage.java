package org.infobip.mobile.messaging.geo.storage;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.sqlite.DatabaseContract;
import org.infobip.mobile.messaging.dal.sqlite.SqliteMessage;

public class SQLiteGeoMessage extends SqliteMessage {

    public SQLiteGeoMessage() {
        super();
    }

    public SQLiteGeoMessage(Message m) {
        super(m);
    }

    @Override
    public String getTableName() {
        return DatabaseContract.Tables.GEO_MESSAGES;
    }
}
