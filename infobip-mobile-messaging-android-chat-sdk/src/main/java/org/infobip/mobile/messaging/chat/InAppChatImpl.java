package org.infobip.mobile.messaging.chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.webkit.WebView;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.chat.core.InAppChatBroadcasterImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatViewImpl;
import org.infobip.mobile.messaging.chat.mobileapi.InAppChatSynchronizer;
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.chat.view.InAppChatActivity;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;


public class InAppChatImpl extends InAppChat implements MessageHandlerModule {

    private static InAppChatImpl instance;
    private Context context;
    private AndroidBroadcaster coreBroadcaster;
    private InAppChatBroadcasterImpl inAppChatBroadcaster;
    private InAppChatViewImpl inAppChatView;
    private PropertyHelper propertyHelper;
    private WebView webView;
    private MobileApiResourceProvider mobileApiResourceProvider;
    private InAppChatSynchronizer inAppChatSynchronizer;
    private static Boolean isChatWidgetConfigSynced = null;

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
    public void activate() {
        propertyHelper().saveBoolean(MobileMessagingChatProperty.IN_APP_CHAT_ACTIVATED, true);
    }

    @Override
    public boolean handleMessage(Message message) {
        if (!message.isChatMessage()) {
            return false;
        }
        coreBroadcaster().messageReceived(message);
        if (!isChatWidgetActivityForeground()) {
            MobileMessagingCore.getInstance(context).getNotificationHandler().displayNotification(message);
        }
        return true;
    }

    private boolean isChatWidgetActivityForeground() {
        Activity activity = null;
        ActivityLifecycleMonitor activityLifecycleMonitor = MobileMessagingCore.getInstance(context).getActivityLifecycleMonitor();
        if (activityLifecycleMonitor != null) {
            activity = activityLifecycleMonitor.getForegroundActivity();
        }
        return activity != null && activity.getClass().toString().equals(InAppChatActivity.class.toString());
    }

    @Override
    public boolean messageTapped(Message message) {
        if (!message.isChatMessage()) {
            return false;
        }
        coreBroadcaster().notificationTapped(message);
        doCoreTappedActions(message);
        return true;
    }

    @Override
    public InAppChatViewImpl inAppChatView() {
        if (inAppChatView == null) {
            inAppChatView = new InAppChatViewImpl(context);
        }
        if (!isActivated()) {
            MobileMessagingLogger.e("In-app chat wasn't activated, call activate()");
        }
        return inAppChatView;
    }

    @Override
    public void setActivitiesToStartOnMessageTap(Class... activityClasses) {
        propertyHelper().saveClasses(MobileMessagingChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES, activityClasses);
    }

    public static Boolean getIsChatWidgetConfigSynced() {
        return isChatWidgetConfigSynced;
    }

    // region private methods

    synchronized private AndroidBroadcaster coreBroadcaster() {
        if (coreBroadcaster == null) {
            coreBroadcaster = new AndroidBroadcaster(context);
        }
        return coreBroadcaster;
    }

    synchronized private InAppChatBroadcasterImpl inAppChatBroadcaster() {
        if (inAppChatBroadcaster == null) {
            inAppChatBroadcaster = new InAppChatBroadcasterImpl(context);
        }
        return inAppChatBroadcaster;
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
                    coreBroadcaster(),
                    inAppChatBroadcaster(),
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
        performSyncActions();
    }

    @Override
    public void cleanup() {
        mobileApiResourceProvider = null;
        inAppChatSynchronizer = null;
        cleanupWidgetData();
        PropertyHelper.remove(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID.getKey());
        PropertyHelper.remove(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE.getKey());
        PropertyHelper.remove(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.getKey());
        PropertyHelper.remove(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.getKey());
        PropertyHelper.remove(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE.getKey());
    }

    // must be done on separate thread if it's not invoked by UI thread
    private void cleanupWidgetData() {
        isChatWidgetConfigSynced = false;
        try {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    webView().clearHistory();
                    webView().clearCache(true);
                    MobileMessagingLogger.d("Deleted local widget history");
                }
            });
        } catch (Exception e) {
            MobileMessagingLogger.w("Failed to delete local widget history due to " + e.getMessage());
        }
    }

    @Override
    public void depersonalize() {
        cleanupWidgetData();
    }

    @Override
    public void performSyncActions() {
        if (isActivated() && (isChatWidgetConfigSynced == null || !isChatWidgetConfigSynced)) {
            inAppChatSynchronizer().getWidgetConfiguration(new MobileMessaging.ResultListener<WidgetInfo>() {
                @Override
                public void onResult(Result<WidgetInfo, MobileMessagingError> result) {
                    isChatWidgetConfigSynced = result.isSuccess();
                }
            });
        }
    }

    @NonNull
    private TaskStackBuilder stackBuilderForNotificationTap(Message message) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        Bundle messageBundle = MessageBundleMapper.messageToBundle(message);
        Class[] classes = propertyHelper().findClasses(MobileMessagingChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES);
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

    public boolean isActivated() {
        return propertyHelper().findBoolean(MobileMessagingChatProperty.IN_APP_CHAT_ACTIVATED);
    }

    // endregion
}
