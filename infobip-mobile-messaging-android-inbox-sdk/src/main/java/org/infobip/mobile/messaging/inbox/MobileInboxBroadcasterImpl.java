/*
 * MobileInboxBroadcasterImpl.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MobileInboxBroadcasterImpl implements MobileInboxBroadcaster {
    private final Context context;

    public MobileInboxBroadcasterImpl(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public void inboxFetched(Inbox inbox) {
        send(prepare(MobileInboxEvent.INBOX_MESSAGES_FETCHED)
                .putExtras(InboxBundleMapper.inboxToBundle(inbox)));
    }

    @Override
    public void seenReported(@NonNull String... messageIds) {
        if (messageIds.length == 0) {
            return;
        }

        Intent seenReportsSent = prepare(MobileInboxEvent.INBOX_SEEN_REPORTED);
        Bundle extras = new Bundle();
        extras.putStringArray(BroadcastParameter.EXTRA_INBOX_SEEN_IDS, messageIds);
        seenReportsSent.putExtras(extras);
        send(seenReportsSent);
    }

    private void send(Intent intent) {
        try {
            context.sendBroadcast(intent);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception ex) {
            MobileMessagingLogger.e("Failed to send broadcast for action " + intent.getAction() + " due to exception " + ex.getMessage());
        }
    }

    private Intent prepare(MobileInboxEvent event) {
        return prepare(event.getKey());
    }

    private Intent prepare(String event) {
        return new Intent(event)
                .setPackage(context.getPackageName());
    }
}
