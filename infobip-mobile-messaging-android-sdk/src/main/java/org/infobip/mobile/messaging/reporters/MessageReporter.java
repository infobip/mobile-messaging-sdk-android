package org.infobip.mobile.messaging.reporters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.api.messages.MoMessageDelivery;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.stats.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tasks.SendMessageResult;
import org.infobip.mobile.messaging.tasks.SendMessageTask;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Executor;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author sslavin
 * @since 21/07/16.
 */
public class MessageReporter {

    static class MessageDelivery extends Message {

        public static Message fromJson(String json) {
            JSONObject object;
            try {
                object = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
                return new Message();
            }

            Bundle bundle = new Bundle();
            bundle.putString(BundleField.DESTINATION.getKey(), object.optString(JsonField.DESTINATION.getKey()));
            bundle.putString(BundleField.MESSAGE_ID.getKey(), object.optString(JsonField.MESSAGE_ID.getKey()));
            bundle.putString(BundleField.BODY.getKey(), object.optString(JsonField.TEXT.getKey()));
            if (object.has(JsonField.CUSTOM_PAYLOAD.getKey())) {
                bundle.putString(BundleField.CUSTOM_PAYLOAD.getKey(), object.optJSONObject(JsonField.CUSTOM_PAYLOAD.getKey()).toString());
            }
            if (object.has(JsonField.STATUS_CODE.getKey())) {
                int statusId = object.optInt(JsonField.STATUS_CODE.getKey(), Status.UNKNOWN.ordinal());
                for (Status status : Status.values()) {
                    if (status.ordinal() == statusId) {
                        bundle.putString(BundleField.STATUS.getKey(), status.getKey());
                        break;
                    }
                }
            }
            bundle.putString(BundleField.STATUS_MESSAGE.getKey(), object.optString(JsonField.STATUS.getKey()));

            return Message.createFrom(bundle);
        }

        enum JsonField {
            MESSAGE_ID("messageId"),
            DESTINATION("destination"),
            TEXT("text"),
            CUSTOM_PAYLOAD("customPayload"),
            STATUS_CODE("statusCode"),
            STATUS("status");

            private final String key;

            JsonField(String key) {
                this.key = key;
            }

            public String getKey() {
                return key;
            }
        }
    }

    public void send(final Context context, final MobileMessagingStats stats, Executor executor, final Message... messages) {
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

                ArrayList<Bundle> messageBundles = new ArrayList<>();
                JsonSerializer serializer = new JsonSerializer();
                for (MoMessageDelivery delivery : sendMessageResult.getMessageDeliveries()) {
                    Message message = MessageDelivery.fromJson(serializer.serialize(delivery));
                    messageBundles.add(message.getBundle());
                }

                Intent messagesSent = new Intent(Event.MESSAGES_SENT.getKey());
                messagesSent.putParcelableArrayListExtra(BroadcastParameter.EXTRA_MESSAGES, messageBundles);
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

    private void reportFailedMessages(final Context context, String errorMessage, final Message... messages) {

        ArrayList<Bundle> messageBundles = new ArrayList<>();
        for (Message message : messages) {
            messageBundles.add(message.getBundle());
        }

        Intent messagesSent = new Intent(Event.MESSAGES_SENT.getKey());
        messagesSent.putParcelableArrayListExtra(BroadcastParameter.EXTRA_MESSAGES, messageBundles);
        context.sendBroadcast(messagesSent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(messagesSent);
    }
}