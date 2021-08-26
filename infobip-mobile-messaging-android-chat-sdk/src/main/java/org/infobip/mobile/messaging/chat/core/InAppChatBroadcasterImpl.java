package org.infobip.mobile.messaging.chat.core;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

public class InAppChatBroadcasterImpl implements InAppChatBroadcaster {
    private final Context context;

    public InAppChatBroadcasterImpl(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public void chatConfigurationSynced() {
        send(prepare(InAppChatEvent.CHAT_CONFIGURATION_SYNCED));
    }

    @Override
    public void unreadMessagesCounterUpdated(int unreadMessagesCount) {
        send(prepare(InAppChatEvent.UNREAD_MESSAGES_COUNTER_UPDATED)
                .putExtra(BroadcastParameter.EXTRA_UNREAD_CHAT_MESSAGES_COUNT, unreadMessagesCount));
    }

    private void send(Intent intent) {
        try {
            context.sendBroadcast(intent);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        } catch (Exception ex) {
            MobileMessagingLogger.e("Failed to send broadcast for action " + intent.getAction() + " due to exception " + ex.getMessage());
        }
    }

    private Intent prepare(InAppChatEvent event) {
        return prepare(event.getKey());
    }

    private Intent prepare(String event) {
        return new Intent(event)
                .setPackage(context.getPackageName());
    }

}
