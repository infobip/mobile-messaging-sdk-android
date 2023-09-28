package org.infobip.mobile.messaging.interactive.inapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.json.JSONArrayAdapter;
import org.infobip.mobile.messaging.dal.json.JSONObjectAdapter;
import org.json.JSONException;
import org.json.JSONObject;

public class InAppWebViewMessage extends Message {

    @NonNull
    public String url;
    @Nullable
    public InAppWebViewPosition position;
    @NonNull
    public InAppWebViewType type;

    private static final JsonSerializer serializer = new JsonSerializer(false, new JSONObjectAdapter(), new JSONArrayAdapter());

    public enum InAppWebViewPosition {
        @SerializedName(value = "TOP", alternate = "0")
        TOP,
        @SerializedName(value = "BOTTOM", alternate = "1")
        BOTTOM,
    }

    public enum InAppWebViewType {
        @SerializedName(value = "BANNER", alternate = "0")
        BANNER,
        @SerializedName(value = "POPUP", alternate = "1")
        POPUP,
        @SerializedName(value = "FULLSCREEN", alternate = "2")
        FULLSCREEN
    }

    public static InAppWebViewMessage createInAppWebViewMessage(Message message) {
        if (message == null) {
            return null;
        }

        JSONObject internalData;
        JSONObject inAppDetails = null;

        if (message.getInternalData() != null) {
            try {
                internalData = new JSONObject(message.getInternalData());
                inAppDetails = internalData.optJSONObject("inAppDetails");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
        if (inAppDetails == null) return null;

        InAppWebViewMessage inAppWebViewMessage = serializer.deserialize(inAppDetails.toString(), InAppWebViewMessage.class);

        if (inAppWebViewMessage.type == null || inAppWebViewMessage.type.ordinal() > 2)
            return null;
        if (inAppWebViewMessage.position == null)
            inAppWebViewMessage.position = InAppWebViewPosition.TOP;

        inAppWebViewMessage.setMessageId(message.getMessageId());
        inAppWebViewMessage.setBody(message.getBody());
        inAppWebViewMessage.setBrowserUrl(message.getBrowserUrl());
        inAppWebViewMessage.setMessageType(message.getMessageType());
        inAppWebViewMessage.setCategory(message.getCategory());
        inAppWebViewMessage.setContentUrl(message.getContentUrl());
        inAppWebViewMessage.setCustomPayload(message.getCustomPayload());
        inAppWebViewMessage.setDeeplink(message.getDeeplink());
        inAppWebViewMessage.setDestination(message.getDestination());
        inAppWebViewMessage.setFrom(message.getFrom());
        inAppWebViewMessage.setIcon(message.getIcon());
        inAppWebViewMessage.setInAppDismissTitle(message.getInAppDismissTitle());
        inAppWebViewMessage.setInAppExpiryTimestamp(message.getInAppExpiryTimestamp());
        inAppWebViewMessage.setInAppOpenTitle(message.getInAppOpenTitle());
        inAppWebViewMessage.setInAppStyle(message.getInAppStyle());

        inAppWebViewMessage.setInternalData(message.getInternalData());

        inAppWebViewMessage.setSeenTimestamp(message.getSeenTimestamp());
        inAppWebViewMessage.setSentTimestamp(message.getSentTimestamp());
        inAppWebViewMessage.setSilent(message.isSilent());
        inAppWebViewMessage.setSound(message.getSound());
        inAppWebViewMessage.setStatus(message.getStatus());
        inAppWebViewMessage.setStatusMessage(message.getStatusMessage());
        inAppWebViewMessage.setTitle(message.getTitle());
        inAppWebViewMessage.setVibrate(message.isVibrate());
        inAppWebViewMessage.setWebViewUrl(message.getWebViewUrl());
        return inAppWebViewMessage;
    }
}

