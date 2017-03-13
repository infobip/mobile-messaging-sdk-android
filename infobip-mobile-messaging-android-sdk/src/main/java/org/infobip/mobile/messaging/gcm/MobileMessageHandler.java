package org.infobip.mobile.messaging.gcm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.dal.bundle.BundleMessageMapper;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author mstipanov
 * @since 14.04.2016.
 */
public class MobileMessageHandler {

    public void handleMessage(Context context, Intent intent) {
        if (!MobileMessagingCore.getInstance(context).isPushRegistrationEnabled()) {
            return;
        }

        String from = intent.getStringExtra("from");
        Bundle data = intent.getExtras();

        data.putLong("received_timestamp", System.currentTimeMillis());

        Message message = createMessage(from, data);
        if (StringUtils.isBlank(message.getMessageId())) {
            MobileMessagingLogger.w("Ignoring message without messageId");
            return;
        }

        MobileMessagingLogger.d("Message received from: " + from);

        sendDeliveryReport(context, message);
        saveMessage(context, message);

        MobileMessagingLogger.d("Message is silent: " + message.isSilent());
        if (!message.isSilent()) {
            NotificationHandler.displayNotification(context, message);
        }

        if (!MobileMessagingCore.hasGeo(message)) {
            Intent messageReceived = new Intent(Event.MESSAGE_RECEIVED.getKey());
            messageReceived.putExtras(BundleMessageMapper.toBundle(message));
            context.sendBroadcast(messageReceived);
            LocalBroadcastManager.getInstance(context).sendBroadcast(messageReceived);
        }
    }

    private void saveMessage(Context context, Message message) {
        MessageStore messageStore = MobileMessagingCore.getInstance(context).getMessageStoreForMessage(message);
        if (messageStore == null) {
            MobileMessagingLogger.d("Skipping save message: " + message.getMessageId());
            return;
        }

        MobileMessagingLogger.d("Saving message: " + message.getMessageId());
        try {
            messageStore.save(context, message);

            if (MobileMessagingCore.hasGeo(message)) {
                MobileMessagingCore.getInstance(context).setAllActiveGeoAreasMonitored(false);
                MobileMessagingCore.getInstance(context).startGeoMonitoringIfNecessary();
            }
        } catch (Exception e) {
            MobileMessagingLogger.e(InternalSdkError.ERROR_SAVING_MESSAGE.get(), e);
        }
    }

    private Message createMessage(String from, Bundle data) {
        Message message = Message.createFrom(data);
        message.setFrom(from);
        return message;
    }

    private void sendDeliveryReport(Context context, Message message) {
        if (StringUtils.isBlank(message.getMessageId())) {
            MobileMessagingLogger.e("No ID received for message: " + message);
            return;
        }
        MobileMessagingLogger.d("Sending DR: " + message.getMessageId());
        MobileMessagingCore.getInstance(context).setMessagesDelivered(message.getMessageId());
    }
}