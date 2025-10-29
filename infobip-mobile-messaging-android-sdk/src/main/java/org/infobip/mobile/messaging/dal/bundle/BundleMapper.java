/*
 * BundleMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.dal.bundle;


import android.os.Bundle;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.json.JSONArrayAdapter;
import org.infobip.mobile.messaging.dal.json.JSONObjectAdapter;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class BundleMapper {

    protected static final JsonSerializer serializer = new JsonSerializer(false, new JSONObjectAdapter(), new JSONArrayAdapter());

    /**
     * De-serializes generic object from bundle.
     *
     * @param bundle where to load data from
     * @param tag    tag to use when reading data from bundle
     * @param <T>    object type
     * @return deserialized object instance
     */
    @Nullable
    protected static <T> T objectFromBundle(@NonNull Bundle bundle, @NonNull String tag, Class<T> cls) {
        String json = bundle.getString(tag);
        if (json == null) {
            return null;
        }
        return serializer.deserialize(json, cls);
    }

    /**
     * Serializes generic object into bundle
     *
     * @param object what to serialize
     * @param tag    tag to use when storing data in bundle
     * @return bundle that contains object data
     */
    @NonNull
    protected static Bundle objectToBundle(@NonNull Object object, @NonNull String tag) {
        Bundle bundle = new Bundle();
        bundle.putString(tag, serializer.serialize(object));
        return bundle;
    }

    /**
     * De-serializes list of objects from bundle
     *
     * @param bundle where to load data from
     * @param tag    tag to use when reading data from bundle
     * @param <T>    object type
     * @return list of deserialized objects
     */
    @NonNull
    protected static <T> List<T> objectsFromBundle(@NonNull Bundle bundle, @NonNull String tag, @NonNull Class<T> cls) {
        ArrayList<Bundle> bundles = bundle.getParcelableArrayList(tag);
        if (bundles == null) {
            return new ArrayList<>();
        }

        List<T> list = new ArrayList<>(bundles.size());
        for (Bundle b : bundles) {
            list.add(serializer.deserialize(b.getString(tag), cls));
        }
        return list;
    }

    /**
     * Serializes list of objects into bundle
     *
     * @param objects what to serialize
     * @param tag     tag to use when storing data to bundle
     * @return bundle with objects' contents
     */
    @NonNull
    protected static Bundle objectsToBundle(@NonNull List<?> objects, @NonNull String tag) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(tag, objectsToBundles(objects, tag));
        return bundle;
    }

    /**
     * De-serializes list of bundles into list of objects
     *
     * @param bundles where to load data from
     * @param tag     tag to use when storing data to bundle
     * @return bundles with objects' contents
     */
    @NonNull
    protected static <T> List<T> objectsFromBundles(@NonNull List<Bundle> bundles, @NonNull String tag, @NonNull Class<T> cls) {
        ArrayList<T> objects = new ArrayList<>(bundles.size());
        for (Bundle bundle : bundles) {
            objects.add(serializer.deserialize(bundle.getString(tag), cls));
        }
        return objects;
    }

    /**
     * Serializes list of objects into list of bundles
     *
     * @param objects what to serialize
     * @param tag     tag to use when storing data to bundle
     * @return bundles with objects' contents
     */
    @NonNull
    protected static ArrayList<Bundle> objectsToBundles(@NonNull List<?> objects, @NonNull String tag) {
        ArrayList<Bundle> bundles = new ArrayList<>(objects.size());
        for (Object object : objects) {
            Bundle bundle = new Bundle();
            bundle.putString(tag, serializer.serialize(object));
            bundles.add(bundle);
        }

        return bundles;
    }
}
