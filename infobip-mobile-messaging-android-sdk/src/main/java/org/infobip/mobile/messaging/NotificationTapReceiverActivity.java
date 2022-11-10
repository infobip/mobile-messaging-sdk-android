package org.infobip.mobile.messaging;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.platform.Broadcaster;

public class NotificationTapReceiverActivity extends Activity {
    private Broadcaster broadcaster;
    private MobileMessagingCore mobileMessagingCore;

    public NotificationTapReceiverActivity() {
    }

    @VisibleForTesting
    public NotificationTapReceiverActivity(Broadcaster broadcaster, MobileMessagingCore mobileMessagingCore) {
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleNotificationTap(this, getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationTap(this, getIntent());
        finish();
    }

    public void handleNotificationTap(Context context, Intent intent) {
        Bundle messageBundle = intent.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE);
        if (messageBundle == null) {
            MobileMessagingLogger.e("Received no message in NotificationTapReceiverActivity");
            return;
        }
        Message message = Message.createFrom(messageBundle);
        if (message == null) {
            MobileMessagingLogger.e("Received no message in NotificationTapReceiverActivity");
            return;
        }

        for (MessageHandlerModule module : mobileMessagingCore(context).getMessageHandlerModules()) {
            if (module.messageTapped(message)) {
                return;
            }
        }

        broadcaster(context).notificationTapped(message);

        NotificationSettings notificationSettings = mobileMessagingCore(context).getNotificationSettings();
        if (notificationSettings == null) {
            return;
        }

        if (notificationSettings.markSeenOnTap()) {
            mobileMessagingCore(context).setMessagesSeen(message.getMessageId());
        }
    }

    public Broadcaster broadcaster(Context context) {
        if (broadcaster == null) {
            broadcaster = new AndroidBroadcaster(context);
        }
        return broadcaster;
    }

    public MobileMessagingCore mobileMessagingCore(Context context) {
        if (mobileMessagingCore == null) {
            mobileMessagingCore = MobileMessagingCore.getInstance(context);
        }
        return mobileMessagingCore;
    }
}
