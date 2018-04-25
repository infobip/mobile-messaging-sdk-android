package org.infobip.mobile.messaging.interactive.inapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.interactive.MobileInteractive;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.inapp.cache.OneMessageCache;
import org.infobip.mobile.messaging.interactive.inapp.cache.OneMessagePreferenceCache;
import org.infobip.mobile.messaging.interactive.inapp.rules.InAppRules;
import org.infobip.mobile.messaging.interactive.inapp.rules.ShowOrNot;
import org.infobip.mobile.messaging.interactive.inapp.view.DialogStack;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppView;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppViewFactory;
import org.infobip.mobile.messaging.interactive.inapp.view.QueuedDialogStack;
import org.infobip.mobile.messaging.interactive.platform.AndroidInteractiveBroadcaster;
import org.infobip.mobile.messaging.interactive.platform.InteractiveBroadcaster;

/**
 * @author sslavin
 * @since 11/04/2018.
 */

public class InAppNotificationHandlerImpl implements InAppNotificationHandler, InAppView.Callback {

    private final MobileInteractive mobileInteractive;
    private final InAppViewFactory inAppViewFactory;
    private final InAppRules inAppRules;
    private final OneMessageCache oneMessageCache;
    private final DialogStack dialogStack;
    private final InteractiveBroadcaster interactiveBroadcaster;

    @VisibleForTesting
    InAppNotificationHandlerImpl(MobileInteractive mobileInteractive, InAppViewFactory inAppViewFactory, InAppRules inAppRules, OneMessageCache oneMessageCache, DialogStack dialogStack, InteractiveBroadcaster interactiveBroadcaster) {
        this.mobileInteractive = mobileInteractive;
        this.inAppViewFactory = inAppViewFactory;
        this.inAppRules = inAppRules;
        this.oneMessageCache = oneMessageCache;
        this.dialogStack = dialogStack;
        this.interactiveBroadcaster = interactiveBroadcaster;
    }

    public InAppNotificationHandlerImpl(Context context) {
        this(MobileInteractive.getInstance(context),
                new InAppViewFactory(),
                new InAppRules(
                        MobileInteractive.getInstance(context),
                        MobileMessagingCore.getInstance(context).getForegroundStateMonitor()
                ),
                new OneMessagePreferenceCache(context),
                new QueuedDialogStack(),
                new AndroidInteractiveBroadcaster(context));
    }

    @Override
    public void handleMessage(Message message) {
        ShowOrNot showOrNot = inAppRules.shouldDisplayDialogFor(message);
        if (showOrNot.shouldShowNow()) {
            dialogStack.add(
                    inAppViewFactory.create(showOrNot.getBaseActivityForDialog(),this),
                    message,
                    showOrNot.getCategory(),
                    showOrNot.getActionsToShowFor());
            return;
        }

        if (showOrNot.shouldShowWhenInForeground()) {
            oneMessageCache.save(message);
        }
    }

    @Override
    public void appWentToForeground() {
        dialogStack.clear();
        Message message = oneMessageCache.getAndRemove();
        if (message == null) {
            return;
        }

        handleMessage(message);
    }

    @Override
    public void userPressedNotificationButtonForMessage(@NonNull Message message) {
        oneMessageCache.remove(message);
    }

    @Override
    public void buttonPressedFor(@NonNull InAppView inAppView, @NonNull Message message, @NonNull NotificationCategory category, @NonNull NotificationAction action) {
        mobileInteractive.triggerSdkActionsFor(action, message);
        interactiveBroadcaster.notificationActionTapped(message, category, action);
    }

    @Override
    public void dismissed(@NonNull InAppView inAppView) {
        dialogStack.remove(inAppView);
    }
}
