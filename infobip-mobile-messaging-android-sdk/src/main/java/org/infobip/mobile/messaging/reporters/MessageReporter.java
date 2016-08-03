package org.infobip.mobile.messaging.reporters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MoMessage;
import org.infobip.mobile.messaging.api.messages.MoMessageDelivery;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tasks.SendMessageResult;
import org.infobip.mobile.messaging.tasks.SendMessageTask;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author sslavin
 * @since 21/07/16.
 */
public class MessageReporter {

    class MoDeliveredMessage extends MoMessage {

        private final static int STATUS_ID_SENT = 0;
        private final static int STATUS_ID_ERROR = 1;

        public MoDeliveredMessage(String destination, String text, Map<String, Object> customPayload, String messageId, int statusId, String statusMessage) {
            super(destination, text, customPayload);
            this.messageId = messageId;
            this.status = getStatusFromStatusId(statusId);
            this.statusMessage = statusMessage;
        }

        Status getStatusFromStatusId(int statusId) {
            switch (statusId) {
                case STATUS_ID_SENT:
                    return Status.SUCCESS;
                case STATUS_ID_ERROR:
                    return Status.ERROR;
                default:
                    return Status.UNKNOWN;
            }
        }
    }

    public void send(final Context context, final MobileMessagingStats stats, Executor executor, final MoMessage... messages) {
        new SendMessageTask(context) {
            @Override
            protected void onPostExecute(SendMessageResult sendMessageResult) {
                if (sendMessageResult.hasError()) {
                    Log.e(TAG, "MobileMessaging API returned error!");
                    stats.reportError(MobileMessagingError.MESSAGE_SEND_ERROR);

                    Intent sendMessageError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    sendMessageError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, sendMessageResult.getError());
                    context.sendBroadcast(sendMessageError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(sendMessageError);

                    reportFailedMessages(context, sendMessageResult.getError().getMessage(), messages);

                    return;
                }

                ArrayList<String> moMessages = new ArrayList<>();
                JsonSerializer jsonSerializer = new JsonSerializer();
                for (MoMessageDelivery delivery : sendMessageResult.getMessageDeliveries()) {

                    MoDeliveredMessage message = new MoDeliveredMessage(
                            delivery.getDestination(),
                            delivery.getText(),
                            delivery.getCustomPayload(),
                            delivery.getMessageId(),
                            delivery.getStatusCode(),
                            delivery.getStatus());

                    moMessages.add(jsonSerializer.serialize(message));
                }

                Intent messagesSent = new Intent(Event.MESSAGES_SENT.getKey());
                messagesSent.putStringArrayListExtra(BroadcastParameter.EXTRA_MO_MESSAGES, moMessages);
                context.sendBroadcast(messagesSent);
                LocalBroadcastManager.getInstance(context).sendBroadcast(messagesSent);
            }

            @Override
            protected void onCancelled() {
                Log.e(TAG, "Error sending messages!");
                stats.reportError(MobileMessagingError.MESSAGE_SEND_ERROR);
                reportFailedMessages(context, "Network error", messages);
            }
        }.executeOnExecutor(executor, messages);
    }

    private void reportFailedMessages(final Context context, String errorMessage, final MoMessage... messages) {

        ArrayList<String> moMessages = new ArrayList<>();
        JsonSerializer jsonSerializer = new JsonSerializer();
        for (MoMessage message : messages) {
            MoDeliveredMessage moMessage = new MoDeliveredMessage(
                    message.getDestination(),
                    message.getText(),
                    message.getCustomPayload(),
                    message.getMessageId(),
                    MoDeliveredMessage.STATUS_ID_ERROR,
                    errorMessage
            );
            moMessages.add(jsonSerializer.serialize(moMessage));
        }

        Intent messagesSent = new Intent(Event.MESSAGES_SENT.getKey());
        messagesSent.putStringArrayListExtra(BroadcastParameter.EXTRA_MO_MESSAGES, moMessages);
        context.sendBroadcast(messagesSent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(messagesSent);
    }
}