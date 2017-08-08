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
    public void notificationActionTapped(Message message, NotificationCategory category, NotificationAction action) {
        Intent actionTapped = prepareIntent(InteractiveEvent.NOTIFICATION_ACTION_TAPPED);
        actionTapped.putExtras(MessageBundleMapper.messageToBundle(message));
        actionTapped.putExtras(NotificationActionBundleMapper.notificationActionToBundle(action));
        actionTapped.putExtras(NotificationCategoryBundleMapper.notificationCategoryToBundle(category));
        context.sendBroadcast(actionTapped);
        LocalBroadcastManager.getInstance(context).sendBroadcast(actionTapped);
    }

    private Intent prepareIntent(InteractiveEvent event) {
        return prepareIntent(event.getKey());
    }

    private Intent prepareIntent(String event) {
        return new Intent(event)
                .setPackage(context.getPackageName());
    }
}
