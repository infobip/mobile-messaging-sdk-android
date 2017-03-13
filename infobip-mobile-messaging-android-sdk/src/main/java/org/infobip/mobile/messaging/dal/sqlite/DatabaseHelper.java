package org.infobip.mobile.messaging.dal.sqlite;

import java.util.List;

/**
 * @author sslavin
 * @since 13/01/2017.
 */

public interface DatabaseHelper {
    /**
     * Finds all instances of specified type in database
     * @param cls object class
     * @param <T> object type
     * @return list of all objects in database
     */
    <T extends DatabaseContract.DatabaseObject> List<T> findAll(Class<T> cls);

    /**
     * Finds instance of object by primary key
     * @param cls object class
     * @param primaryKey object primary key
     * @param <T> object type
     * @return instance of object or null if not found
     */
    <T extends DatabaseContract.DatabaseObject> T find(Class<T> cls, String primaryKey);

    /**
     * Counts all instances of specified type in database
     * @param cls object class
     * @param <T> object type
     * @return number of all objects in database
     */
    <T extends DatabaseContract.DatabaseObject> long countAll(Class<T> cls);

    /**
     * Saves object to database
     * @param object object to save
     */
    void save(DatabaseContract.DatabaseObject object);

    /**
     * Deletes all objects of the specified type from the database
     * @param cls object class
     */
    <T extends DatabaseContract.DatabaseObject> void deleteAll(Class<T> cls);

    /**
     * Deletes object from database
     * @param cls object class
     * @param primaryKey object primary key
     */
    <T extends DatabaseContract.DatabaseObject> void delete(Class<T> cls, String primaryKey);

    /**
     * Deletes object from database
     * @param cls object class
     * @param primaryKeys object primary keys
     */
    <T extends DatabaseContract.DatabaseObject> void delete(Class<T> cls, String[] primaryKeys);
}