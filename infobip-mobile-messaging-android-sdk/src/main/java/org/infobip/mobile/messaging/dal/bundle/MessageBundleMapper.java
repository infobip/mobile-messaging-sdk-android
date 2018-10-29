package org.infobip.mobile.messaging.dal.bundle;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.infobip.mobile.messaging.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 02/02/2017.
 */

public class MessageBundleMapper extends BundleMapper {

    private static final String BUNDLED_MESSAGE_TAG = MessageBundleMapper.class.getName() + ".message";

    /**
     * De-serializes message object from bundle
     *
     * @param bundle where to load data from
     * @return new message object
     */
    @Nullable
    public static Message messageFromBundle(@NonNull Bundle bundle) {
        return objectFromBundle(bundle, BUNDLED_MESSAGE_TAG, Message.class);
    }

    /**
     * De-serializes list of bundles into list of messages
     *
     * @param bundles where to read messages from
     * @return list of messages
     */
    @NonNull
    public static List<Message> messagesFromBundles(@NonNull ArrayList<Bundle> bundles) {
        return objectsFromBundles(bundles, BUNDLED_MESSAGE_TAG, Message.class);
    }

    /**
     * Serializes message object into bundle
     *
     * @param message object to serialize
     * @return bundle with message contents
     */
    @NonNull
    public static Bundle messageToBundle(@NonNull Message message) {
        return objectToBundle(message, BUNDLED_MESSAGE_TAG);
    }

    /**
     * Serializes list of messages into list of bundles
     *
     * @param messages objects to serialize
     * @return list of bundles with messages' contents
     */
    @NonNull
    public static ArrayList<Bundle> messagesToBundles(@NonNull List<Message> messages) {
        return objectsToBundles(messages, BUNDLED_MESSAGE_TAG);
    }
}