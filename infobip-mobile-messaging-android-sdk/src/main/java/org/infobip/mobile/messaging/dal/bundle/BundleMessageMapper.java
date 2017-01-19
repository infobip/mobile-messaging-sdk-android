package org.infobip.mobile.messaging.dal.bundle;

import android.os.Bundle;
import android.util.Log;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.geo.Geo;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 29/12/2016.
 */

public class BundleMessageMapper {

    private static final String TAG = BundleMessageMapper.class.getSimpleName();

    public static Message fromBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }

        boolean silent = "true".equals(bundle.getString(BundleField.SILENT.getKey()));
        boolean vibrate = silent ? getSilentField(bundle, InternalDataField.VIBRATE.getKey(), true) : "true".equals(bundle.getString(BundleField.VIBRATE.getKey(), "true"));
        String messageId = bundle.getString(BundleField.MESSAGE_ID.getKey());
        String title = silent ? (String) getSilentField(bundle, InternalDataField.TITLE.getKey()) : bundle.getString(BundleField.TITLE.getKey());
        String body = silent ? (String) getSilentField(bundle, InternalDataField.BODY.getKey()) : bundle.getString(BundleField.BODY.getKey());
        String sound = silent ? (String) getSilentField(bundle, InternalDataField.SOUND.getKey()) : bundle.getString(BundleField.SOUND2.getKey(), bundle.getString(BundleField.SOUND.getKey()));
        String icon = bundle.getString(BundleField.ICON.getKey());
        String category = silent ? (String) getSilentField(bundle, InternalDataField.CATEGORY.getKey()) : bundle.getString(BundleField.CATEGORY.getKey());
        String from = bundle.getString(BundleField.FROM.getKey());
        long receivedTs = bundle.getLong(BundleField.RECEIVED_TIMESTAMP.getKey());
        long seenTs = bundle.getLong(BundleField.SEEN_TIMESTAMP.getKey());
        JSONObject internalData = getJSON(bundle, BundleField.INTERNAL_DATA.getKey());
        JSONObject customPayload = getJSON(bundle, BundleField.CUSTOM_PAYLOAD.getKey());

        Geo geo = null;
        try {
            geo = internalData != null ? new JsonSerializer().deserialize(internalData.toString(), Geo.class) : null;
        } catch (Exception ignored) {}

        String destination = bundle.getString(BundleField.DESTINATION.getKey());
        String statusMessage = bundle.getString(BundleField.STATUS_MESSAGE.getKey());
        Message.Status status = Message.Status.UNKNOWN;
        try {
            status = Message.Status.valueOf(bundle.getString(BundleField.STATUS.getKey()));
        } catch (Exception ignored) { }

        return new Message(messageId, title, body, sound, vibrate, icon, silent, category, from, receivedTs, seenTs, internalData, customPayload, geo, destination, status, statusMessage);
    }

    public static List<Message> fromBundles(ArrayList<Bundle> bundles) {
        List<Message> messages = new ArrayList<>();
        for (Bundle bundle : bundles) {
            messages.add(fromBundle(bundle));
        }
        return messages;
    }

    public static Bundle toBundle(Message message) {
        if (message == null) {
            return null;
        }

        Bundle bundle = new Bundle();
        bundle.putString(BundleField.MESSAGE_ID.getKey(), message.getMessageId());
        bundle.putString(BundleField.SILENT.getKey(), message.isSilent() ? "true" : "false");
        bundle.putString(BundleField.TITLE.getKey(), message.getTitle());
        bundle.putString(BundleField.BODY.getKey(), message.getBody());
        bundle.putString(BundleField.SOUND.getKey(), message.getSound());
        bundle.putString(BundleField.SOUND2.getKey(), message.getSound());
        bundle.putString(BundleField.VIBRATE.getKey(), message.isVibrate() ? "true" : "false");
        bundle.putString(BundleField.ICON.getKey(), message.getIcon());
        bundle.putString(BundleField.CATEGORY.getKey(), message.getCategory());
        bundle.putString(BundleField.FROM.getKey(), message.getFrom());
        bundle.putLong(BundleField.RECEIVED_TIMESTAMP.getKey(), message.getReceivedTimestamp());
        bundle.putLong(BundleField.SEEN_TIMESTAMP.getKey(), message.getSeenTimestamp());
        bundle.putString(BundleField.INTERNAL_DATA.getKey(), message.getInternalData() != null ? message.getInternalData().toString() : null);
        bundle.putString(BundleField.CUSTOM_PAYLOAD.getKey(), message.getCustomPayload() != null ? message.getCustomPayload().toString() : null);
        bundle.putString(BundleField.DESTINATION.getKey(), message.getDestination());
        bundle.putString(BundleField.STATUS.getKey(), message.getStatus().name());
        bundle.putString(BundleField.STATUS_MESSAGE.getKey(), message.getStatusMessage());
        return bundle;
    }

    public static ArrayList<Bundle> toBundles(List<Message> messages) {
        ArrayList<Bundle> bundles = new ArrayList<>();
        for (Message message : messages) {
            bundles.add(toBundle(message));
        }
        return bundles;
    }

    private static <T> T getSilentField(Bundle bundle, String key) {
        return getSilentField(bundle, key, null);
    }

    private static <T> T getSilentField(Bundle bundle, String key, T defaultValue) {
        JSONObject internalData = getJSON(bundle, BundleField.INTERNAL_DATA.getKey());
        if (internalData == null) {
            return defaultValue;
        }

        JSONObject silentData = internalData.optJSONObject(InternalDataField.SILENT_DATA.getKey());
        if (silentData == null) {
            return defaultValue;
        }

        Object o = silentData.opt(key);
        if (o == null) {
            return defaultValue;
        }

        try {
            return (T) o;
        } catch (Exception e) {
            MobileMessagingLogger.d(TAG, Log.getStackTraceString(e));
            return defaultValue;
        }
    }

    private static JSONObject getJSON(Bundle from, String key) {
        String string = from.getString(key);
        if (string == null) {
            return null;
        }

        try {
            return new JSONObject(string);
        } catch (JSONException e) {
            MobileMessagingLogger.w(TAG, "Cannot parse (" + key + "): " + e.getMessage());
            MobileMessagingLogger.d(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    private enum BundleField {
        MESSAGE_ID("gcm.notification.messageId"),
        TITLE("gcm.notification.title"),
        BODY("gcm.notification.body"),
        SOUND("gcm.notification.sound"),
        SOUND2("gcm.notification.sound2"),
        VIBRATE("gcm.notification.vibrate"),
        ICON("gcm.notification.icon"),
        SILENT("gcm.notification.silent"),
        CATEGORY("gcm.notification.category"),
        FROM("from"),
        RECEIVED_TIMESTAMP("received_timestamp"),
        SEEN_TIMESTAMP("seen_timestamp"),
        INTERNAL_DATA("internalData"),
        CUSTOM_PAYLOAD("customPayload"),

        STATUS("status"),
        STATUS_MESSAGE("status_message"),
        DESTINATION("destination");

        private final String key;

        BundleField(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    private enum InternalDataField {
        SILENT_DATA("silent"),
        TITLE("title"),
        BODY("body"),
        SOUND("sound"),
        VIBRATE("vibrate"),
        CATEGORY("category");

        private final String key;

        InternalDataField(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
