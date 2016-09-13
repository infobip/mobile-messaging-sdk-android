package org.infobip.mobile.messaging.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author mstipanov
 * @since 14.04.2016.
 */
class MobileMessageHandler {

    public static final int DEFAULT_NOTIFICATION_ID = 0;

    private SharedPreferencesMessageStore messageStore;

    void handleMessage(Context context, Intent intent) {
        String from = intent.getStringExtra("from");
        Bundle data = intent.getExtras();

        data.putLong("received_timestamp", System.currentTimeMillis());

        Message message = createMessage(from, data);
        if (StringUtils.isBlank(message.getMessageId())) {
            Log.w(MobileMessaging.TAG, "Ignoring message without messageId");
            return;
        }

        Log.d(MobileMessaging.TAG, "Message received from: " + from);

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

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message message received.
     */
    private void displayNotification(Context context, Message message) {
        NotificationCompat.Builder builder = notificationCompatBuilder(context, message);
        if (builder == null) return;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = builder.build();
        notificationManager.notify(DEFAULT_NOTIFICATION_ID, notification);
    }

    private void saveMessage(Context context, Message message) {
        if (!MobileMessagingCore.getInstance(context).isMessageStoreEnabled()) {
            Log.d(MobileMessaging.TAG, "Skipping save message: " + message.getMessageId());

            if (message.getGeofenceAreasList() != null && !message.getGeofenceAreasList().isEmpty()) {
                // if message store is not enabled, we need to use it internally (by creating new instance of SharedPreferencesMessageStore.class),
                // to save only those Messages which contains GeofenceAreas, otherwise they would never be triggered.
                messageStore().save(context, message);
                Log.d(MobileMessaging.TAG, "Only save message that contains geofence areas: " + message.getMessageId());
            }
            return;
        }

        Log.d(MobileMessaging.TAG, "Saving message: " + message.getMessageId());
        try {
            MobileMessagingCore.getInstance(context).getMessageStore().save(context, message);
        } catch (Exception e) {
            Log.e(MobileMessaging.TAG, "Error saving message: " + message.getMessageId(), e);
        }
    }

    private MessageStore messageStore() {
        if (messageStore == null) {
            messageStore = new SharedPreferencesMessageStore();
        }
        return messageStore;
    }

    private Message createMessage(String from, Bundle data) {
        Message message = Message.createFrom(data);
        message.setFrom(from);
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

    private NotificationCompat.Builder notificationCompatBuilder(Context context, Message message) {
        NotificationSettings notificationSettings = notificationSettings(context, message);
        if (notificationSettings == null) return null;

        Intent intent = new Intent(context, notificationSettings.getCallbackActivity());
        intent.putExtra(MobileMessagingProperty.EXTRA_MESSAGE.getKey(), message.getBundle());
        intent.addFlags(notificationSettings.getIntentFlags());
        @SuppressWarnings("ResourceType") PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, notificationSettings.getPendingIntentFlags());

        String title = StringUtils.isNotBlank(message.getTitle()) ? message.getTitle() : notificationSettings.getDefaultTitle();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message.getBody())
                .setAutoCancel(notificationSettings.isNotificationAutoCancel())
                .setContentIntent(pendingIntent)
                .setWhen(message.getReceivedTimestamp());

        setNotificationDefaults(context, notificationBuilder, message);
        setNotificationSound(context, notificationBuilder, message);
        setNotificationIcon(context, notificationBuilder, message);

        return notificationBuilder;
    }

    private void setNotificationDefaults(Context context, NotificationCompat.Builder notificationBuilder, Message message) {
        NotificationSettings notificationSettings = notificationSettings(context, message);
        if (notificationSettings == null) return;

        int notificationDefaults = notificationSettings.getNotificationDefaults();

        if (!message.isDefaultSound()) {
            notificationDefaults &= ~Notification.DEFAULT_SOUND;
        }

        if (!message.isVibrate()) {
            notificationDefaults &= ~Notification.DEFAULT_VIBRATE;
        } else if (message.hasVibrateSetting() && (notificationDefaults & Notification.DEFAULT_VIBRATE) == 0) {
            notificationDefaults |= Notification.DEFAULT_VIBRATE;
        }

        notificationBuilder.setDefaults(notificationDefaults);
    }

    private void setNotificationIcon(Context context, NotificationCompat.Builder notificationBuilder, Message message) {
        NotificationSettings notificationSettings = notificationSettings(context, message);
        if (notificationSettings == null) return;

        int icon;
        if (StringUtils.isNotBlank(message.getIcon())) {
            icon = ResourceLoader.loadResourceByName(context, "drawable", message.getIcon());
        } else {
            icon = notificationSettings.getDefaultIcon();
        }
        notificationBuilder.setSmallIcon(icon);
    }

    private void setNotificationSound(Context context, NotificationCompat.Builder notificationBuilder, Message message) {
        Uri soundUri = null;
        String sound = message.getSound();
        if (!message.isDefaultSound() && StringUtils.isNotBlank(sound)) {
            soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/raw/" + sound);
            if (soundUri == null) {
                Log.w(MobileMessaging.TAG, "Cannot load notification sound from message: " + sound);
            }
        }

        if (soundUri == null) {
            NotificationSettings notificationSettings = notificationSettings(context, message);
            String stringUri = notificationSettings != null ? notificationSettings.getDefaultNotificationSound() : null;
            soundUri = stringUri != null ? Uri.parse(stringUri) : null;
        }

        if (soundUri != null) {
            notificationBuilder.setSound(soundUri);
        }
    }

    private NotificationSettings notificationSettings(Context context, Message message) {
        NotificationSettings notificationSettings = MobileMessagingCore.getInstance(context).getNotificationSettings();
        if (null == notificationSettings) {
            return null;
        }

        if (!notificationSettings.isDisplayNotificationEnabled() ||
                null == notificationSettings.getCallbackActivity()) {
            return null;
        }

        if (StringUtils.isBlank(message.getBody())) {
            return null;
        }

        if (ActivityLifecycleMonitor.isForeground() && notificationSettings.isForegroundNotificationDisabled()) {
            return null;
        }

        return notificationSettings;
    }
}
