package org.infobip.mobile.messaging.plugins;

import android.os.Bundle;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Message data mapper for JSON conversion in plugins
 */
public class MessageJson {

    public static JSONObject bundleToJSON(Bundle bundle) {
        Message message = Message.createFrom(bundle);
        if (message == null) {
            return null;
        }

        return toJSON(message);
    }

    public static JSONObject toJSON(Message message) {
        try {
            return new JSONObject()
                    .putOpt("messageId", message.getMessageId())
                    .putOpt("title", message.getTitle())
                    .putOpt("body", message.getBody())
                    .putOpt("sound", message.getSound())
                    .putOpt("vibrate", message.isVibrate())
                    .putOpt("icon", message.getIcon())
                    .putOpt("silent", message.isSilent())
                    .putOpt("category", message.getCategory())
                    .putOpt("from", message.getFrom())
                    .putOpt("receivedTimestamp", message.getReceivedTimestamp())
                    .putOpt("customPayload", message.getCustomPayload())
                    .putOpt("contentUrl", message.getContentUrl())
                    .putOpt("seen", message.getSeenTimestamp() != 0)
                    .putOpt("seenDate", message.getSeenTimestamp())
                    .putOpt("chat", message.isChatMessage())
                    .putOpt("browserUrl", message.getBrowserUrl())
                    .putOpt("webViewUrl", message.getWebViewUrl())
                    .putOpt("deeplink", message.getDeeplink())
                    .putOpt("inAppOpenTitle", message.getInAppOpenTitle())
                    .putOpt("inAppDismissTitle", message.getInAppDismissTitle());
        } catch (JSONException e) {
            MobileMessagingLogger.w("Cannot convert message to JSON: ", e);
            return null;
        }
    }

    public static JSONArray toJSONArray(@NonNull Message[] messages) {
        JSONArray array = new JSONArray();
        for (Message message : messages) {
            JSONObject json = toJSON(message);
            if (json == null) {
                continue;
            }
            array.put(json);
        }
        return array;
    }

    private static Message fromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        Message message = new Message();
        message.setMessageId(json.optString("messageId", null));
        message.setTitle(json.optString("title", null));
        message.setBody(json.optString("body", null));
        message.setSound(json.optString("sound", null));
        message.setVibrate(json.optBoolean("vibrate", true));
        message.setIcon(json.optString("icon", null));
        message.setSilent(json.optBoolean("silent", false));
        message.setCategory(json.optString("category", null));
        message.setFrom(json.optString("from", null));
        message.setReceivedTimestamp(json.optLong("receivedTimestamp", 0));
        message.setCustomPayload(json.optJSONObject("customPayload"));
        message.setContentUrl(json.optString("contentUrl", null));
        message.setSeenTimestamp(json.optLong("seenDate", 0));
        message.setBrowserUrl(json.optString("browserUrl", null));
        message.setWebViewUrl(json.optString("webViewUrl", null));
        message.setDeeplink(json.optString("deeplink", null));
        message.setInAppOpenTitle(json.optString("inAppOpenTitle", null));
        message.setInAppDismissTitle(json.optString("inAppDismissTitle", null));
        if (json.optBoolean("chat", false)) {
            message.setMessageType(Message.MESSAGE_TYPE_CHAT);
        }
        return message;
    }

    @NonNull
    public static List<Message> resolveMessages(JSONArray args) throws JSONException {
        if (args == null || args.length() < 1 || args.getString(0) == null) {
            throw new IllegalArgumentException("Cannot resolve messages from arguments");
        }

        List<Message> messages = new ArrayList<>(args.length());
        for (int i = 0; i < args.length(); i++) {
            Message m = fromJSON(args.optJSONObject(i));
            if (m == null) {
                continue;
            }

            messages.add(m);
        }
        return messages;
    }
}
