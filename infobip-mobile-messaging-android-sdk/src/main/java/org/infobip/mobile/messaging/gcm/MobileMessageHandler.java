package org.infobip.mobile.messaging.gcm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.dal.bundle.FCMMessageMapper;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author mstipanov
 * @since 14.04.2016.
 */
public class MobileMessageHandler {

    private Broadcaster broadcaster;

    public MobileMessageHandler(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    /**
     * Handles GCM/FCM new push message intent
     * @param intent intent that contains new message
     */
    public void handleMessage(Context context, Intent intent) {
        Bundle data = intent.getExtras();
        handleMessage(context, FCMMessageMapper.fromCloudBundle(data));
    }

    /**
     * Handles new push message
     * @param message new message
     */
    public void handleMessage(Context context, Message message) {

        if (!MobileMessagingCore.getInstance(context).isPushRegistrationEnabled()) {
            return;
        }

        if (StringUtils.isBlank(message.getMessageId())) {
            MobileMessagingLogger.w("Ignoring message without messageId");
            return;
        }

        message.setReceivedTimestamp(System.currentTimeMillis());

        sendDeliveryReport(context, message);
        saveMessage(context, message);

        MobileMessagingLogger.d("Message is silent: " + message.isSilent());
        if (!message.isSilent()) {
            NotificationHandler.displayNotification(context, message);
        }

        if (!MobileMessagingCore.hasGeo(message)) {
            broadcaster.messageReceived(message);
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
                MobileMessagingCore.getInstance(context).startGeoMonitoringIfNecessary();
            }
        } catch (Exception e) {
            MobileMessagingLogger.e(InternalSdkError.ERROR_SAVING_MESSAGE.get(), e);
        }
    }

    private void sendDeliveryReport(Context context, Message message) {
        if (StringUtils.isBlank(message.getMessageId())) {
            MobileMessagingLogger.e("No ID received for message: " + message);
            return;
        }
        MobileMessagingLogger.d("Sending DR: " + message.getMessageId());
        MobileMessagingCore.getInstance(context).setMessagesDelivered(message.getMessageId());
        broadcaster.deliveryReported(message.getMessageId());
    }
}