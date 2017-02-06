package org.infobip.mobile.messaging.dal.bundle;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoReport;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 02/02/2017.
 */

public class BundleMapper {

    private static final String BUNDLED_MESSAGE_TAG = BundleMapper.class.getName() + ".message";
    private static final String BUNDLED_GEO_TAG = BundleMapper.class.getName() + ".geo";
    private static final String BUNDLED_GEO_REPORTS_TAG = BundleMapper.class.getName() + ".geo.report";

    private static final JsonSerializer serializer = new JsonSerializer();

    /**
     * De-serializes message object from bundle
     * @param bundle where to load data from
     * @return new message object
     */
    public static Message messageFromBundle(@NonNull Bundle bundle) {
        return objectFromBundle(bundle, BUNDLED_MESSAGE_TAG, Message.class);
    }

    /**
     * De-serializes list of bundles into list of messages
     * @param bundles where to read messages from
     * @return list of messages
     */
    public static List<Message> messagesFromBundles(ArrayList<Bundle> bundles) {
        return objectsFromBundles(bundles, BUNDLED_MESSAGE_TAG, Message.class);
    }

    /**
     * Serializes message object into bundle
     * @param message object to serialize
     * @return bundle with message contents
     */
    public static Bundle messageToBundle(Message message) {
        return objectToBundle(message, BUNDLED_MESSAGE_TAG);
    }

    /**
     * Serializes list of messages into list of bundles
     * @param messages objects to serialize
     * @return list of bundles with messages' contents
     */
    public static ArrayList<Bundle> messagesToBundles(List<Message> messages) {
        return objectsToBundles(messages, BUNDLED_MESSAGE_TAG);
    }

    /**
     * De-serializes Geo object from bundle
     * @param bundle where to load data from
     * @return new geo object
     */
    public static Geo geoFromBundle(@NonNull Bundle bundle) {
        return objectFromBundle(bundle, BUNDLED_GEO_TAG, Geo.class);
    }

    /**
     * Serializes geo object into bundle
     * @param geo object to serialize
     * @return bundle with geo contents
     */
    public static Bundle geoToBundle(@NonNull Geo geo) {
        return objectToBundle(geo, BUNDLED_GEO_TAG);
    }

    /**
     * De-serializes geo reports from bundle
     * @param bundle where to load data from
     * @return new geo report object
     */
    public static List<GeoReport> geoReportsFromBundle(@NonNull Bundle bundle) {
        return objectsFromBundle(bundle, BUNDLED_GEO_REPORTS_TAG, GeoReport.class);
    }

    /**
     * Serializes geo reports into bundle
     * @param reports geo reports to serialize
     * @return bundle with geo reports' contents
     */
    public static Bundle geoReportsToBundle(@NonNull List<GeoReport> reports) {
        return objectsToBundle(reports, BUNDLED_GEO_REPORTS_TAG);
    }

    // --------------------------------------------------------
    // Generic serialization and deserialization methods below
    // --------------------------------------------------------

    /**
     * De-serializes generic object from bundle.
     * @param bundle where to load data from
     * @param tag tag to use when reading data from bundle
     * @param <T> object type
     * @return deserialized object instance
     */
    private static <T> T objectFromBundle(@NonNull Bundle bundle, @NonNull String tag, Class<T> cls) {
        String json = bundle.getString(tag);
        if (json == null) {
            return null;
        }
        return serializer.deserialize(json, cls);
    }

    /**
     * Serializes generic object into bundle
     * @param object what to serialize
     * @param tag tag to use when storing data in bundle
     * @return bundle that contains object data
     */
    private static Bundle objectToBundle(@NonNull Object object, @NonNull String tag) {
        Bundle bundle = new Bundle();
        bundle.putString(tag, serializer.serialize(object));
        return bundle;
    }

    /**
     * De-serializes list of objects from bundle
     * @param bundle where to load data from
     * @param tag tag to use when reading data from bundle
     * @param <T> object type
     * @return list of deserialized objects
     */
    private static <T> List<T> objectsFromBundle(@NonNull Bundle bundle, @NonNull String tag, Class<T> cls) {
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
     * @param objects what to serialize
     * @param tag tag to use when storing data to bundle
     * @return bundle with objects' contents
     */
    private static Bundle objectsToBundle(@NonNull List<?> objects, @NonNull String tag) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(tag, objectsToBundles(objects, tag));
        return bundle;
    }

    /**
     * De-serializes list of bundles into list of objects
     * @param bundles where to load data from
     * @param tag tag to use when storing data to bundle
     * @return bundles with objects' contents
     */
    private static <T> List<T> objectsFromBundles(@NonNull List<Bundle> bundles, @NonNull String tag, Class<T> cls) {
        ArrayList<T> objects = new ArrayList<>(bundles.size());
        for (Bundle bundle : bundles) {
            objects.add(serializer.deserialize(bundle.getString(tag), cls));
        }
        return objects;
    }

    /**
     * Serializes list of objects into list of bundles
     * @param objects what to serialize
     * @param tag tag to use when storing data to bundle
     * @return bundles with objects' contents
     */
    private static ArrayList<Bundle> objectsToBundles(@NonNull List<?> objects, @NonNull String tag) {
        ArrayList<Bundle> bundles = new ArrayList<>(objects.size());
        for (Object object : objects) {
            Bundle bundle = new Bundle();
            bundle.putString(tag, serializer.serialize(object));
            bundles.add(bundle);
        }

        return bundles;
    }
}