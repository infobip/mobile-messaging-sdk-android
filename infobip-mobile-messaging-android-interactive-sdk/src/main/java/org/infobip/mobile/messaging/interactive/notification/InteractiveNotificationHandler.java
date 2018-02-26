package org.infobip.mobile.messaging.interactive.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.interactive.MobileInteractiveImpl;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationActionBundleMapper;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationCategoryBundleMapper;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.notification.CoreNotificationHandler;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_MESSAGE;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_NOTIFICATION_ID;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_ACTION;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_CATEGORY;


public class InteractiveNotificationHandler implements NotificationHandler {

    private Context context;

    public InteractiveNotificationHandler() {

    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void displayNotification(Message message) {
        if (context == null) return;

        CoreNotificationHandler notificationHandler = new CoreNotificationHandler();
        notificationHandler.setContext(context);
        int notificationId = notificationHandler.getNotificationId(message);
        NotificationCompat.Builder builder = getNotificationBuilder(message, notificationHandler, notificationId);

        notificationHandler.displayNotification(builder, message, notificationId);
    }

    private NotificationCompat.Builder getNotificationBuilder(Message message, CoreNotificationHandler notificationHandler, int notificationId) {
        NotificationCompat.Builder builder = notificationHandler.notificationCompatBuilder(message);
        if (builder == null) return null;

        String category = message.getCategory();
        NotificationCategory triggeredNotificationCategory = MobileInteractiveImpl.getInstance(context).getNotificationCategory(category);
        setNotificationActions(builder, message, triggeredNotificationCategory, notificationId);

        return builder;
    }

    private void setNotificationActions(NotificationCompat.Builder notificationBuilder,
                                        Message message,
                                        NotificationCategory triggeredNotificationCategory,
                                        int notificationId) {
        if (triggeredNotificationCategory == null) {
            return;
        }

        NotificationAction[] notificationActions = triggeredNotificationCategory.getNotificationActions();
        for (NotificationAction notificationAction : notificationActions) {
            PendingIntent pendingIntent = createActionTapPendingIntent(message, triggeredNotificationCategory, notificationAction, notificationId);
            notificationBuilder.addAction(createAndroidNotificationAction(notificationAction, pendingIntent));
        }
    }

    @NonNull
    private NotificationCompat.Action createAndroidNotificationAction(NotificationAction notificationAction, PendingIntent pendingIntent) {
        NotificationCompat.Action.Builder builder = new NotificationCompat.Action.Builder(
                notificationAction.getIcon(),
                notificationActionTitle(context, notificationAction),
                pendingIntent);

        if (notificationAction.hasInput()) {
            RemoteInput.Builder inputBuilder = new RemoteInput.Builder(notificationAction.getId());
            String inputPlaceholderText = notificationActionInputPlaceholder(context, notificationAction);
            if (inputPlaceholderText != null) {
                inputBuilder.setLabel(inputPlaceholderText);
            }
            builder.addRemoteInput(inputBuilder.build());
        }

        return builder.build();
    }

    private static String notificationActionTitle(Context context, NotificationAction action) {
        return action.getTitleResourceId() != 0 ? context.getString(action.getTitleResourceId()) : action.getTitleText();
    }

    private static String notificationActionInputPlaceholder(Context context, NotificationAction action) {
        return action.getInputPlaceholderResourceId() != 0 ? context.getString(action.getInputPlaceholderResourceId()) : action.getInputPlaceholderText();
    }

    @SuppressWarnings("WrongConstant")
    @NonNull
    private PendingIntent createActionTapPendingIntent(Message message, NotificationCategory notificationCategory, NotificationAction notificationAction, int notificationId) {
        Intent intent = new Intent(context, NotificationActionTapReceiver.class);
        intent.setAction(message.getMessageId() + notificationAction.getId());
        intent.putExtra(EXTRA_MESSAGE, MessageBundleMapper.messageToBundle(message));
        intent.putExtra(EXTRA_TAPPED_ACTION, NotificationActionBundleMapper.notificationActionToBundle(notificationAction));
        intent.putExtra(EXTRA_TAPPED_CATEGORY, NotificationCategoryBundleMapper.notificationCategoryToBundle(notificationCategory));
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        return PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
}
