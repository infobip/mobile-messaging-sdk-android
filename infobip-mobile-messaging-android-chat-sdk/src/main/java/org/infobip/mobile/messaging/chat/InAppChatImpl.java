package org.infobip.mobile.messaging.chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.TaskStackBuilder;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.chat.core.InAppChatBroadcasterImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatMultiThreadFlag;
import org.infobip.mobile.messaging.chat.core.InAppChatViewImpl;
import org.infobip.mobile.messaging.chat.mobileapi.InAppChatSynchronizer;
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils;
import org.infobip.mobile.messaging.chat.view.InAppChatActivity;
import org.infobip.mobile.messaging.chat.view.InAppChatFragment;
import org.infobip.mobile.messaging.chat.view.InAppChatWebView;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;

import java.util.Locale;


public class InAppChatImpl extends InAppChat implements MessageHandlerModule {

    @SuppressLint("StaticFieldLeak")
    private static InAppChatImpl instance;
    private Context context;
    private AndroidBroadcaster coreBroadcaster;
    private InAppChatBroadcasterImpl inAppChatBroadcaster;
    private InAppChatViewImpl inAppChatView;
    private PropertyHelper propertyHelper;
    private InAppChatWebView webView;
    private MobileApiResourceProvider mobileApiResourceProvider;
    private InAppChatSynchronizer inAppChatSynchronizer;
    private static Result<WidgetInfo, MobileMessagingError> chatWidgetConfigSyncResult = null;
    private static Boolean isWebViewCacheCleaned = false;
    private JwtProvider jwtProvider = null;

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

    public boolean isActivated() {
        return propertyHelper().findBoolean(MobileMessagingChatProperty.IN_APP_CHAT_ACTIVATED);
    }

    @Override
    public boolean handleMessage(Message message) {
        if (!message.isChatMessage()) {
            return false;
        }
        int unreadChatMessageCount = propertyHelper().findInt(MobileMessagingChatProperty.UNREAD_CHAT_MESSAGES_COUNT) + 1;
        propertyHelper().saveInt(MobileMessagingChatProperty.UNREAD_CHAT_MESSAGES_COUNT, unreadChatMessageCount);
        inAppChatBroadcaster().unreadMessagesCounterUpdated(unreadChatMessageCount);
        coreBroadcaster().messageReceived(message);
        if (!isChatWidgetOnForeground() && !message.isSilent()) {
            MobileMessagingCore.getInstance(context).getNotificationHandler().displayNotification(message);
        }
        MobileMessagingLogger.d("Message with id: " + message.getMessageId() + " will be handled by inAppChat MessageHandler");
        return true;
    }

