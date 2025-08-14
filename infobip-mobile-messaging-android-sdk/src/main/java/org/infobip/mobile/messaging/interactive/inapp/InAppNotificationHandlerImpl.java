package org.infobip.mobile.messaging.interactive.inapp;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.OpenLivechatAction;
import org.infobip.mobile.messaging.app.ActivityStarterWrapper;
import org.infobip.mobile.messaging.app.ContentIntentWrapper;
import org.infobip.mobile.messaging.interactive.MobileInteractive;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.inapp.cache.OneMessageCache;
import org.infobip.mobile.messaging.interactive.inapp.cache.OneMessagePreferenceCache;
import org.infobip.mobile.messaging.interactive.inapp.foreground.ForegroundStateMonitorImpl;
import org.infobip.mobile.messaging.interactive.inapp.rules.InAppRules;
import org.infobip.mobile.messaging.interactive.inapp.rules.ShowOrNot;
import org.infobip.mobile.messaging.interactive.inapp.view.DialogStack;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppNativeView;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppView;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppViewFactory;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppWebView;
import org.infobip.mobile.messaging.interactive.inapp.view.QueuedDialogStack;
import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppCtx;
import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppNativeCtx;
import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppWebCtx;
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
    private final MessageHandlerModule inAppChatModule;
    private ContentIntentWrapper contentIntentWrapper;

    @VisibleForTesting
    InAppNotificationHandlerImpl(MobileInteractive mobileInteractive, InAppViewFactory inAppViewFactory, InAppRules inAppRules, OneMessageCache oneMessageCache, DialogStack dialogStack,
                                 InteractiveBroadcaster interactiveBroadcaster, ActivityStarterWrapper activityStarterWrapper, MessageHandlerModule inAppChatModule) {
        this.mobileInteractive = mobileInteractive;
        this.inAppViewFactory = inAppViewFactory;
        this.inAppRules = inAppRules;
        this.oneMessageCache = oneMessageCache;
        this.dialogStack = dialogStack;
        this.interactiveBroadcaster = interactiveBroadcaster;
        this.activityStarterWrapper = activityStarterWrapper;
        this.inAppChatModule = inAppChatModule;
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
                new ActivityStarterWrapper(context, MobileMessagingCore.getInstance(context)),
                MobileMessagingCore.getInstance(context).findMessageHandlerModule(MobileMessagingCore.IN_APP_CHAT_MESSAGE_HANDLER_MODULE_NAME)
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
    public void handleMessage(InAppWebViewMessage message) {
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

        InAppWebViewMessage wvMessage = InAppWebViewMessage.createInAppWebViewMessage(message);
        if (wvMessage != null)
            handleMessage(wvMessage);
        else
            handleMessage(message);
    }

    @Override
    public void displayDialogFor(Message message) {
        InAppWebViewMessage wvMessage = InAppWebViewMessage.createInAppWebViewMessage(message);

        if (wvMessage != null)
            displayDialogFor(wvMessage, true);
        else
            displayDialogFor(message, true);
    }

    private void displayDialogFor(Message message, Boolean displayingEnabled) {
        ShowOrNot showOrNot = inAppRules.shouldDisplayDialogFor(message);

        if (!showOrNot.shouldShowNow()) {
            return;
        }

        if (displayingEnabled) {
            dialogStack.add(createInAppNativeCtx(message, showOrNot));
        } else {
            interactiveBroadcaster.inAppNotificationIsReadyToDisplay(message);
        }
    }

    private void displayDialogFor(InAppWebViewMessage message, Boolean displayingEnabled) {
        ShowOrNot showOrNot = inAppRules.shouldDisplayDialogFor(message);

        if (!showOrNot.shouldShowNow()) {
            return;
        }

        if (displayingEnabled) {
            dialogStack.add(createInAppWebCtx(message, showOrNot));
        } else {
            interactiveBroadcaster.inAppNotificationIsReadyToDisplay(message);
        }
    }

    private InAppCtx createInAppWebCtx(InAppWebViewMessage wvMessage, ShowOrNot showOrNot) {
        InAppWebView view = inAppViewFactory.create(showOrNot.getBaseActivityForDialog(), this, wvMessage);
        return new InAppWebCtx(view, wvMessage, showOrNot.getActionsToShowFor());
    }

    private InAppCtx createInAppNativeCtx(Message message, ShowOrNot showOrNot) {
        InAppNativeView view = inAppViewFactory.create(showOrNot.getBaseActivityForDialog(), this, message);
        return new InAppNativeCtx(view, message, showOrNot.getCategory(), showOrNot.getActionsToShowFor());
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
    public void buttonPressedFor(@NonNull InAppNativeView inAppView, @NonNull Message message, NotificationCategory category, @NonNull NotificationAction action) {
        mobileInteractive.triggerSdkActionsFor(action, message);
        Intent callbackIntent = interactiveBroadcaster.notificationActionTapped(message, category, action);
        handleButtonPress(message, action, callbackIntent);
    }

    @Override
    public void actionButtonPressedFor(@NonNull InAppWebView inAppView, @NonNull Message message, NotificationCategory category, @NonNull NotificationAction action) {
        Intent callbackIntent = interactiveBroadcaster.notificationActionTapped(message, category, action);
        handleButtonPress(message, action, callbackIntent);
    }

    @Override
    public void notificationPressedFor(@NonNull InAppWebView inAppView, @NonNull Message message, @NonNull NotificationAction action, Context context) {
        Intent callbackIntent;
        if (StringUtils.isNotBlank(message.getWebViewUrl())) {
            callbackIntent = contentIntentWrapper(context).createWebViewContentIntent(message);
        } else if (StringUtils.isNotBlank(message.getBrowserUrl())) {
            callbackIntent = contentIntentWrapper(context).createBrowserIntent(message.getBrowserUrl());
        } else {
            callbackIntent = contentIntentWrapper(context).createContentIntent(message, MobileMessagingCore.getInstance(context).getNotificationSettings());
        }
        handleButtonPress(message, action, callbackIntent);
    }

    private void handleButtonPress(@NonNull Message message, @NonNull NotificationAction action, Intent callbackIntent) {
        if (PredefinedActionsProvider.isOpenAction(action.getId()) || action.bringsAppToForeground()) {
            if (StringUtils.isNotBlank(message.getWebViewUrl())) {
                activityStarterWrapper.startWebViewActivity(callbackIntent, message.getWebViewUrl());
            } else if (StringUtils.isNotBlank(message.getBrowserUrl())) {
                activityStarterWrapper.startBrowser(message.getBrowserUrl());
            } else if (OpenLivechatAction.parseFrom(message) != null && inAppChatModule != null) {
                inAppChatModule.messageTapped(message);
            } else {
                activityStarterWrapper.startCallbackActivity(callbackIntent);
            }
        }
    }

    private ContentIntentWrapper contentIntentWrapper(Context context) {
        if (contentIntentWrapper == null) {
            contentIntentWrapper = new ContentIntentWrapper(context);
        }
        return contentIntentWrapper;
    }

    @Override
    public void dismissed(@NonNull InAppView inAppView) {
        dialogStack.remove(inAppView);
    }
}
