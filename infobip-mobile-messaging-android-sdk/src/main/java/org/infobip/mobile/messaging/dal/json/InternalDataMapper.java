package org.infobip.mobile.messaging.dal.json;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.Geo;

import java.util.ArrayList;

/**
 * @author sslavin
 * @since 03/02/2017.
 */

public class InternalDataMapper {

    private static final JsonSerializer serializer = new JsonSerializer();

    /**
     * @param <VibrateValueType> vibrate comes as String from FCM and as Boolean from Infobip Services
     */
    private static class Silent<VibrateValueType> {
        String title;
        String body;
        String sound;
        VibrateValueType vibrate;
        String category;
    }

    private static class InternalData<VibrateValueType> extends Geo {

        InternalData() {
            super(null, null, null, null, null, null, new ArrayList<Area>(), null);
        }

        Silent<VibrateValueType> silent;
    }

    /**
     * Creates internal data json based on message contents
     * </p> Note that boolean fields will be saved as String for FCM.
     * @param message a message which to create internal data for
     * @return internal data json
     */
    public static String createInternalDataForFCMBasedOnMessageContents(@NonNull Message message) {
        return InternalDataMapper.<String>createInternalDataForMessage(message);
    }

    /**
     * Creates internal data json based on message contents
     * @param message a message which to create internal data for
     * @return internal data json
     */
    public static String createInternalDataBasedOnMessageContents(@NonNull Message message) {
        return InternalDataMapper.<Boolean>createInternalDataForMessage(message);
    }

    /**
     * Updates message fields based on what is set in internal data (such as geo data and silent data)
     * @param message message to update
     * @param internalDataJson json object with internal data
     */
    public static void updateMessageWithInternalData(@NonNull Message message, String internalDataJson) {
        InternalData internalData = serializer.deserialize(internalDataJson, InternalData.class);
        message.setGeo(internalData);

        if (internalData == null || internalData.silent == null) {
            return;
        }

        message.setTitle(internalData.silent.title);
        message.setBody(internalData.silent.body);
        message.setSound(internalData.silent.sound);
        message.setVibrate(Boolean.valueOf(internalData.silent.vibrate.toString()));
    }


    /**
     * Returns title from internal data
     * @param json internal data json
     * @return title if present or null otherwise
     */
    public static String getInternalDataTitle(String json) {
        try {
            return new JsonSerializer().deserialize(json, InternalData.class).silent.title;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns body from internal data
     * @param json internal data json
     * @return body if present or null otherwise
     */
    public static String getInternalDataBody(String json) {
        try {
            return new JsonSerializer().deserialize(json, InternalData.class).silent.body;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns sound from internal data
     * @param json internal data json
     * @return sound if present or null otherwise
     */
    public static String getInternalDataSound(String json) {
        try {
            return new JsonSerializer().deserialize(json, InternalData.class).silent.sound;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns vibrate flag from internal data
     * @param json internal data json
     * @param defaultVibrate value to return if no vibrate set in internal data
     * @return vibrate if present or defaultVibrate otherwise
     */
    public static boolean getInternalDataVibrate(String json, boolean defaultVibrate) {
        try {
            Object vibrate = new JsonSerializer().deserialize(json, InternalData.class).silent.vibrate;
            return Boolean.valueOf(vibrate.toString());
        } catch (Exception e) {
            return defaultVibrate;
        }
    }

    /**
     * Returns category from internal data
     * @param json internal data json
     * @return category if present or null otherwise
     */
    public static String getInternalDataCategory(String json) {
        try {
            return new JsonSerializer().deserialize(json, InternalData.class).silent.category;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <VibrateValueType> String createInternalDataForMessage(Message message) {
        InternalData<VibrateValueType> internalData = null;
        if (message.getGeo() != null) {
            internalData = serializer.deserialize(
                    serializer.serialize(message.getGeo()), InternalData.class);
        }

        if (message.isSilent()) {
            if (internalData == null) {
                internalData = new InternalData<>();
            }
            if (internalData.silent == null) {
                internalData.silent = new Silent<>();
            }
            internalData.silent.title = message.getTitle();
            internalData.silent.body = message.getBody();
            internalData.silent.sound = message.getSound();
            if (internalData.silent.vibrate instanceof Boolean) {
                internalData.silent.vibrate = (VibrateValueType) Boolean.valueOf(message.isVibrate());
            } else if (internalData.silent.vibrate instanceof String) {
                internalData.silent.vibrate = (VibrateValueType) (message.isVibrate() ? "true" : "false");
            }
            internalData.silent.category = message.getCategory();
        }

        return internalData != null ? serializer.serialize(internalData) : null;
    }
}
