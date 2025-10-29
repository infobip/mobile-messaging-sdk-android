/*
 * StoredMessageMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.storage;

import android.os.Bundle;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.json.InternalDataMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.json.JSONException;
import org.json.JSONObject;

@Deprecated
public class StoredMessageMapper {

    private static final String TAG = StoredMessageMapper.class.getSimpleName();

    /**
     * De-serializes Push message from the Bundle we receive by pulling messages from server
     *
     * @param bundle data from the intent
     * @return deserialized message
     */
    public static Message fromCloudBundle(Bundle bundle) {
        if (bundle == null) {
            return null;
        }

        boolean silent = "true".equals(bundle.getString(BundleField.SILENT.getKey()));
        String messageId = bundle.getString(BundleField.MESSAGE_ID.getKey());
        String icon = bundle.getString(BundleField.ICON.getKey());
        String from = bundle.getString(BundleField.FROM.getKey());
        long receivedTs = bundle.getLong(BundleField.RECEIVED_TIMESTAMP.getKey());
        long seenTs = bundle.getLong(BundleField.SEEN_TIMESTAMP.getKey());
        JSONObject customPayload = getJSON(bundle, BundleField.CUSTOM_PAYLOAD.getKey());

        String internalDataJson = bundle.getString(BundleField.INTERNAL_DATA.getKey());
        boolean vibrate = silent ? InternalDataMapper.getInternalDataVibrate(internalDataJson, true) : "true".equals(bundle.getString(BundleField.VIBRATE.getKey(), "true"));
        String title = silent ? InternalDataMapper.getInternalDataTitle(internalDataJson) : bundle.getString(BundleField.TITLE.getKey());
        String body = silent ? InternalDataMapper.getInternalDataBody(internalDataJson) : bundle.getString(BundleField.BODY.getKey());
        String sound = silent ? InternalDataMapper.getInternalDataSound(internalDataJson) : bundle.getString(BundleField.SOUND2.getKey(), bundle.getString(BundleField.SOUND.getKey()));
        String category = silent ? InternalDataMapper.getInternalDataCategory(internalDataJson) : bundle.getString(BundleField.CATEGORY.getKey());
        String contentUrl = InternalDataMapper.getInternalDataContentUrl(internalDataJson);
        long sentDateTime = InternalDataMapper.getInternalDataSendDateTime(internalDataJson);
        long inAppExpiryDateTime = InternalDataMapper.getInternalDataInAppExpiryDateTime(internalDataJson);
        String webViewUrl = InternalDataMapper.getInternalDataWebViewUrl(internalDataJson);
        String browserUrl = InternalDataMapper.getInternalDataBrowserUrl(internalDataJson);
        String deeplink = InternalDataMapper.getInternalDataDeeplinkUri(internalDataJson);
        String messageType = InternalDataMapper.getInternalDataMessageType(internalDataJson);
        String inAppOpenTitle = InternalDataMapper.getInternalDataInAppOpenTitle(internalDataJson);
        String inAppDismissTitle = InternalDataMapper.getInternalDataInAppDismissTitle(internalDataJson);

        String destination = bundle.getString(BundleField.DESTINATION.getKey());
        String statusMessage = bundle.getString(BundleField.STATUS_MESSAGE.getKey());
        Message.Status status = Message.Status.UNKNOWN;
        try {
            status = Message.Status.valueOf(bundle.getString(BundleField.STATUS.getKey()));
        } catch (Exception ignored) {
        }
        Message.InAppStyle inAppStyle = InternalDataMapper.getInternalDataInAppStyle(internalDataJson);

        return new Message(messageId, title, body, sound,
                vibrate, icon, silent, category, from,
                receivedTs, seenTs, sentDateTime, customPayload,
                internalDataJson, destination, status, statusMessage,
                contentUrl, inAppStyle, inAppExpiryDateTime, webViewUrl, browserUrl,
                messageType, deeplink, inAppOpenTitle, inAppDismissTitle);
    }

    /**
     * Serializes message to the same bundle as we receive if by pulling messages from server
     *
     * @param message message to serialize
     * @return bundle with message contents
     */
    public static Bundle toCloudBundle(Message message) {
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
        bundle.putString(BundleField.INTERNAL_DATA.getKey(), InternalDataMapper.createInternalDataForFCMBasedOnMessageContents(message));
        bundle.putString(BundleField.CUSTOM_PAYLOAD.getKey(), message.getCustomPayload() != null ? message.getCustomPayload().toString() : null);
        bundle.putString(BundleField.DESTINATION.getKey(), message.getDestination());
        bundle.putString(BundleField.STATUS.getKey(), message.getStatus().name());
        bundle.putString(BundleField.STATUS_MESSAGE.getKey(), message.getStatusMessage());
        return bundle;
    }

    private static JSONObject getJSON(Bundle from, String key) {
        String string = from.getString(key);
        if (string == null) {
            return null;
        }

        try {
            return new JSONObject(string);
        } catch (JSONException e) {
            MobileMessagingLogger.e(TAG, "Cannot parse (" + key + "): " + e.getMessage(), e);
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
}