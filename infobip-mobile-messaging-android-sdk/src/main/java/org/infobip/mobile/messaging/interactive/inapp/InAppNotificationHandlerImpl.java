package org.infobip.mobile.messaging.interactive.inapp;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.app.ActivityStarterWrapper;
import org.infobip.mobile.messaging.interactive.MobileInteractive;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.inapp.cache.OneMessageCache;
import org.infobip.mobile.messaging.interactive.inapp.cache.OneMessagePreferenceCache;
import org.infobip.mobile.messaging.interactive.inapp.foreground.ForegroundStateMonitorImpl;
import org.infobip.mobile.messaging.interactive.inapp.rules.InAppRules;
import org.infobip.mobile.messaging.interactive.inapp.rules.ShowOrNot;
import org.infobip.mobile.messaging.interactive.inapp.view.DialogStack;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppView;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppViewFactory;
import org.infobip.mobile.messaging.interactive.inapp.view.QueuedDialogStack;
import org.infobip.mobile.messaging.interactive.platform.AndroidInteractiveBroadcaster;
import org.infobip.mobile.messaging.interactive.platform.InteractiveBroadcaster;
import org.infobip.mobile.messaging.interactive.predefined.PredefinedActionsProvider;
import org.infobip.mobile.messaging.util.StringUtils;

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
    private final ActivityStarterWrapper activityStarterWrapper;

    @VisibleForTesting
    InAppNotificationHandlerImpl(MobileInteractive mobileInteractive, InAppViewFactory inAppViewFactory, InAppRules inAppRules, OneMessageCache oneMessageCache, DialogStack dialogStack, InteractiveBroadcaster interactiveBroadcaster, ActivityStarterWrapper activityStarterWrapper) {
        this.mobileInteractive = mobileInteractive;
        this.inAppViewFactory = inAppViewFactory;
        this.inAppRules = inAppRules;
        this.oneMessageCache = oneMessageCache;
        this.dialogStack = dialogStack;
        this.interactiveBroadcaster = interactiveBroadcaster;
        this.activityStarterWrapper = activityStarterWrapper;
    }

    public InAppNotificationHandlerImpl(Context context) {
        this(MobileInteractive.getInstance(context),
                new InAppViewFactory(),
                new InAppRules(
                        MobileInteractive.getInstance(context),
                        new ForegroundStateMonitorImpl(context),
                        new PredefinedActionsProvider(context),
                        MobileMessagingCore.getInstance(context).getNotificationSettings()
                ),
                new OneMessagePreferenceCache(context),
                new QueuedDialogStack(),
                new AndroidInteractiveBroadcaster(context),
                new ActivityStarterWrapper(context,
                        MobileMessagingCore.getInstance(context))
        );
    }

    @Override
    public void handleMessage(Message message) {
        ShowOrNot showOrNot = inAppRules.shouldDisplayDialogFor(message);

        if (!showOrNot.shouldShowNow() && showOrNot.shouldShowWhenInForeground()) {
            oneMessageCache.save(message);
            return;
        }

        displayDialogFor(message, inAppRules.areModalInAppNotificationsEnabled());
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
    public void displayDialogFor(Message message) {
        displayDialogFor(message, true);
    }

    private void displayDialogFor(Message message, Boolean displayingEnabled) {
        ShowOrNot showOrNot = inAppRules.shouldDisplayDialogFor(message);
        if (!showOrNot.shouldShowNow()) {
            return;
        }

        if (displayingEnabled) {
            dialogStack.add(
                        inAppViewFactory.create(showOrNot.getBaseActivityForDialog(), this),
                        message,
                        showOrNot.getCategory(),
                        showOrNot.getActionsToShowFor());
        } else {
            interactiveBroadcaster.inAppNotificationIsReadyToDisplay(message);
        }
    }

    @Override
    public void userPressedNotificationButtonForMessage(@NonNull Message message) {
        oneMessageCache.remove(message);
    }

    @Override
    public void userTappedNotificationForMessage(@NonNull Message message) {
        if (TextUtils.isEmpty(message.getCategory())) {
            oneMessageCache.remove(message);
        }
    }

    @Override
    public void buttonPressedFor(@NonNull InAppView inAppView, @NonNull Message message, NotificationCategory category, @NonNull NotificationAction action) {
        mobileInteractive.triggerSdkActionsFor(action, message);
        Intent callbackIntent = interactiveBroadcaster.notificationActionTapped(message, category, action);

        if (PredefinedActionsProvider.isOpenAction(action.getId()) || action.bringsAppToForeground()) {
            if (StringUtils.isNotBlank(message.getWebViewUrl())) {
                activityStarterWrapper.startWebViewActivity(callbackIntent, message.getWebViewUrl());
            } else if (StringUtils.isNotBlank(message.getBrowserUrl())) {
                activityStarterWrapper.startBrowser(message.getBrowserUrl());
            } else {
                activityStarterWrapper.startCallbackActivity(callbackIntent);
            }
        }
    }

    @Override
    public void dismissed(@NonNull InAppView inAppView) {
        dialogStack.remove(inAppView);
    }
}
