package org.infobip.mobile.messaging.mobile.messages;

import android.content.Context;
import android.util.Log;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.messages.MoMessageDelivery;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 21/07/16.
 */
public class MessageSender {

    private static final String TAG = MessageSender.class.getSimpleName();

    private final Broadcaster broadcaster;

    public MessageSender(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void send(final Context context, final MobileMessagingStats stats, Executor executor, final MobileMessaging.ResultListener<Message[]> listener, final Message... messages) {
        new SendMessageTask(context) {
            @Override
            protected void onPostExecute(SendMessageResult sendMessageResult) {
                if (sendMessageResult.hasError()) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (sending message)!");
                    stats.reportError(MobileMessagingStatsError.MESSAGE_SEND_ERROR);
                    broadcaster.error(MobileMessagingError.createFrom(sendMessageResult.getError()));

                    reportFailedMessages(context, messages, sendMessageResult.getError(), listener);

                    return;
                }

                reportMessageDelivery(context, sendMessageResult.getMessageDeliveries(), listener);
            }

        }.executeOnExecutor(executor, messages);
    }

    private void reportMessageDelivery(Context context, MoMessageDelivery messageDeliveries[], MobileMessaging.ResultListener<Message[]> listener) {

        List<Message> messages = new ArrayList<>(messageDeliveries.length);
        for (MoMessageDelivery delivery : messageDeliveries) {
            Message message = new Message();
            message.setMessageId(delivery.getMessageId());
            message.setDestination(delivery.getDestination());
            message.setBody(delivery.getText());
            message.setStatusMessage(delivery.getStatus());
            message.setReceivedTimestamp(System.currentTimeMillis());
            message.setSeenTimestamp(System.currentTimeMillis());
            messages.add(message);

            String json = delivery.getCustomPayload() != null ? delivery.getCustomPayload().toString() : null;
            try {
                message.setCustomPayload(json != null ? new JSONObject(json) : null);
            } catch (JSONException e) {
                MobileMessagingLogger.w(TAG, Log.getStackTraceString(e));
            }

            Message.Status status = Message.Status.UNKNOWN;
            int statusCode = delivery.getStatusCode();
            if (statusCode < Message.Status.values().length) {
                status = Message.Status.values()[statusCode];
            } else {
                MobileMessagingLogger.e(TAG, "Unexpected status code: " + statusCode);
            }
            message.setStatus(status);
        }

        reportMessages(context, listener, messages);
    }

    private void reportFailedMessages(final Context context, final Message messages[], Throwable error, MobileMessaging.ResultListener<Message[]> listener) {

        for (Message message : messages) {
            message.setStatus(Message.Status.ERROR);
            message.setStatusMessage(error.getMessage());
        }

        reportMessages(context, listener, Arrays.asList(messages));
    }

    private void reportMessages(Context context, MobileMessaging.ResultListener<Message[]> listener, List<Message> messages) {
        MessageStore messageStore = MobileMessagingCore.getInstance(context).getMessageStore();
        if (messageStore != null) {
            messageStore.save(context, messages.toArray(new Message[messages.size()]));
        }

        broadcaster.messagesSent(messages);

        if (listener != null) {
            listener.onResult(messages.toArray(new Message[messages.size()]));
        }
    }
}