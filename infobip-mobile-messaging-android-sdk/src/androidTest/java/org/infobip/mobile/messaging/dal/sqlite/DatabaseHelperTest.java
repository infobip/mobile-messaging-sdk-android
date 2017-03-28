package org.infobip.mobile.messaging.dal.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotEquals;

/**
 * @author sslavin
 * @since 13/01/2017.
 */

public class DatabaseHelperTest extends MobileMessagingTestCase {

    private SQLiteDatabase database;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        database = databaseProvider.getDatabase();
        database.execSQL("DROP TABLE IF EXISTS '" + SomethingInDatabase.getTable() + "'");
        database.execSQL("CREATE TABLE '" + SomethingInDatabase.getTable() + "' (" +
                "'string_value' TEXT PRIMARY KEY," +
                "'long_value' INTEGER," +
                "'double_value' REAL)");
    }

    @Test
    public void test_shouldCreateObjectsInDatabase() throws Exception {
        Cursor cursor = database.rawQuery("SELECT * FROM '" + SomethingInDatabase.getTable() + "'", new String[0]);
        cursor.moveToFirst();
        assertEquals(0, cursor.getCount());
        cursor.close();

        databaseHelper.save(new SomethingInDatabase("SomeString", 12345L, 1234.5));
        databaseHelper.save(new SomethingInDatabase("SomeString1", 12346L, 1234.6));
        databaseHelper.save(new SomethingInDatabase("SomeString2", 12347L, 1234.7));

        cursor = database.rawQuery("SELECT * FROM '" + SomethingInDatabase.getTable() + "'", new String[0]);
        cursor.moveToFirst();
        assertEquals("SomeString", cursor.getString(cursor.getColumnIndex("string_value")));
        assertEquals(12345L, cursor.getLong(cursor.getColumnIndex("long_value")));
        assertEquals(1234.5, cursor.getDouble(cursor.getColumnIndex("double_value")), 0.001);
        cursor.moveToNext();
        assertEquals("SomeString1", cursor.getString(cursor.getColumnIndex("string_value")));
        assertEquals(12346L, cursor.getLong(cursor.getColumnIndex("long_value")));
        assertEquals(1234.6, cursor.getDouble(cursor.getColumnIndex("double_value")), 0.001);
        cursor.moveToNext();
        assertEquals("SomeString2", cursor.getString(cursor.getColumnIndex("string_value")));
        assertEquals(12347L, cursor.getLong(cursor.getColumnIndex("long_value")));
        assertEquals(1234.7, cursor.getDouble(cursor.getColumnIndex("double_value")), 0.001);
        assertFalse(cursor.moveToNext());
    }

    @Test
    public void test_shouldFindAllObjectsInDatabase() {
        UUID uuid = UUID.randomUUID();
        SomethingInDatabase something1 = new SomethingInDatabase("Something1" + uuid, 1, 2);
        SomethingInDatabase something2 = new SomethingInDatabase("Something2" + uuid, 3, 4);
        SomethingInDatabase something3 = new SomethingInDatabase("Something3" + uuid, 5, 6);
        database.insert(SomethingInDatabase.getTable(), null, something1.getContentValues());
        database.insert(SomethingInDatabase.getTable(), null, something2.getContentValues());
        database.insert(SomethingInDatabase.getTable(), null, something3.getContentValues());

        List<SomethingInDatabase> somethings = databaseHelper.findAll(SomethingInDatabase.class);

        assertEquals(3, somethings.size());
        assertNotEquals(something1, somethings.get(0));
        assertEquals("Something1" + uuid, somethings.get(0).stringValue);
        assertEquals(1, somethings.get(0).longValue);
        assertEquals(2, somethings.get(0).doubleValue, 0.001);
        assertNotEquals(something2, somethings.get(1));
        assertEquals("Something2" + uuid, somethings.get(1).stringValue);
        assertEquals(3, somethings.get(1).longValue);
        assertEquals(4, somethings.get(1).doubleValue, 0.001);
        assertNotEquals(something3, somethings.get(2));
        assertEquals("Something3" + uuid, somethings.get(2).stringValue);
        assertEquals(5, somethings.get(2).longValue);
        assertEquals(6, somethings.get(2).doubleValue, 0.001);
    }

    @Test
    public void test_shouldFindObjectInDatabaseById() {
        UUID uuid = UUID.randomUUID();
        SomethingInDatabase something1 = new SomethingInDatabase("Something1" + uuid, 1, 2);
        SomethingInDatabase something2 = new SomethingInDatabase("Something2" + uuid, 3, 4);
        SomethingInDatabase something3 = new SomethingInDatabase("Something3" + uuid, 5, 6);
        database.insert(SomethingInDatabase.getTable(), null, something1.getContentValues());
        database.insert(SomethingInDatabase.getTable(), null, something2.getContentValues());
        database.insert(SomethingInDatabase.getTable(), null, something3.getContentValues());

        SomethingInDatabase something1Found = databaseHelper.find(SomethingInDatabase.class, "Something1" + uuid);
        assertNotEquals(something1, something1Found);
        assertEquals("Something1" + uuid, something1Found.stringValue);
        assertEquals(1, something1Found.longValue);
        assertEquals(2, something1Found.doubleValue, 0.001);

        SomethingInDatabase something2Found = databaseHelper.find(SomethingInDatabase.class, "Something2" + uuid);
        assertNotEquals(something2, something2Found);
        assertEquals("Something2" + uuid, something2Found.stringValue);
        assertEquals(3, something2Found.longValue);
        assertEquals(4, something2Found.doubleValue, 0.001);

        SomethingInDatabase something3Found = databaseHelper.find(SomethingInDatabase.class, "Something3" + uuid);
        assertNotEquals(something3, something3Found);
        assertEquals("Something3" + uuid, something3Found.stringValue);
        assertEquals(5, something3Found.longValue);
        assertEquals(6, something3Found.doubleValue, 0.001);

        SomethingInDatabase somethingNotFound = databaseHelper.find(SomethingInDatabase.class, "SomethingNotFound" + uuid);
        assertNull(somethingNotFound);
    }

    @Test
    public void test_shouldDeleteObjectFromDatabaseById() {
        database.insert(SomethingInDatabase.getTable(), null, new SomethingInDatabase("Something1", 1, 2).getContentValues());
        database.insert(SomethingInDatabase.getTable(), null, new SomethingInDatabase("Something2", 3, 4).getContentValues());

        databaseHelper.delete(SomethingInDatabase.class, "Something1");

        Cursor cursor = database.rawQuery("SELECT * FROM '" + SomethingInDatabase.getTable() + "'", new String[0]);
        cursor.moveToFirst();
        assertEquals("Something2", cursor.getString(cursor.getColumnIndex("string_value")));
        assertEquals(3, cursor.getLong(cursor.getColumnIndex("long_value")));
        assertEquals(4, cursor.getDouble(cursor.getColumnIndex("double_value")), 0.001);
        assertFalse(cursor.moveToNext());
        cursor.close();
    }

    @Test
    public void test_shouldDeleteAllObjectsFromDatabase() {
        database.insert(SomethingInDatabase.getTable(), null, new SomethingInDatabase("Something1", 1, 2).getContentValues());
        database.insert(SomethingInDatabase.getTable(), null, new SomethingInDatabase("Something2", 1, 2).getContentValues());
        database.insert(SomethingInDatabase.getTable(), null, new SomethingInDatabase("Something3", 1, 2).getContentValues());

        databaseHelper.deleteAll(SomethingInDatabase.class);

        Cursor cursor = database.rawQuery("SELECT * FROM '" + SomethingInDatabase.getTable() + "'", new String[0]);
        cursor.moveToFirst();
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void test_shouldCountAllObjectsInDatabase() {
        int numOfSomethings = 100;
        for (int i = 0; i < numOfSomethings; i++) {
            database.insert(SomethingInDatabase.getTable(), null, new SomethingInDatabase("Something" + i, numOfSomethings + 1, numOfSomethings + 2).getContentValues());
        }

        assertEquals(100, databaseHelper.countAll(SomethingInDatabase.class));
    }
}