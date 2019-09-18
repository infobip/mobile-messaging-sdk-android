package org.infobip.mobile.messaging.dal.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * @author sslavin
 * @since 13/01/2017.
 */

class SomethingInDatabase implements DatabaseContract.DatabaseObject {

    String stringValue;
    long longValue;
    double doubleValue;

    SomethingInDatabase() {
    }

    SomethingInDatabase(String stringValue, long longValue, double doubleValue) {
        this.stringValue = stringValue;
        this.longValue = longValue;
        this.doubleValue = doubleValue;
    }

    @Override
    public String getTableName() {
        return "something_in_database";
    }

    @Override
    public String getPrimaryKeyColumnName() {
        return "string_value";
    }

    @Override
    public void fillFromCursor(Cursor cursor) throws Exception {
        stringValue = cursor.getString(cursor.getColumnIndex("string_value"));
        longValue = cursor.getLong(cursor.getColumnIndex("long_value"));
        doubleValue = cursor.getDouble(cursor.getColumnIndex("double_value"));
    }

    @Override
    public ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("string_value", stringValue);
        contentValues.put("long_value", longValue);
        contentValues.put("double_value", doubleValue);
        return contentValues;
    }

    static String getTable() {
        return new SomethingInDatabase().getTableName();
    }
}