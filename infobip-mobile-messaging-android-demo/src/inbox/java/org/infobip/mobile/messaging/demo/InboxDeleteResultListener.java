/*
 * InboxDeleteResultListener.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Records success for the originating inbox without retaining or depending on an Activity. */
final class InboxDeleteResultListener extends MobileMessaging.ResultListener<Void> {
    interface FailureListener {
        void onInboxDeleteFailed();
    }

    private final InboxDeletionStore.UserState origin;
    private final Set<String> messageIds;
    private final WeakReference<FailureListener> failureListener;

    InboxDeleteResultListener(InboxDeletionStore.UserState origin, Collection<String> messageIds,
                              FailureListener failureListener) {
        this.origin = origin;
        this.messageIds = new HashSet<>(messageIds);
        this.failureListener = new WeakReference<>(failureListener);
    }

    @Override
    public void onResult(Result<Void, MobileMessagingError> result) {
        if (result.isSuccess()) {
            // Always record success, even after the initiating screen is gone or the user changes.
            origin.recordDeleted(messageIds);
        } else {
            FailureListener listener = failureListener.get();
            if (listener != null) {
                listener.onInboxDeleteFailed();
            }
        }
    }
}
