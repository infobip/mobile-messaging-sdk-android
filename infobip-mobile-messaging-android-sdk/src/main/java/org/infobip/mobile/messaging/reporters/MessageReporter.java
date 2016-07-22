package org.infobip.mobile.messaging.reporters;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MoMessage;
import org.infobip.mobile.messaging.api.messages.MoOutgoingMessageDelivery;
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

        private final static String STATUS_ID_SENT = "0";
        private final static String STATUS_ID_ERROR = "1";

        public MoDeliveredMessage(String destination, String text, Map<String, Object> customPayload, String messageId, String statusId) {
            super(destination, text, customPayload);
            this.messageId = messageId;
            this.status = getStatusFromStatusId(statusId);
        }

        Status getStatusFromStatusId(String statusId) {
            if (statusId == null) {
                return Status.UNKNOWN;
            }

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

    public void send(final Context context, final MobileMessagingStats stats, Executor executor, MoMessage... messages) {
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
                    return;
                }

                ArrayList<String> moMessages = new ArrayList<>();
                JsonSerializer jsonSerializer = new JsonSerializer();
                for (MoOutgoingMessageDelivery delivery : sendMessageResult.getMessageDeliveries()) {

                    MoDeliveredMessage message = new MoDeliveredMessage(
                            delivery.getDestination(),
                            delivery.getText(),
                            delivery.getCustomPayload(),
                            delivery.getMessageId(),
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
                Log.e(TAG, "Error reporting user data!");
                stats.reportError(MobileMessagingError.MESSAGE_SEND_ERROR);
            }
        }.executeOnExecutor(executor, messages);
    }
}