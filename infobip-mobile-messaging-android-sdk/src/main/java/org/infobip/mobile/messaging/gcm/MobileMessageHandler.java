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

import org.infobip.mobile.messaging.Actionable;
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

    public static final int CHAT_NOTIFICATION_ID = 1;
    public static final int COUPON_NOTIFICATION_ID = 2;
    public static final int DEFAULT_NOTIFICATION_ID = 0;
    private NotificationSettings notificationSettings;
    private SharedPreferencesMessageStore messageStore;

    void handleNotification(Context context, Intent intent) {
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
            String category = message.getCategory();

            if (Actionable.CHAT.equalsIgnoreCase(category)) {
                displayChatNotification(context, message);

            } else if (Actionable.COUPON.equalsIgnoreCase(category)) {
                displayCouponNotification(context, message);

            } else {
                displayNotification(context, message);
            }
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

    /**
     * Create and show a notification containing the received GCM message and two actions - one to mark message as seen
     * and one to reply to the received message without opening your application.
     *
     * @param message message received.
     */
    private void displayChatNotification(Context context, Message message) {
        NotificationCompat.Builder builder = notificationCompatBuilder(context, message);
        if (builder == null) return;

        Intent markSeenIntent = new Intent(context, NotificationActionReceiver.class);
        markSeenIntent.setAction(NotificationAction.ACTION_MARK_SEEN);
        markSeenIntent.putExtra(MobileMessagingProperty.EXTRA_MESSAGE.getKey(), message.getBundle());
        PendingIntent pendingIntentMarkSeen = PendingIntent.getBroadcast(context, 0, markSeenIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action markSeenAction = new NotificationCompat.Action(0, notificationSettings.getMarkSeenActionTitle(), pendingIntentMarkSeen);

        Intent replyIntent = new Intent(context, NotificationActionReceiver.class);
        replyIntent.setAction(NotificationAction.ACTION_REPLY);
        replyIntent.putExtra(MobileMessagingProperty.EXTRA_MESSAGE.getKey(), message.getBundle());
        PendingIntent pendingIntentReply = PendingIntent.getBroadcast(context, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action replyAction = new NotificationCompat.Action(0, notificationSettings.getReplyActionTitle(), pendingIntentReply);

        builder.addAction(markSeenAction);
        builder.addAction(replyAction);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = builder.build();
        notificationManager.notify(CHAT_NOTIFICATION_ID, notification);
    }

    /**
     * Create and show a notification containing the received GCM message and action that opens received web link in browser.
     *
     * @param message message received.
     */
    private void displayCouponNotification(Context context, Message message) {
        NotificationCompat.Builder builder = notificationCompatBuilder(context, message);
        if (builder == null) return;

        Actionable actionable = message.getActionable();
        if (actionable != null && actionable.getInteractive() != null) {
            Actionable.Interactive interactive = actionable.getInteractive();

            String couponUrl = interactive.getButtonActions().getCouponUrl();
            Intent openUrlIntent = new Intent(context, NotificationActionReceiver.class);
            openUrlIntent.setAction(NotificationAction.ACTION_COUPON_URL);
            openUrlIntent.putExtra(MobileMessagingProperty.EXTRA_MESSAGE.getKey(), message.getBundle());
            openUrlIntent.putExtra(Actionable.EXTRA_COUPON_URL, couponUrl);
            PendingIntent pendingIntentOpenUrl = PendingIntent.getBroadcast(context, 0, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Action openUrlAction = new NotificationCompat.Action(0, notificationSettings.getOpenUrlActionTitle(), pendingIntentOpenUrl);
            builder.addAction(openUrlAction);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = builder.build();
            notificationManager.notify(COUPON_NOTIFICATION_ID, notification);

        } else {
            Log.e(MobileMessaging.TAG, "Unable to parse internalData.interactive object", new Throwable("interactive json object may be mailformed"));
        }
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
        notificationSettings = notificationSettings(context, message);
        if (notificationSettings == null) return null;

        Intent intent = new Intent(context, notificationSettings.getCallbackActivity());
        intent.putExtra(MobileMessagingProperty.EXTRA_MESSAGE.getKey(), message.getBundle());
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

        return notificationBuilder;
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
