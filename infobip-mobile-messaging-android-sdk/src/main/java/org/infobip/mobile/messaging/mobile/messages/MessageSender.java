package org.infobip.mobile.messaging.mobile.messages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.messages.MoMessageDelivery;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 21/07/16.
 */
public class MessageSender {

    private static class MessageDelivery extends Message {

        static void setStatus(Message message, Status status, String statusMessage) {
            if (message.getBundle() == null) {
                return;
            }

            message.getBundle().putString(BundleField.STATUS.getKey(), status.getKey());
            message.getBundle().putString(BundleField.STATUS_MESSAGE.getKey(), statusMessage);
        }

        public static Message fromJson(String json) {
            JSONObject object;
            try {
                object = new JSONObject(json);
            } catch (JSONException e) {
                MobileMessagingLogger.w("Cannot parse message from JSON: " + e.getMessage());
                MobileMessagingLogger.d(Log.getStackTraceString(e));
                return new Message();
            }

            Bundle bundle = new Message().getBundle();
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

    public void send(final Context context, final MobileMessagingStats stats, Executor executor, final MobileMessaging.ResultListener<Message[]> listener, final Message... messages) {
        new SendMessageTask(context) {
            @Override
            protected void onPostExecute(SendMessageResult sendMessageResult) {
                if (sendMessageResult.hasError()) {
                    MobileMessagingLogger.e("MobileMessaging API returned error (sending message)!");
                    stats.reportError(MobileMessagingStatsError.MESSAGE_SEND_ERROR);

                    Intent sendMessageError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
                    sendMessageError.putExtra(BroadcastParameter.EXTRA_EXCEPTION, MobileMessagingError.createFrom(sendMessageResult.getError()));
                    context.sendBroadcast(sendMessageError);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(sendMessageError);

                    reportFailedMessages(context, messages, sendMessageResult.getError(), listener);

                    return;
                }

                reportMessageDelivery(context, sendMessageResult.getMessageDeliveries(), listener);
            }

        }.executeOnExecutor(executor, messages);
    }

    private void reportMessageDelivery(Context context, MoMessageDelivery messageDeliveries[], MobileMessaging.ResultListener<Message[]> listener) {

        ArrayList<Bundle> messageBundles = new ArrayList<>();
        JsonSerializer serializer = new JsonSerializer();
        for (MoMessageDelivery delivery : messageDeliveries) {
            Message message = MessageDelivery.fromJson(serializer.serialize(delivery));
            messageBundles.add(message.getBundle());
        }

        reportMessages(context, listener, messageBundles);
    }

    private void reportFailedMessages(final Context context, final Message messages[], Throwable error, MobileMessaging.ResultListener<Message[]> listener) {

        ArrayList<Bundle> messageBundles = new ArrayList<>();
        for (Message message : messages) {
            MessageDelivery.setStatus(message, Message.Status.ERROR, error.getMessage());
            messageBundles.add(message.getBundle());
        }

        reportMessages(context, listener, messageBundles);
    }

    private void reportMessages(Context context, MobileMessaging.ResultListener<Message[]> listener, ArrayList<Bundle> messageBundles) {
        List<Message> messageList = Message.createFrom(messageBundles);
        MessageStore messageStore = MobileMessagingCore.getInstance(context).getMessageStore();
        if (messageStore != null) {
            messageStore.save(context, messageList.toArray(new Message[messageList.size()]));
        }

        Intent messagesSent = new Intent(Event.MESSAGES_SENT.getKey());
        messagesSent.putParcelableArrayListExtra(BroadcastParameter.EXTRA_MESSAGES, messageBundles);
        context.sendBroadcast(messagesSent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(messagesSent);

        if (listener != null) {
            listener.onResult(messageList.toArray(new Message[messageList.size()]));
        }
    }
}