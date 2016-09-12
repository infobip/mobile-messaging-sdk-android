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
        this.bundle = new Bundle();
        this.bundle.putString(BundleField.MESSAGE_ID.getKey(), this.messageId);
        this.bundle.putString(BundleField.STATUS.getKey(), this.status.getKey());
        this.bundle.putLong(BundleField.RECEIVED_TIMESTAMP.getKey(), this.receivedTimestamp);
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
            this.title = getSilentField(bundle, InternalDataField.TITLE.getKey());
            this.body = getSilentField(bundle, InternalDataField.BODY.getKey());
            this.sound = getSilentField(bundle, InternalDataField.SOUND.getKey());
            this.category = getSilentField(bundle, InternalDataField.CATEGORY.getKey());
        } else {
            this.title = bundle.getString(BundleField.TITLE.getKey());
            this.body = bundle.getString(BundleField.BODY.getKey());
            this.sound = bundle.getString(BundleField.SOUND.getKey());
            this.category = bundle.getString(BundleField.CATEGORY.getKey());
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

    public Actionable getActionable() {
        if (getInternalData() == null) {
            return null;
        }

        return new JsonSerializer().deserialize(getInternalData().toString(), Actionable.class);
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

    private static String getSilentField(Bundle from, String key) {
        JSONObject internalData = getJSON(from, BundleField.INTERNAL_DATA.getKey());
        if (internalData == null) {
            return null;
        }

        JSONObject silentData = internalData.optJSONObject(InternalDataField.SILENT_DATA.getKey());
        if (silentData == null) {
            return null;
        }

        return silentData.optString(key);
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

    public void setSeenTimestamp(long seenTimestamp) {
        this.seenTimestamp = seenTimestamp;
        this.bundle.putLong(BundleField.SEEN_TIMESTAMP.getKey(), seenTimestamp);
    }

    public void setTitle(String title) {
        this.title = title;
        this.bundle.putString(BundleField.TITLE.getKey(), title);
    }

    public void setInternalData(JSONObject internalData) {
        this.internalData = internalData;
        if (internalData != null) {
            this.bundle.putString(BundleField.INTERNAL_DATA.getKey(), internalData.toString());
        }
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
        this.bundle.putString(BundleField.MESSAGE_ID.getKey(), messageId);
    }

    public void setSound(String sound) {
        this.sound = sound;
        this.bundle.putString(BundleField.SOUND.getKey(), sound);
    }

    public void setSilent(String silent) {
        this.silent = "true".equalsIgnoreCase(silent);
        this.bundle.putString(BundleField.SILENT.getKey(), silent);
    }

    protected enum BundleField {
        MESSAGE_ID("gcm.notification.messageId"),
        TITLE("gcm.notification.title"),
        BODY("gcm.notification.body"),
        SOUND("gcm.notification.sound"),
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
