package org.infobip.mobile.messaging.mobileapi.messages;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.messages.MoMessagesBody;
import org.infobip.mobile.messaging.api.messages.MoMessagesResponse;
import org.infobip.mobile.messaging.api.messages.MobileApiMessages;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.storage.MessageStoreWrapper;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 21/07/16.
 */
public class MoMessageSender {

    private final long MESSAGE_MAX_RETRY_LIFETIME = TimeUnit.DAYS.toMillis(2);

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final Broadcaster broadcaster;
    private final Executor executor;
    private final MobileMessagingStats stats;
    private final MRetryPolicy retryPolicy;
    private final MRetryPolicy noRetryPolicy;
    private final MobileApiMessages mobileApiMessages;
    private final MessageStoreWrapper messageStoreWrapper;
    private final JsonSerializer jsonSerializer;

    abstract class Task extends MRetryableTask<Message, Message[]> {
        @Override
        public Message[] run(Message[] messages) {
            if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
                MobileMessagingLogger.w("Can't send messages without valid registration");
                throw InternalSdkError.NO_VALID_REGISTRATION.getException();
            }

            MoMessagesBody moMessagesBody = MoMessageMapper.body(mobileMessagingCore.getPushRegistrationId(), messages);

            MobileMessagingLogger.v("SEND MO >>>", moMessagesBody);
            MoMessagesResponse moMessagesResponse = mobileApiMessages.sendMO(moMessagesBody);
            MobileMessagingLogger.v("SEND MO DONE <<<", moMessagesResponse);

            return MoMessageMapper.messages(moMessagesResponse);
        }
    }

    public MoMessageSender(Context context, MobileMessagingCore mobileMessagingCore, Broadcaster broadcaster, Executor executor, MobileMessagingStats stats, MRetryPolicy retryPolicy, MobileApiMessages mobileApiMessages, MessageStoreWrapper messageStoreWrapper) {
        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.broadcaster = broadcaster;
        this.executor = executor;
        this.stats = stats;
        this.retryPolicy = retryPolicy;
        this.mobileApiMessages = mobileApiMessages;
        this.jsonSerializer = new JsonSerializer(false);
        this.messageStoreWrapper = messageStoreWrapper;
        this.noRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(0)
                .build();
    }

    public void send(final MobileMessaging.ResultListener<Message[]> listener, Message... messages) {
        send(listener, true, messages);
    }

    public void sendDontSave(final MobileMessaging.ResultListener<Message[]> listener, Message... messages) {
        send(listener, false, messages);
    }

    public void send(final MobileMessaging.ResultListener<Message[]> listener, final boolean doSave, Message... messages) {
        new Task() {
            @Override
            public void after(Message[] messages) {
                if (doSave) {
                    messageStoreWrapper.upsert(messages);
                }
                broadcaster.messagesSent(Arrays.asList(messages));
                if (listener != null) {
                    listener.onResult(new Result<>(messages));
                }
            }

            @Override
            public void error(Message[] messages, Throwable error) {
                MobileMessagingLogger.e("MobileMessaging API returned error (sending message)!");
                stats.reportError(MobileMessagingStatsError.MESSAGE_SEND_ERROR);
                broadcaster.error(MobileMessagingError.createFrom(error));

                for (Message message : messages) {
                    message.setStatus(Message.Status.ERROR);
                    message.setStatusMessage(error.getMessage());
                }

                after(messages);
            }
        }
        .retryWith(noRetryPolicy)
        .execute(executor, messages);
    }

    public void sendWithRetry(Message... messages) {
        saveMessages(messages);
        sync();
    }

    public void sync() {
        Message[] messages = getAndRemoveMessages();
        if (messages.length == 0) {
            return;
        }

        new Task() {

            @Override
            public void error(Message[] messages, Throwable error) {
                MobileMessagingLogger.e("MobileMessaging API returned error (sending messages in retry)! ", error);

                stats.reportError(MobileMessagingStatsError.MESSAGE_SEND_ERROR);
                broadcaster.error(MobileMessagingError.createFrom(error));

                saveMessages(messages);
            }
        }
        .retryWith(retryPolicy)
        .execute(executor, messages);
    }

    private void saveMessages(Message... messages) {
        String[] jsons = messagesToJson(excludeOutdatedMessages(messages));
        PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.UNSENT_MO_MESSAGES, jsons);
    }

    private Message[] getAndRemoveMessages() {
        String[] jsons = PreferenceHelper.findAndRemoveStringArray(context, MobileMessagingProperty.UNSENT_MO_MESSAGES);
        Message[] messages = messagesFromJson(jsons);
        return excludeOutdatedMessages(messages);
    }

    private String[] messagesToJson(Message... messages) {
        if (messages == null || messages.length == 0) {
            return new String[0];
        }

        List<String> jsons = new ArrayList<>(messages.length);
        for (Message message : messages) {
            jsons.add(jsonSerializer.serialize(message));
        }
        return jsons.toArray(new String[0]);
    }

    private Message[] messagesFromJson(String... jsons) {
        if (jsons == null || jsons.length == 0) {
            return new Message[0];
        }

        List<Message> messages = new ArrayList<>(jsons.length);
        for (String json : jsons) {
            messages.add(jsonSerializer.deserialize(json, Message.class));
        }
        return messages.toArray(new Message[0]);
    }

    private Message[] excludeOutdatedMessages(Message[] messages) {
        if (messages.length == 0) {
            return new Message[0];
        }

        Date now = Time.date();
        List<Message> relevantMessages = new ArrayList<>();
        for (Message message : messages) {
            long expirationTimestamp = message.getReceivedTimestamp() + MESSAGE_MAX_RETRY_LIFETIME;
            if (expirationTimestamp < now.getTime()) {
                continue;
            }

            relevantMessages.add(message);
        }
        return relevantMessages.toArray(new Message[0]);
    }
}