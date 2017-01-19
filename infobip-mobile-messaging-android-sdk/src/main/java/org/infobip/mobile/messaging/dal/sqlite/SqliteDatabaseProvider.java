package org.infobip.mobile.messaging.dal.sqlite;

import android.database.sqlite.SQLiteDatabase;

/**
 * @author sslavin
 * @since 13/01/2017.
 */

interface SqliteDatabaseProvider {
    /**
     * Returns SQLite database for raw operations
     * You don't need to close it when you're done.
     * @return writable sqlite database
     */
    SQLiteDatabase getDatabase();
}
