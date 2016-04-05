package org.infobip.mobile.messaging.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.GcmReceiver;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 21.03.2016.
 */
public class MobileMessagingGcmReceiver extends GcmReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (!"com.google.android.c2dm.intent.RECEIVE".equals(intent.getAction())) {
            return;
        }

        String from = intent.getStringExtra("from");
        Bundle data = intent.getExtras();

        data.putLong("received_timestamp", System.currentTimeMillis());

        Log.d(MobileMessaging.TAG, "Message received from: " + from);
        Message message = createMessage(from, data);
        sendDeliveryReport(context, message);

        Log.d(MobileMessaging.TAG, "Message is silent: " + message.isSilent());
        if (!message.isSilent()) {
            saveMessage(context, message);
            displayNotification(context, message);
        }

        Intent messageReceived = new Intent(Event.MESSAGE_RECEIVED.getKey());
        messageReceived.putExtras(message.getBundle());
        context.sendBroadcast(messageReceived);
        LocalBroadcastManager.getInstance(context).sendBroadcast(messageReceived);
    }

    private void saveMessage(Context context, Message message) {
        if (!MobileMessaging.getInstance(context).isMessageStoreEnabled()) {
            Log.d(MobileMessaging.TAG, "Skipping save message: " + message.getMessageId());
            return;
        }

        Log.d(MobileMessaging.TAG, "Saving message: " + message.getMessageId());
        try {
            MobileMessaging.getInstance(context).getMessageStore().save(context, message);
        } catch (Exception e) {
            Log.e(MobileMessaging.TAG, "Error saving message: " + message.getMessageId(), e);
        }
    }

    private Message createMessage(String from, Bundle data) {
        Message message = Message.copyFrom(data);
        message.setFrom(from);
        message.setData(data);
        return message;
    }

    private void sendDeliveryReport(Context context, Message message) {
        if (StringUtils.isBlank(message.getMessageId())) {
            Log.e(TAG, "No ID received for message: " + message);
            return;
        }
        Log.d(MobileMessaging.TAG, "Sending DR: " + message.getMessageId());
        MobileMessaging.getInstance(context).addUnreportedMessageIds(message.getMessageId());
        MobileMessaging.getInstance(context).reportUnreportedMessageIds();
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message message received.
     */
    private void displayNotification(Context context, Message message) {
        MobileMessaging mobileMessaging = MobileMessaging.getInstance(context);
        if (!mobileMessaging.isDisplayNotificationEnabled() ||
                StringUtils.isBlank(message.getBody()) ||
                null == mobileMessaging.getCallbackActivity()) {
            return;
        }

        Intent intent = new Intent(context, mobileMessaging.getCallbackActivity());
        intent.putExtra("message", message.getBundle());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

//        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); //TODO load message.sound from /res/raw/

        String title = StringUtils.isNotBlank(message.getTitle()) ? message.getTitle() : mobileMessaging.getDefaultTitle();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setContentTitle(title)
                .setContentText(message.getBody())
                .setAutoCancel(true)
//                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setWhen(message.getReceivedTimestamp());

        int icon;
        if (StringUtils.isNotBlank(message.getIcon())) {
            icon = ResourceLoader.loadResourceByName(context, "drawable", message.getIcon());
        } else {
            icon = mobileMessaging.getDefaultIcon();
        }
        notificationBuilder.setSmallIcon(icon);

        if (null != mobileMessaging.getVibrate()) {
            notificationBuilder.setVibrate(mobileMessaging.getVibrate());
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = notificationBuilder.build();
        notificationManager.notify(0, notification);
    }
}