    private boolean isChatWidgetOnForeground() {
        Activity activity = null;
        ActivityLifecycleMonitor activityLifecycleMonitor = MobileMessagingCore.getInstance(context).getActivityLifecycleMonitor();
        if (activityLifecycleMonitor != null) {
            activity = activityLifecycleMonitor.getForegroundActivity();
        }
        if (activity == null) return false;

        //InAppChatActivity is on foreground
        if (activity.getClass().toString().equals(InAppChatActivity.class.toString())) return true;

        if (activity instanceof AppCompatActivity) {
            //InAppChatFragment is visible and resumed
            Fragment inAppChatFragment = ((AppCompatActivity) activity).getSupportFragmentManager().findFragmentByTag(IN_APP_CHAT_FRAGMENT_TAG);
            return inAppChatFragment != null && inAppChatFragment.isVisible() && inAppChatFragment.isResumed();
        }
        return false;
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

    private void doCoreTappedActions(Message chatMessage) {
        TaskStackBuilder stackBuilder = stackBuilderForNotificationTap(chatMessage);
        if (stackBuilder.getIntentCount() != 0) {
            stackBuilder.startActivities();
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

    public static Result<WidgetInfo, MobileMessagingError> getChatWidgetConfigSyncResult() {
        return chatWidgetConfigSyncResult;
    }

    public static Boolean getIsWebViewCacheCleaned() {
        return isWebViewCacheCleaned;
    }

    public static void setIsWebViewCacheCleaned(Boolean webViewCacheCleaned) {
        isWebViewCacheCleaned = webViewCacheCleaned;
    }

    @Override
    public void sendContextualData(String data) {
        sendContextualData(data, false, new MobileMessaging.ResultListener<Void>() {
            @Override
            public void onResult(Result<Void, MobileMessagingError> result) {
                if (!result.isSuccess()) {
                    MobileMessagingLogger.e("Send contextual data error: " + result.getError().getMessage());
                }
            }
        });
    }

    @Override
    public void sendContextualData(String data, Boolean allMultiThreadStrategy) {
        sendContextualData(data, allMultiThreadStrategy, new MobileMessaging.ResultListener<Void>() {
            @Override
            public void onResult(Result<Void, MobileMessagingError> result) {
                if (!result.isSuccess()) {
                    MobileMessagingLogger.e("Send contextual data error: " + result.getError().getMessage());
                }
            }
        });
    }

    @Override
    public void sendContextualData(String data, Boolean allMultiThreadStrategy, MobileMessaging.ResultListener<Void> resultListener) {
        try {
            if (inAppChatWVFragment != null) {
                InAppChatMultiThreadFlag strategy = InAppChatMultiThreadFlag.ACTIVE;
                if (allMultiThreadStrategy) {
                    strategy = InAppChatMultiThreadFlag.ALL;
                }
                inAppChatWVFragment.sendContextualMetaData(data, strategy);
            }
            if (resultListener != null)
                resultListener.onResult(new Result<>(null));
        } catch (Throwable throwable) {
            MobileMessagingLogger.e("sendContextualData() failed", throwable);
            if (resultListener != null)
                resultListener.onResult(new Result<>(MobileMessagingError.createFrom(throwable)));
        }
    }

    @Override
    public void setJwtProvider(InAppChat.JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public InAppChat.JwtProvider getJwtProvider() {
        return jwtProvider;
    }

    @Override
    public void applicationInForeground() {
        performSyncActions();
    }

    @Override
    public void cleanup() {
        mobileApiResourceProvider = null;
        inAppChatSynchronizer = null;
        jwtProvider = null;
        cleanupWidgetData();
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE);
        resetMessageCounter();
    }

    // must be done on separate thread if it's not invoked by UI thread
    private void cleanupWidgetData() {
        chatWidgetConfigSyncResult = null;
        isWebViewCacheCleaned = true;
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
        resetMessageCounter();
    }

    @Override
    public void performSyncActions() {
        if (isActivated() && (chatWidgetConfigSyncResult == null || !chatWidgetConfigSyncResult.isSuccess())) {
            inAppChatSynchronizer().getWidgetConfiguration(new MobileMessaging.ResultListener<WidgetInfo>() {
                @Override
                public void onResult(Result<WidgetInfo, MobileMessagingError> result) {
                    chatWidgetConfigSyncResult = result;
                }
            });
        }
    }

    private InAppChatFragment inAppChatWVFragment;
    private final static String IN_APP_CHAT_FRAGMENT_TAG = InAppChatFragment.class.getName();

    public void showInAppChatFragment(FragmentManager fragmentManager, int containerId) {
        if (inAppChatWVFragment == null) inAppChatWVFragment = new InAppChatFragment();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (fragmentManager.findFragmentByTag(IN_APP_CHAT_FRAGMENT_TAG) == null) {
            fragmentTransaction.add(containerId, inAppChatWVFragment, IN_APP_CHAT_FRAGMENT_TAG);
        } else {
            fragmentTransaction.show(inAppChatWVFragment);
        }
        fragmentTransaction.commit();
    }

    public void hideInAppChatFragment(FragmentManager fragmentManager) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.hide(inAppChatWVFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void resetMessageCounter() {
        MobileMessagingLogger.d("Resetting unread message counter to 0");
        propertyHelper().remove(MobileMessagingChatProperty.UNREAD_CHAT_MESSAGES_COUNT);
        inAppChatBroadcaster().unreadMessagesCounterUpdated(0);
    }

    @Override
    public int getMessageCounter() {
        return propertyHelper().findInt(MobileMessagingChatProperty.UNREAD_CHAT_MESSAGES_COUNT);
    }

    @Override
    public void setLanguage(String language) {
        setLanguage(language, new MobileMessaging.ResultListener<Void>() {
            @Override
            public void onResult(Result<Void, MobileMessagingError> result) {
                if (!result.isSuccess()) {
                    MobileMessagingLogger.e("Set language error: " + result.getError().getMessage());
                }
            }
        });
    }

    @Override
    public void setLanguage(String language, MobileMessaging.ResultListener<Void> resultListener) {
        try {
            if (inAppChatWVFragment != null) {
                inAppChatWVFragment.setLanguage(language);
            }
            propertyHelper().saveString(MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE, language);

            Locale locale = LocalizationUtils.localeFromString(language);
            LocalizationUtils.getInstance(context).setLanguage(locale);
            if (resultListener != null) {
                resultListener.onResult(new Result<>(null));
            }
        } catch (Throwable t) {
            if (resultListener != null) {
                resultListener.onResult(new Result<>(MobileMessagingError.createFrom(t)));
            }
        }
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

    synchronized private InAppChatWebView webView() {
        if (webView == null) {
            webView = new InAppChatWebView(context);
        }
        return webView;
    }
    //endregion
}
