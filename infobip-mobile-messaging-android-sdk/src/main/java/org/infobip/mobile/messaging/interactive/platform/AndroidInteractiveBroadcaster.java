package org.infobip.mobile.messaging.interactive.platform;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.interactive.InteractiveEvent;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationActionBundleMapper;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationCategoryBundleMapper;

/**
 * @author tjuric
 * @since 04/08/17.
 */
public class AndroidInteractiveBroadcaster implements InteractiveBroadcaster {

    private final Context context;

    public AndroidInteractiveBroadcaster(Context context) {
        this.context = context;
    }

    @Override
    public Intent notificationActionTapped(Message message, NotificationCategory category, NotificationAction action) {
        Intent actionTapped = prepareTappedIntent();
        actionTapped.putExtras(MessageBundleMapper.messageToBundle(message));
        actionTapped.putExtras(NotificationActionBundleMapper.notificationActionToBundle(action));
        actionTapped.putExtras(NotificationCategoryBundleMapper.notificationCategoryToBundle(category));

        context.sendBroadcast(actionTapped);
        LocalBroadcastManager.getInstance(context).sendBroadcast(actionTapped);

        return actionTapped;
    }

    private Intent prepareTappedIntent() {
        return new Intent(InteractiveEvent.NOTIFICATION_ACTION_TAPPED.getKey())
                .setPackage(context.getPackageName());
    }
}
