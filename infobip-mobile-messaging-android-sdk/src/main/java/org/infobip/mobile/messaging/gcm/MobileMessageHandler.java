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
import org.infobip.mobile.messaging.*;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author mstipanov
 * @since 14.04.2016.
 */
class MobileMessageHandler {

    void handleNotification(Context context, Intent intent) {
        String from = intent.getStringExtra("from");
        Bundle data = intent.getExtras();

        data.putLong("received_timestamp", System.currentTimeMillis());

        Log.d(MobileMessaging.TAG, "Message received from: " + from);
        Message message = createMessage(from, data);
        sendDeliveryReport(context, message);
        saveMessage(context, message);

        Log.d(MobileMessaging.TAG, "Message is silent: " + message.isSilent());
        if (!message.isSilent()) {
            displayNotification(context, message);
        }

        Intent messageReceived = new Intent(Event.MESSAGE_RECEIVED.getKey());
        messageReceived.putExtras(message.getBundle());
        context.sendBroadcast(messageReceived);
        LocalBroadcastManager.getInstance(context).sendBroadcast(messageReceived);
    }

    private void saveMessage(Context context, Message message) {
        if (!MobileMessagingCore.getInstance(context).isMessageStoreEnabled()) {
            Log.d(MobileMessaging.TAG, "Skipping save message: " + message.getMessageId());
            return;
        }

        Log.d(MobileMessaging.TAG, "Saving message: " + message.getMessageId());
        try {
            MobileMessagingCore.getInstance(context).getMessageStore().save(context, message);
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
            Log.e(MobileMessaging.TAG, "No ID received for message: " + message);
            return;
        }
        Log.d(MobileMessaging.TAG, "Sending DR: " + message.getMessageId());
        MobileMessagingCore.getInstance(context).setMessagesDelivered(message.getMessageId());
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message message received.
     */
    private void displayNotification(Context context, Message message) {
        NotificationSettings notificationSettings = MobileMessagingCore.getInstance(context).getNotificationSettings();
        if (null == notificationSettings) {
            return;
        }

        if (!notificationSettings.isDisplayNotificationEnabled() ||
                null == notificationSettings.getCallbackActivity()) {
            return;
        }

        if (StringUtils.isBlank(message.getBody())) {
            return;
        }

        if (ActivityLifecycleMonitor.isForeground() && notificationSettings.isForegroundNotificationDisabled()) {
            return;
        }

        Intent intent = new Intent(context, notificationSettings.getCallbackActivity());
        intent.putExtra("message", message.getBundle());
        intent.addFlags(notificationSettings.getIntentFlags());
        @SuppressWarnings("ResourceType") PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, notificationSettings.getPendingIntentFlags());

        String title = StringUtils.isNotBlank(message.getTitle()) ? message.getTitle() : notificationSettings.getDefaultTitle();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setDefaults(notificationSettings.getNotificationDefaults())
                .setContentTitle(title)
                .setContentText(message.getBody())
                .setAutoCancel(notificationSettings.isNotificationAutoCancel())
                .setContentIntent(pendingIntent)
                .setWhen(message.getReceivedTimestamp());

        int icon;
        if (StringUtils.isNotBlank(message.getIcon())) {
            icon = ResourceLoader.loadResourceByName(context, "drawable", message.getIcon());
        } else {
            icon = notificationSettings.getDefaultIcon();
        }
        notificationBuilder.setSmallIcon(icon);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = notificationBuilder.build();
        notificationManager.notify(0, notification);
    }
}
