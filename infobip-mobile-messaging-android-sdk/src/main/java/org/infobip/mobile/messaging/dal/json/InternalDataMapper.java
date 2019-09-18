package org.infobip.mobile.messaging.dal.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author sslavin
 * @since 03/02/2017.
 */

public class InternalDataMapper {

    private static final JsonSerializer serializer = new JsonSerializer(false);

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

    public static class InternalData<VibrateValueType> {
        Attachment[] atts = new Attachment[0];
        long sendDateTime;
        Silent<VibrateValueType> silent;
        String bulkId;
        String initialMessageId;
        @Deprecated Boolean inApp;
        Message.InAppStyle inAppStyle;

        public InternalData() {
        }

        public InternalData(long sendDateTime, String contentUrl) {
            this.sendDateTime = sendDateTime;
            if (contentUrl != null) {
                this.atts = createAttachments(contentUrl);
            }
        }
    }

    private static class Attachment {
        String t;
        String url;
    }

    /**
     * Creates internal data json based on message contents
     * <br> Note that boolean fields will be saved as String for FCM.
     *
     * @param message a message which to create internal data for
     * @return internal data json
     */
    public static String createInternalDataForFCMBasedOnMessageContents(@NonNull Message message) {
        return InternalDataMapper.<String>createInternalDataForMessage(message);
    }

    /**
     * Creates internal data json based on message contents
     *
     * @param message a message which to create internal data for
     * @return internal data json
     */
    public static String createInternalDataBasedOnMessageContents(@NonNull Message message) {
        return InternalDataMapper.<Boolean>createInternalDataForMessage(message);
    }

    /**
     * Updates message fields based on what is set in internal data (such as geo data and silent data)
     *
     * @param message          message to update
     * @param internalDataJson json object with internal data
     */
    public static void updateMessageWithInternalData(@NonNull Message message, String internalDataJson) {
        InternalData internalData = serializer.deserialize(internalDataJson, InternalData.class);

        if (internalData == null || internalData.silent == null) {
            return;
        }

        message.setTitle(internalData.silent.title);
        message.setBody(internalData.silent.body);
        message.setSound(internalData.silent.sound);
        if (internalData.silent.vibrate != null) {
            message.setVibrate(Boolean.valueOf(internalData.silent.vibrate.toString()));
        }
    }


