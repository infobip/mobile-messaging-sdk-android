package org.infobip.mobile.messaging.gcm;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.GcmListenerService;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageStore;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 21.03.2016.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class MobileMessagingGcmListenerService extends GcmListenerService {

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Message message = createMessage(from, data);
        sendDeliveryReport(message);

        if (!message.isSilent()) {
            saveMessage(message);
            displayNotification(message);
        }

        Intent messageReceived = new Intent(Event.MESSAGE_RECEIVED.getKey());
        messageReceived.putExtras(message.getBundle());
        LocalBroadcastManager.getInstance(this).sendBroadcast(messageReceived);
    }

    private void saveMessage(Message message) {
        if (!MobileMessaging.getInstance().isMessageStoreEnabled()) {
            return;
        }

        MessageStore.INSTANCE.save(message);
    }

    private Message createMessage(String from, Bundle data) {
        Message message = new Message(new Bundle());
        message.setFrom(from);
        message.setData(data);

        Bundle notification = data.getBundle("notification");
        if (null != notification) {
            message.copyFrom(notification);
        }

        if (StringUtils.isBlank(message.getMessageId())) {
            //TODO Remove this ASAP!
            String metaJson = data.getString("meta");
            if (null != metaJson) {
                try {
                    message.setMessageId(new JSONObject(metaJson).getString("messageId"));
                } catch (JSONException e) {
                    //ignore
                }
            }
        }
        return message;
    }

    private void sendDeliveryReport(Message message) {
        if (StringUtils.isBlank(message.getMessageId())) {
            Log.e(TAG, "No ID received for message: " + message);
            return;
        }
        MobileMessaging.getInstance().addUnreportedMessageIds(message.getMessageId());
        MobileMessaging.getInstance().reportUnreportedMessageIds();
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message message received.
     */
    private void displayNotification(Message message) {
        MobileMessaging mobileMessaging = MobileMessaging.getInstance();
        if (mobileMessaging.isDisplayNotificationEnabled() ||
                StringUtils.isNotBlank(message.getBody()) ||
                null != mobileMessaging.getCallbackActivity()) {
            return;
        }

        Intent intent = new Intent(this, mobileMessaging.getCallbackActivity());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int requestCode = 0; //TODO Request code
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); //TODO load message.sound from /res/raw/

        String title = StringUtils.isNotBlank(message.getTitle()) ? message.getTitle() : mobileMessaging.getDefaultTitle();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(message.getBody())
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        int icon;
        if (StringUtils.isNotBlank(message.getIcon())) {
            icon = ResourceLoader.loadResourceByName(this, "drawable", message.getIcon());
        } else {
            icon = mobileMessaging.getDefaultIcon();
        }
        notificationBuilder.setSmallIcon(icon);

        if (null != mobileMessaging.getVibrate()) {
            notificationBuilder.setVibrate(mobileMessaging.getVibrate());
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int id = 0; //TODO ID of notification
        notificationManager.notify(id, notificationBuilder.build());
    }
}
