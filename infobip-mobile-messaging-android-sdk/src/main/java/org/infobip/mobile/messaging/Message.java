package org.infobip.mobile.messaging;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Message bundle adapter. Offers convenience methods to extract and save message data to a bundle.
 *
 * @author sslavin
 * @since 05/09/16.
 */
public class Message implements Comparable<Message> {

    Bundle bundle;
    String messageId;
    String title;
    String body;
    String sound;
    boolean vibrate;
    String icon;
    boolean silent;
    String category;
    String from;
    Long receivedTimestamp;
    Long seenTimestamp;
    JSONObject internalData;
    JSONObject customPayload;
    String destination;
    Status status;
    String statusMessage;

    public static Message createFrom(Bundle bundle) {
        return new Message(bundle);
    }

    public static List<Message> createFrom(ArrayList<Bundle> bundles) {
        List<Message> messages = new ArrayList<>();
        for (Bundle bundle : bundles) {
            messages.add(Message.createFrom(bundle));
        }
        return messages;
    }

    public Message() {
        this.messageId = UUID.randomUUID().toString();
        this.status = Status.UNKNOWN;
        this.receivedTimestamp = System.currentTimeMillis();
        this.seenTimestamp = System.currentTimeMillis();
        this.vibrate = true;
        this.silent = false;

        this.bundle = new Bundle();
        this.bundle.putString(BundleField.MESSAGE_ID.getKey(), this.messageId);
        this.bundle.putString(BundleField.STATUS.getKey(), this.status.getKey());
        this.bundle.putLong(BundleField.RECEIVED_TIMESTAMP.getKey(), this.receivedTimestamp);
        this.bundle.putLong(BundleField.SEEN_TIMESTAMP.getKey(), this.seenTimestamp);
        this.bundle.putString(BundleField.VIBRATE.getKey(), Boolean.toString(this.vibrate));
        this.bundle.putString(BundleField.SILENT.getKey(), Boolean.toString(this.silent));
    }

    private Message(Bundle bundle) {

        this.bundle = bundle;
        this.silent = "true".equals(bundle.getString(BundleField.SILENT.getKey()));
        this.messageId = bundle.getString(BundleField.MESSAGE_ID.getKey());
        this.icon = bundle.getString(BundleField.ICON.getKey());
        this.from = bundle.getString(BundleField.FROM.getKey());
        this.receivedTimestamp = bundle.getLong(BundleField.RECEIVED_TIMESTAMP.getKey());
        this.seenTimestamp = bundle.getLong(BundleField.SEEN_TIMESTAMP.getKey());
        this.internalData = getJSON(bundle, BundleField.INTERNAL_DATA.getKey());
        this.customPayload = getJSON(bundle, BundleField.CUSTOM_PAYLOAD.getKey());
        this.destination = bundle.getString(BundleField.DESTINATION.getKey());
        try {
            this.status = Status.valueOf(bundle.getString(BundleField.STATUS.getKey()));
        } catch (Exception ignored) {
            this.status = Status.UNKNOWN;
        }
        this.statusMessage = bundle.getString(BundleField.STATUS_MESSAGE.getKey());

        if (this.silent) {
            this.title = getSilentField(InternalDataField.TITLE.getKey());
            this.body = getSilentField(InternalDataField.BODY.getKey());
            this.sound = getSilentField(InternalDataField.SOUND.getKey());
            this.category = getSilentField(InternalDataField.CATEGORY.getKey());
            this.vibrate = getSilentField(InternalDataField.VIBRATE.getKey(), true);
        } else {
            this.title = bundle.getString(BundleField.TITLE.getKey());
            this.body = bundle.getString(BundleField.BODY.getKey());
            this.sound = bundle.getString(BundleField.SOUND2.getKey(),
                    bundle.getString(BundleField.SOUND.getKey()));
            this.category = bundle.getString(BundleField.CATEGORY.getKey());
            this.vibrate = "true".equals(bundle.getString(BundleField.VIBRATE.getKey(), "true"));
        }
    }

    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public int compareTo(@NonNull Message another) {
        return (int) Math.signum(another.getReceivedTimestamp() - getReceivedTimestamp());
    }

    public String getMessageId() {
        return messageId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getSound() {
        return sound;
    }

    public boolean isDefaultSound() {
        return "default".equals(sound);
    }

    public String getIcon() {
        return icon;
    }

    public boolean isSilent() {
        return silent;
    }

    public String getCategory() {
        return category;
    }

    public String getFrom() {
        return from;
    }

    public Long getReceivedTimestamp() {
        return receivedTimestamp;
    }

    public Long getSeenTimestamp() {
        return seenTimestamp;
    }

    public JSONObject getInternalData() {
        return internalData;
    }

    public JSONObject getCustomPayload() {
        return customPayload;
    }

    public String getDestination() {
        return destination;
    }

    public Status getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public List<GeofenceAreas.Area> getGeofenceAreasList() {
        if (getInternalData() == null) {
            return new ArrayList<>(0);
        }

        try {
            GeofenceAreas geofenceAreas = new JsonSerializer().deserialize(getInternalData().toString(), GeofenceAreas.class);
            return geofenceAreas.getAreasList();
        } catch (Exception e) {
            Log.e(MobileMessaging.TAG, e.getMessage(), e);
            return new ArrayList<>(0);
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
            e.printStackTrace();
            return null;
        }
    }

    private <T> T getSilentField(String key) {
        return getSilentField(key, null);
    }

    private <T> T getSilentField(String key, T defaultValue) {
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
            e.printStackTrace();
            return defaultValue;
        }
    }

    public void setFrom(String from) {
        this.from = from;
        this.bundle.putString(BundleField.FROM.getKey(), from);
    }

    public void setBody(String body) {
        this.body = body;
        this.bundle.putString(BundleField.BODY.getKey(), body);
    }

    public void setDestination(String destination) {
        this.destination = destination;
        this.bundle.putString(BundleField.DESTINATION.getKey(), destination);
    }

    public void setCustomPayload(JSONObject customPayload) {
        this.customPayload = customPayload;
        if (customPayload != null) {
            this.bundle.putString(BundleField.CUSTOM_PAYLOAD.getKey(), customPayload.toString());
        }
    }

    public void setInternalData(JSONObject internalData) {
        this.internalData = internalData;
        if (internalData != null) {
            this.bundle.putString(BundleField.INTERNAL_DATA.getKey(), internalData.toString());
        }
    }

    public void setSeenTimestamp(long seenTimestamp) {
        this.seenTimestamp = seenTimestamp;
        this.bundle.putLong(BundleField.SEEN_TIMESTAMP.getKey(), seenTimestamp);
    }

    public void setTitle(String title) {
        this.title = title;
        this.bundle.putString(BundleField.TITLE.getKey(), title);
    }

    protected enum BundleField {
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

    protected enum InternalDataField {
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

    public enum Status {
        SUCCESS("SUCCESS"),
        ERROR("ERROR"),
        UNKNOWN("UNKNOWN");

        private final String key;

        Status(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
