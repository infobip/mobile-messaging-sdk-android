package org.infobip.mobile.messaging.interactive.notification;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.RemoteInput;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.interactive.InteractiveEvent;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationActionBundleMapper;
import org.infobip.mobile.messaging.interactive.platform.AndroidInteractiveBroadcaster;
import org.infobip.mobile.messaging.interactive.platform.InteractiveBroadcaster;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_ACTION;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_TAPPED_CATEGORY;


public class NotificationActionTapReceiver extends BroadcastReceiver {

    private InteractiveBroadcaster broadcaster;
    private MobileMessagingCore mobileMessagingCore;

    public NotificationActionTapReceiver() {
    }

    @VisibleForTesting
    public NotificationActionTapReceiver(InteractiveBroadcaster broadcaster, MobileMessagingCore mobileMessagingCore) {
        this.broadcaster = broadcaster;
        this.mobileMessagingCore = mobileMessagingCore;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle actionBundle = intent.getBundleExtra(EXTRA_TAPPED_ACTION);
        Bundle categoryBundle = intent.getBundleExtra(EXTRA_TAPPED_CATEGORY);
        int notificationId = intent.getIntExtra(BroadcastParameter.EXTRA_NOTIFICATION_ID, -1);
        Bundle messageBundle = intent.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE);

        Message message = Message.createFrom(messageBundle);
        NotificationCategory notificationCategory = NotificationCategory.createFrom(categoryBundle);
        NotificationAction notificationAction = NotificationAction.createFrom(actionBundle);
        String inputText = getInputTextFromIntent(intent, notificationAction);
        cancelNotification(context, notificationId);

        if (message == null) {
            MobileMessagingLogger.e("Received no message in NotificationActionTapReceiver");
            return;
        }
        if (notificationAction == null) {
            MobileMessagingLogger.e("Received no action in NotificationActionTapReceiver");
            return;
        }
        if (notificationCategory == null) {
            MobileMessagingLogger.e("Received no notification category in NotificationActionTapReceiver");
            return;
        }

        if (inputText != null) {
            notificationAction.setInputText(inputText);
        }

        broadcaster(context).notificationActionTapped(message, notificationCategory, notificationAction);

        markAsSeen(context, message);
        sendMo(context, notificationCategory, notificationAction);
        startCallbackActivity(context, intent, messageBundle, actionBundle, categoryBundle);
    }

    private String getInputTextFromIntent(Intent intent, NotificationAction notificationAction) {
        if (notificationAction == null || !notificationAction.hasInput()) {
            return null;
        }

        Bundle input = RemoteInput.getResultsFromIntent(intent);
        CharSequence sequence = input.getCharSequence(notificationAction.getId());
        return sequence != null ? sequence.toString() : "";
    }

    private void markAsSeen(Context context, Message message) {
        NotificationSettings notificationSettings = mobileMessagingCore(context).getNotificationSettings();
        if (notificationSettings == null) {
            return;
        }
        if (notificationSettings.markSeenOnTap()) {
            mobileMessagingCore(context).setMessagesSeen(message.getMessageId());
        }
    }

    private void sendMo(Context context, NotificationCategory category, NotificationAction action) {
        if (!action.sendsMoMessage()) {
            return;
        }

        mobileMessagingCore(context).sendMessagesWithRetry(messageFor(category, action));
    }

    private Message messageFor(final NotificationCategory category, final NotificationAction action) {
        Message message = new Message();
        message.setBody(category.getCategoryId() + " " + action.getId());
        return message;
    }

    private void cancelNotification(Context context, int notificationId) {
        if (notificationId != -1) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }

    private void startCallbackActivity(Context context, Intent intent, Bundle messageBundle, Bundle actionBundle, Bundle categoryBundle) {
        NotificationAction notificationAction = NotificationActionBundleMapper.notificationActionFromBundle(actionBundle);
        if (notificationAction == null) {
            return;
        }

        if (!notificationAction.bringsAppToForeground()) {
            return;
        }

        NotificationSettings notificationSettings = mobileMessagingCore(context).getNotificationSettings();
        if (notificationSettings == null) {
            return;
        }
        Class callbackActivity = notificationSettings.getCallbackActivity();
        if (callbackActivity == null) {
            MobileMessagingLogger.e("Callback activity is not set, cannot proceed");
            return;
        }

        int intentFlags = intent.getIntExtra(MobileMessagingProperty.EXTRA_INTENT_FLAGS.getKey(),
                (Integer) MobileMessagingProperty.INTENT_FLAGS.getDefaultValue());

        Intent callbackIntent = new Intent(context, callbackActivity);
        callbackIntent.setAction(InteractiveEvent.NOTIFICATION_ACTION_TAPPED.getKey());
        callbackIntent.putExtras(messageBundle);
        callbackIntent.putExtras(actionBundle);
        callbackIntent.putExtras(categoryBundle);

        // FLAG_ACTIVITY_NEW_TASK has to be here because we're starting activity outside of activity context
        callbackIntent.addFlags(intentFlags | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callbackIntent);
    }

    private InteractiveBroadcaster broadcaster(Context context) {
        if (broadcaster == null) {
            broadcaster = new AndroidInteractiveBroadcaster(context);
        }
        return broadcaster;
    }

    private MobileMessagingCore mobileMessagingCore(Context context) {
        if (mobileMessagingCore == null) {
            mobileMessagingCore = MobileMessagingCore.getInstance(context);
        }
        return mobileMessagingCore;
    }
}
