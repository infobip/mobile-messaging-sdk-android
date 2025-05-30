package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OpenLivechatAction {

    static final String ACTION_KEY = "openLiveChat";
    private static final String KEYWORD = "keyword";

    @Nullable
    public String keyword;

    public static OpenLivechatAction parseFrom(Message message) {
        if (message == null || message.getInternalData() == null) {
            return null;
        }
        OpenLivechatAction action = null;
        try {
            JSONObject internalData = new JSONObject(message.getInternalData());
            if (internalData.has(ACTION_KEY)) {
                action = new OpenLivechatAction();
                JSONObject openLiveChat = internalData.optJSONObject(ACTION_KEY);
                action.keyword = openLiveChat != null ? openLiveChat.optString(KEYWORD) : null;
            }
        } catch (JSONException e) {
            MobileMessagingLogger.e("OpenLivechatAction", "Failed to parse internal data.", e);
        }

        return action;
    }

    @NonNull
    @Override
    public String toString() {
        return "OpenLivechatAction{" +
                "keyword='" + keyword + '\'' +
                '}';
    }
}
