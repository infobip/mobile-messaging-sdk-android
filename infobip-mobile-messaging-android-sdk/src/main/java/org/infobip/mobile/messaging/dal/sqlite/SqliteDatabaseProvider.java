package org.infobip.mobile.messaging.dal.sqlite;

import android.database.sqlite.SQLiteDatabase;

/**
 * @author sslavin
 * @since 13/01/2017.
 */

public interface SqliteDatabaseProvider {
    /**
     * Returns SQLite database for raw operations
     * You don't need to close it when you're done.
     * @return writable sqlite database
     */
    SQLiteDatabase getDatabase();

    /**
     * Deletes database and releases related resources.
     */
    void deleteDatabase();
}
