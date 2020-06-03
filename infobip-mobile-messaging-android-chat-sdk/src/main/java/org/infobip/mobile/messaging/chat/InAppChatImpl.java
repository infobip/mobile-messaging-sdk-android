package org.infobip.mobile.messaging.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.webkit.WebView;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.chat.core.InAppChatViewImpl;
import org.infobip.mobile.messaging.chat.mobileapi.InAppChatSynchronizer;
import org.infobip.mobile.messaging.chat.properties.InAppChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;


public class InAppChatImpl extends InAppChat implements MessageHandlerModule {

    private static InAppChatImpl instance;
    private Context context;
    private AndroidBroadcaster broadcaster;
    private InAppChatViewImpl inAppChatView;
    private PropertyHelper propertyHelper;
    private WebView webView;
    private MobileApiResourceProvider mobileApiResourceProvider;
    private InAppChatSynchronizer inAppChatSynchronizer;
    private boolean syncedChatWidgetConfig;

    public static InAppChatImpl getInstance(Context context) {
        if (instance == null) {
            instance = MobileMessagingCore.getInstance(context).getMessageHandlerModule(InAppChatImpl.class);
        }
        return instance;
    }

    public InAppChatImpl() {
    }

    @Override
    public void init(Context appContext) {
        this.context = appContext;
    }

    @Override
    public boolean handleMessage(Message message) {
        if (!message.isChatMessage()) {
            return false;
        }
        broadcaster().messageReceived(message);
        MobileMessagingCore.getInstance(context).getNotificationHandler().displayNotification(message);
        return true;
    }

    @Override
    public boolean messageTapped(Message message) {
        if (!message.isChatMessage()) {
            return false;
        }
        broadcaster().notificationTapped(message);
        doCoreTappedActions(message);
        return true;
    }

    @Override
    public InAppChatViewImpl inAppChatView() {
        if (inAppChatView == null) {
            inAppChatView = new InAppChatViewImpl(context);
        }
        return inAppChatView;
    }

    @Override
    public void setActivitiesToStartOnMessageTap(Class... activityClasses) {
        propertyHelper().saveClasses(InAppChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES, activityClasses);
    }

    // region private methods

    synchronized private AndroidBroadcaster broadcaster() {
        if (broadcaster == null) {
            broadcaster = new AndroidBroadcaster(context);
        }
        return broadcaster;
    }

    private MobileApiResourceProvider mobileApiResourceProvider() {
        if (mobileApiResourceProvider == null) {
            mobileApiResourceProvider = new MobileApiResourceProvider();
        }
        return mobileApiResourceProvider;
    }

    synchronized private InAppChatSynchronizer inAppChatSynchronizer() {
        if (inAppChatSynchronizer == null) {
            inAppChatSynchronizer = new InAppChatSynchronizer(
                    context,
                    MobileMessagingCore.getInstance(context),
                    broadcaster(),
                    mobileApiResourceProvider().getMobileApiChat(context));
        }
        return inAppChatSynchronizer;
    }

    synchronized private PropertyHelper propertyHelper() {
        if (propertyHelper == null) {
            propertyHelper = new PropertyHelper(context);
        }
        return propertyHelper;
    }

    synchronized private WebView webView() {
        if (webView == null) {
            webView = new WebView(context);
        }
        return webView;
    }

    @Override
    public void applicationInForeground() {
        if (!syncedChatWidgetConfig) {
            inAppChatSynchronizer().getWidgetConfiguration(); //need to know the failed state
            syncedChatWidgetConfig = true; // review no internet state
        }
    }

    @Override
    public void cleanup() {
        syncedChatWidgetConfig = false;
        webView().clearHistory();
        webView().clearCache(true);
        PropertyHelper.remove(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID.getKey());
        PropertyHelper.remove(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE.getKey());
        PropertyHelper.remove(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.getKey());
        PropertyHelper.remove(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.getKey());
    }

    @Override
    public void depersonalize() {
        syncedChatWidgetConfig = false;
        webView().clearHistory();
        webView().clearCache(true);
    }

    @Override
    public void performSyncActions() {
        if (!syncedChatWidgetConfig) {
            inAppChatSynchronizer().getWidgetConfiguration();
            syncedChatWidgetConfig = true;
        }
    }

    @NonNull
    private TaskStackBuilder stackBuilderForNotificationTap(Message message) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        Bundle messageBundle = MessageBundleMapper.messageToBundle(message);
        Class[] classes = propertyHelper().findClasses(InAppChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES);
        if (classes != null) {
            for (Class cls : classes) {
                stackBuilder.addNextIntent(new Intent(context, cls)
                        .setAction(Event.NOTIFICATION_TAPPED.getKey())
                        .putExtras(messageBundle));
            }
        }

        NotificationSettings notificationSettings = MobileMessagingCore.getInstance(context).getNotificationSettings();
        if (stackBuilder.getIntentCount() == 0 && notificationSettings != null && notificationSettings.getCallbackActivity() != null) {
            stackBuilder.addNextIntent(new Intent(context, notificationSettings.getCallbackActivity())
                    .setAction(Event.NOTIFICATION_TAPPED.getKey())
                    .putExtras(messageBundle));
        }

        return stackBuilder;
    }

    private void doCoreTappedActions(Message chatMessage) {
        TaskStackBuilder stackBuilder = stackBuilderForNotificationTap(chatMessage);
        if (stackBuilder.getIntentCount() != 0) {
            stackBuilder.startActivities();
        }
    }

    // endregion
}
