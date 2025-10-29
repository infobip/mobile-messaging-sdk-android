/*
 * MessageStoreWrapperImpl.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.storage;

import android.content.Context;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
