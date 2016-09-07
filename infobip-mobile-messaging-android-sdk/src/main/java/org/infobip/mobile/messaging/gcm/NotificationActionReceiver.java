package org.infobip.mobile.messaging.gcm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.infobip.mobile.messaging.Actionable;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;

/**
 * @author pandric
 * @since 11.07.2016.
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getBundleExtra(MobileMessagingProperty.EXTRA_MESSAGE.getKey());
        if (bundle == null) return;

        Message message = Message.createFrom(bundle);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String action = intent.getAction();
        if (action == null) return;

        int notificationId;
        switch (action) {
            case NotificationAction.ACTION_MARK_SEEN:
                MobileMessagingCore.getInstance(context).setMessagesSeen(message.getMessageId());
                notificationId = MobileMessageHandler.CHAT_NOTIFICATION_ID;
                break;

            case NotificationAction.ACTION_REPLY:
                MobileMessaging.OnReplyClickListener onReplyClickListener = MobileMessagingCore.getInstance(context).getOnReplyClickListener();
                if (onReplyClickListener != null) {
                    onReplyClickListener.onReplyClicked(intent);
                }
                notificationId = MobileMessageHandler.CHAT_NOTIFICATION_ID;
                break;

            case NotificationAction.ACTION_COUPON_URL:
                String stringUrl = intent.getStringExtra(Actionable.EXTRA_COUPON_URL);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(stringUrl == null ? "" : stringUrl));
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
                notificationId = MobileMessageHandler.COUPON_NOTIFICATION_ID;
                break;

            default:
                notificationId = MobileMessageHandler.DEFAULT_NOTIFICATION_ID;
        }

        notificationManager.cancel(notificationId);
    }
}
