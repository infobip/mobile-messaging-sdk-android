package org.infobip.mobile.messaging.gcm;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.storage.MessageStoreWrapper;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author mstipanov
 * @since 14.04.2016.
 */
public class MobileMessageHandler {

    private final Broadcaster broadcaster;
    private final NotificationHandler notificationHandler;
    private final MessageStoreWrapper messageStoreWrapper;
    private final MobileMessagingCore mobileMessagingCore;

    public MobileMessageHandler(MobileMessagingCore mobileMessagingCore, Broadcaster broadcaster, NotificationHandler notificationHandler, MessageStoreWrapper messageStoreWrapper) {
        this.broadcaster = broadcaster;
        this.notificationHandler = notificationHandler;
        this.messageStoreWrapper = messageStoreWrapper;
        this.mobileMessagingCore = mobileMessagingCore;
    }

    /**
     * Handles new push message
     *
     * @param message new message
     */
    public void handleMessage(Message message) {
        if (!mobileMessagingCore.isPushRegistrationEnabled()) {
            return;
        }

        if (StringUtils.isBlank(message.getMessageId())) {
            MobileMessagingLogger.w("Ignoring message without messageId");
            return;
        }

        message.setReceivedTimestamp(Time.now());
        sendDeliveryReport(message);

        for (MessageHandlerModule handler : mobileMessagingCore.getMessageHandlerModules()) {
            MobileMessagingLogger.d("Dispatching message to " + handler.getClass().getName());
            if (handler.handleMessage(message)) {
                return;
            }
        }

        saveMessage(message);
        broadcaster.messageReceived(message);

        MobileMessagingLogger.d("Message is silent: " + message.isSilent());
        if (!message.isSilent()) {
            notificationHandler.displayNotification(message);
        }
    }

    private void saveMessage(Message message) {
        MobileMessagingLogger.d("Saving message: " + message.getMessageId());
        try {
            messageStoreWrapper.upsert(message);
        } catch (Exception e) {
            MobileMessagingLogger.e(InternalSdkError.ERROR_SAVING_MESSAGE.get(), e);
        }
    }

    private void sendDeliveryReport(Message message) {
        if (StringUtils.isBlank(message.getMessageId())) {
            MobileMessagingLogger.e("No ID received for message: " + message);
            return;
        }
        MobileMessagingLogger.d("Sending DR: " + message.getMessageId());
        mobileMessagingCore.setMessagesDelivered(message.getMessageId());
    }
}