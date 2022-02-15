package org.infobip.mobile.messaging.storage;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

/**
 * @author sslavin
 * @since 29/08/2017.
 */

public class MessageStoreWrapperImpl implements MessageStoreWrapper {

    @NonNull
    private final Context context;

    @Nullable
    private final MessageStore messageStore;

    public MessageStoreWrapperImpl(@NonNull Context context, @Nullable MessageStore messageStore) {
        this.context = context;
        this.messageStore = messageStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upsert(@NonNull Message message) {
        if (messageStore == null) {
            MobileMessagingLogger.d("Skipping save message: " + message.getMessageId());
            return;
        }

        messageStore.save(context, message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upsert(@NonNull Message[] messages) {
        if (messageStore == null) {
            for (Message m : messages) {
                MobileMessagingLogger.d("Skipping save message: " + m.getMessageId());
            }
            return;
        }

        messageStore.save(context, messages);
    }
}
