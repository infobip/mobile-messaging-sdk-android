package org.infobip.mobile.messaging.inbox;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

public class MobileInboxBroadcasterImpl implements MobileInboxBroadcaster {
    private final Context context;

    public MobileInboxBroadcasterImpl(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public void inboxFetched(Inbox inbox) {
        send(prepare(MobileInboxEvent.INBOX_MESSAGES_FETCHED)
                .putExtra(BroadcastParameter.EXTRA_INBOX, InboxBundleMapper.inboxToBundle(inbox)));
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
