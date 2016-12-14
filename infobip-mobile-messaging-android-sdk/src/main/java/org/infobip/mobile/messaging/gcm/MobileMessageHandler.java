package org.infobip.mobile.messaging.gcm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.Random;

/**
 * @author mstipanov
 * @since 14.04.2016.
 */
public class MobileMessageHandler {

    private SharedPreferencesMessageStore messageStore;

    public void handleMessage(Context context, Intent intent) {
        if (!MobileMessagingCore.getInstance(context).isPushRegistrationEnabled()) {
            return;
        }

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
            NotificationHandler.displayNotification(context, message, getDefaultNotificationId(context));
        }

        Intent messageReceived = new Intent(Event.MESSAGE_RECEIVED.getKey());
        messageReceived.putExtras(message.getBundle());
        context.sendBroadcast(messageReceived);
        LocalBroadcastManager.getInstance(context).sendBroadcast(messageReceived);
    }

    private int getDefaultNotificationId(Context context) {
        boolean areMultipleNotificationsEnabled = MobileMessagingCore.getInstance(context).getNotificationSettings().areMultipleNotificationsEnabled();
        int defaultNotificationId = 0;
        return areMultipleNotificationsEnabled ? new Random().nextInt() : defaultNotificationId;
    }

    private void saveMessage(Context context, Message message) {
        if (!MobileMessagingCore.getInstance(context).isMessageStoreEnabled()) {
            Log.d(MobileMessaging.TAG, "Skipping save message: " + message.getMessageId());

            if (message.getGeo() != null) {
                // if message store is not enabled, we need to use it internally (by creating new instance of SharedPreferencesMessageStore.class),
                // to save only those Messages which contains Geo, otherwise they would never be triggered.
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
}