    /**
     * Returns title from internal data
     *
     * @param json internal data json
     * @return title if present or null otherwise
     */
    public static String getInternalDataTitle(String json) {
        try {
            return serializer.deserialize(json, InternalData.class).silent.title;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns body from internal data
     *
     * @param json internal data json
     * @return body if present or null otherwise
     */
    public static String getInternalDataBody(String json) {
        try {
            return serializer.deserialize(json, InternalData.class).silent.body;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns sound from internal data
     *
     * @param json internal data json
     * @return sound if present or null otherwise
     */
    public static String getInternalDataSound(String json) {
        try {
            return serializer.deserialize(json, InternalData.class).silent.sound;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns vibrate flag from internal data
     *
     * @param json           internal data json
     * @param defaultVibrate value to return if no vibrate set in internal data
     * @return vibrate if present or defaultVibrate otherwise
     */
    public static boolean getInternalDataVibrate(String json, boolean defaultVibrate) {
        try {
            Object vibrate = serializer.deserialize(json, InternalData.class).silent.vibrate;
            return Boolean.valueOf(vibrate.toString());
        } catch (Exception e) {
            return defaultVibrate;
        }
    }

    /**
     * Returns category from internal data
     *
     * @param json internal data json
     * @return category if present or null otherwise
     */
    public static String getInternalDataCategory(String json) {
        try {
            return serializer.deserialize(json, InternalData.class).silent.category;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns content url from internal data
     *
     * @param json internal data json
     * @return content url if present or null otherwise
     */
    public static String getInternalDataContentUrl(String json) {
        try {
            return serializer.deserialize(json, InternalData.class).atts[0].url;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns send date time from internal data
     *
     * @param json internal data json
     * @return timestamp if present or 0 otherwise
     */
    public static long getInternalDataSendDateTime(String json) {
        try {
            return serializer.deserialize(json, InternalData.class).sendDateTime;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returns bulkId from internal data
     *
     * @param json internal data json
     * @return bulkId if present or null otherwise
     */
    public static String getInternalDataBulkId(String json) {
        try {
            return serializer.deserialize(json, InternalData.class).bulkId;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns initialMessageId from internal data
     *
     * @param json internal data json
     * @return initialMessageId if present or null otherwise
     */
    public static String getInternalDataInitialMessageId(String json) {
        try {
            return serializer.deserialize(json, InternalData.class).initialMessageId;
        } catch (Exception e) {
            return null;
        }
    }

    public static Message.InAppStyle getInternalDataInAppStyle(String json) {
       try {
            InternalData internalData = serializer.deserialize(json, InternalData.class);
            if (internalData.inAppStyle != null) {
                return internalData.inAppStyle;
            }

            if (internalData.inApp) {
                return Message.InAppStyle.MODAL;
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <VibrateValueType> String createInternalDataForMessage(Message message) {
        InternalData<VibrateValueType> internalData = serializer.deserialize(message.getInternalData(), InternalData.class);
        internalData = addContentUrlToInternalData(message, internalData);
        internalData = addSilentToInternalData(message, internalData);
        internalData = addSendDateTimeToInternalData(message, internalData);
        internalData = addInAppStyleToInternalData(message, internalData);
        return mergeExistingInternalDataWithAnythingToJson(message.getInternalData(), internalData);
    }

    @Nullable
    public static String mergeExistingInternalDataWithAnythingToJson(@Nullable String internalDataJson, @Nullable Object object) {
        if (internalDataJson == null && object == null) {
            return null;
        }

        if (internalDataJson == null || object == null) {
            return internalDataJson != null ? internalDataJson : serializer.serialize(object);
        }

        Map resultingMap = serializer.deserialize(internalDataJson, HashMap.class);
        String objectJson = serializer.serialize(object);
        Map objectMap = serializer.deserialize(objectJson, HashMap.class);
        //noinspection unchecked
        resultingMap.putAll(objectMap);
        return serializer.serialize(resultingMap);
    }

    @SuppressWarnings("unchecked")
    private static <VibrateValueType> InternalData<VibrateValueType> addSilentToInternalData(Message message, InternalData<VibrateValueType> internalData) {
        if (!message.isSilent()) {
            return internalData;
        }

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
        return internalData;
    }

    private static InternalData addContentUrlToInternalData(@NonNull Message message, InternalData internalData) {
        if (message.getContentUrl() == null) {
            return internalData;
        }

        if (internalData == null) {
            internalData = new InternalData<>();
        }

        internalData.atts = createAttachments(message.getContentUrl());
        return internalData;
    }

    private static InternalData addSendDateTimeToInternalData(@NonNull Message message, InternalData internalData) {
        if (message.getSentTimestamp() == 0) {
            return internalData;
        }

        if (internalData == null) {
            internalData = new InternalData<>();
        }

        internalData.sendDateTime = message.getSentTimestamp();
        return internalData;
    }

    private static InternalData addInAppStyleToInternalData(@NonNull Message message, InternalData internalData) {
        if (message.getInAppStyle() == null) {
            return internalData;
        }

        if (internalData == null) {
            internalData = new InternalData();
        }

        internalData.inAppStyle = message.getInAppStyle();
        internalData.inApp = message.getInAppStyle() == Message.InAppStyle.MODAL ? true : null;
        return internalData;
    }

    private static Attachment[] createAttachments(@NonNull String contentUrl) {
        Attachment[] atts = new Attachment[1];
        atts[0] = new Attachment();
        atts[0].url = contentUrl;
        return atts;
    }
}
