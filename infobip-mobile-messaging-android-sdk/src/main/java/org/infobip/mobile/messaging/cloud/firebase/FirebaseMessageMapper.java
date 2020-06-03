package org.infobip.mobile.messaging.cloud.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.firebase.messaging.RemoteMessage;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.dal.json.JSONObjectAdapter;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.Time;
import org.json.JSONObject;

/**
 * @author sslavin
 * @since 03/09/2018.
 */
public class FirebaseMessageMapper {

    private static final String TAG = FirebaseMessageMapper.class.getSimpleName();
    private static final String IB_DATA_KEY = "org_ib_d";
    private static final JsonSerializer serializer = new JsonSerializer(false, new JSONObjectAdapter());

    private static class IBData {
        String messageId;
        String text;
        NotificationSettings notification;
        JSONObject custom;
        JSONObject internal;
    }

    private static class NotificationSettings {
        String icon;
        String title;
        String sound;
        String category;
        Boolean vibrate;
        Boolean silent;
        String contentUrl;
        Message.InAppStyle inAppStyle;
    }

    public Message createMessage(RemoteMessage remoteMessage) {
        IBData data = getIBData(remoteMessage);
        if (data == null) {
            MobileMessagingLogger.e(TAG, "Cannot retrieve message data for message ID " + remoteMessage.getMessageId());
            return null;
        }

        if (TextUtils.isEmpty(data.messageId)) {
            MobileMessagingLogger.e(TAG, "Message ID is empty for " + remoteMessage);
            return null;
        }

        if (TextUtils.isEmpty(data.text)) {
            MobileMessagingLogger.e(TAG, "Message text is empty for " + remoteMessage);
            return null;
        }

        NotificationSettings notificationSettings = data.notification != null ? data.notification : new NotificationSettings();
        long sentDateTime = data.internal != null ? data.internal.optLong("sendDateTime", Time.now()) : Time.now();
        long inAppExpiryDateTime = data.internal != null ? data.internal.optLong("inAppExpiryDateTime", 0) : 0;
        String webViewUrl = data.internal != null ? data.internal.optString("webViewUrl") : null;
        String messageType = data.internal != null ? data.internal.optString("messageType") : null;
        boolean inApp = data.internal != null && data.internal.optBoolean("inApp"); // deprecated
        Message.InAppStyle inAppStyle = null;
        if (data.notification.inAppStyle != null) {
            inAppStyle = data.notification.inAppStyle;
        } else if (inApp) {
            inAppStyle = Message.InAppStyle.MODAL;
        }
        return new Message(
                data.messageId,
                notificationSettings.title,
                data.text,
                notificationSettings.sound,
                orDefault(notificationSettings.vibrate, true),
                notificationSettings.icon,
                orDefault(notificationSettings.silent, false),
                notificationSettings.category,
                "",
                Time.now(),
                0,
                sentDateTime,
                data.custom,
                data.internal != null ? data.internal.toString() : null,
                "",
                Message.Status.UNKNOWN,
                "",
                notificationSettings.contentUrl,
                inAppStyle,
                inAppExpiryDateTime,
                webViewUrl,
                messageType);
    }

    private static IBData getIBData(RemoteMessage remoteMessage) {
        String json;
        if (remoteMessage == null || remoteMessage.getData() == null || (json = remoteMessage.getData().get(IB_DATA_KEY)) == null) {
            return null;
        }

        return serializer.deserialize(json, IBData.class);
    }

    private static <T> T orDefault(@Nullable T value, @NonNull T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
