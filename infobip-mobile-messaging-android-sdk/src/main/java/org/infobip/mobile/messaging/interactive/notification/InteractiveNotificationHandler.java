package org.infobip.mobile.messaging.interactive.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.interactive.MobileInteractiveImpl;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationActionBundleMapper;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationCategoryBundleMapper;
import org.infobip.mobile.messaging.notification.BaseNotificationHandler;
import org.infobip.mobile.messaging.notification.NotificationHandler;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_MESSAGE;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_NOTIFICATION_ID;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_ACTION;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_CATEGORY;
import static org.infobip.mobile.messaging.BroadcastParameter.NOTIFICATION_NOT_DISPLAYED_ID;


public class InteractiveNotificationHandler implements NotificationHandler {

    private final Context context;
    private final BaseNotificationHandler baseNotificationHandler;

    public InteractiveNotificationHandler(Context context) {
        this.context = context;
        this.baseNotificationHandler = new BaseNotificationHandler(context);
    }

    @Override
    public int displayNotification(Message message) {
        if (context == null) return NOTIFICATION_NOT_DISPLAYED_ID;

        int notificationId = baseNotificationHandler.getNotificationId(message);
        NotificationCompat.Builder builder = getNotificationBuilder(message, notificationId);
        boolean displayed = baseNotificationHandler.displayNotification(builder, message, notificationId);

        if (!displayed) return NOTIFICATION_NOT_DISPLAYED_ID;
        return notificationId;
    }

    @Override
    public void cancelAllNotifications() {
        baseNotificationHandler.cancelAllNotifications();
    }

    private NotificationCompat.Builder getNotificationBuilder(Message message, int notificationId) {
        NotificationCompat.Builder builder = baseNotificationHandler.createNotificationCompatBuilder(message);
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
        int flags = PendingIntent.FLAG_CANCEL_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags | PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, notificationId, intent, flags);
    }
}
