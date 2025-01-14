package org.infobip.mobile.messaging.chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.chat.core.InAppChatBroadcasterImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatScreenImpl;
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy;
import org.infobip.mobile.messaging.chat.core.SessionStorage;
import org.infobip.mobile.messaging.chat.mobileapi.InAppChatSynchronizer;
import org.infobip.mobile.messaging.chat.mobileapi.LivechatRegistrationChecker;
import org.infobip.mobile.messaging.chat.models.ContextualData;
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils;
import org.infobip.mobile.messaging.chat.view.InAppChatActivity;
import org.infobip.mobile.messaging.chat.view.InAppChatEventsListener;
import org.infobip.mobile.messaging.chat.view.InAppChatFragment;
import org.infobip.mobile.messaging.chat.view.InAppChatView;
import org.infobip.mobile.messaging.chat.view.InAppChatWebView;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatTheme;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.Locale;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.TaskStackBuilder;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


public class InAppChatImpl extends InAppChat implements MessageHandlerModule {

    private final static String IN_APP_CHAT_FRAGMENT_TAG = InAppChatFragment.class.getName();
    private final static String TAG = "InAppChat";
    @SuppressLint("StaticFieldLeak")
    private static InAppChatImpl instance;
    private static MobileMessagingCore mmCore;
    private Context context;
    private AndroidBroadcaster coreBroadcaster;
    private InAppChatBroadcasterImpl inAppChatBroadcaster;
    private InAppChatScreenImpl inAppChatScreen;
    private PropertyHelper propertyHelper;
    private InAppChatWebView webView;
    private MobileApiResourceProvider mobileApiResourceProvider;
    private InAppChatSynchronizer inAppChatSynchronizer;
    private LivechatRegistrationChecker lcRegIgChecker;
    private InAppChatFragment inAppChatWVFragment;

    //region MessageHandlerModule
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
        int unreadChatMessageCount = propertyHelper().findInt(MobileMessagingChatProperty.UNREAD_CHAT_MESSAGES_COUNT) + 1;
        propertyHelper().saveInt(MobileMessagingChatProperty.UNREAD_CHAT_MESSAGES_COUNT, unreadChatMessageCount);
        inAppChatBroadcaster().unreadMessagesCounterUpdated(unreadChatMessageCount);
        coreBroadcaster().messageReceived(message);
        if (!isChatWidgetOnForeground() && !message.isSilent()) {
            MobileMessagingCore.getInstance(context).getNotificationHandler().displayNotification(message);
        }
        MobileMessagingLogger.d(TAG, "Message with id: " + message.getMessageId() + " will be handled by inAppChat MessageHandler");
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

        //InAppChatView is visible
        return isInAppChatViewPresent(activity);
    }

    private boolean isInAppChatViewPresent(Activity activity) {
        View rootView = activity.findViewById(android.R.id.content).getRootView();
        if (rootView instanceof ViewGroup) {
            View inAppChatView = findViewByType((ViewGroup) rootView, InAppChatView.class);
            return inAppChatView != null && inAppChatView.getVisibility() == View.VISIBLE;
        } else {
            return false;
        }
    }

    private View findViewByType(ViewGroup viewGroup, Class<?> type) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childView = viewGroup.getChildAt(i);

            if (type.isInstance(childView)) {
                return childView;
            } else if (childView instanceof ViewGroup) {
                View foundView = findViewByType((ViewGroup) childView, type);
                if (foundView != null) {
                    return foundView;
                }
            }
        }
        return null;
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
        if (stackBuilder != null && stackBuilder.getIntentCount() > 0) {
            stackBuilder.startActivities();
        }
    }

    @Nullable
    private TaskStackBuilder stackBuilderForNotificationTap(Message message) {
        Class<?>[] classes = propertyHelper().findClasses(MobileMessagingChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES);
        if (classes != null) {
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            Bundle messageBundle = MessageBundleMapper.messageToBundle(message);
            for (Class<?> cls : classes) {
                stackBuilder.addNextIntent(
                        new Intent(context, cls)
                                .setAction(Event.NOTIFICATION_TAPPED.getKey())
                                .putExtras(messageBundle)
                );
            }
            return stackBuilder;
        }
        return null;
    }

    @Override
    public void applicationInForeground() {
        performSyncActions();
    }

    // must be done on separate thread if it's not invoked by UI thread
    private void cleanupWidgetData() {
        sessionStorage().setConfigSyncResult(null);
        try {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                webView().clearHistory();
                webView().clearCache(true);
                MobileMessagingLogger.d(TAG, "Deleted local widget history");
            });
        } catch (Exception e) {
            MobileMessagingLogger.w(TAG, "Failed to delete local widget history due to " + e.getMessage());
        }
    }

    @Override
    public void depersonalize() {
        cleanupWidgetData();
        resetMessageCounter();
    }

    @Override
    public void performSyncActions() {
        Result<WidgetInfo, MobileMessagingError> syncResult = sessionStorage().getConfigSyncResult();
        if (isActivated() && (syncResult == null || !syncResult.isSuccess())) {
            inAppChatSynchronizer().getWidgetConfiguration(new MobileMessaging.ResultListener<WidgetInfo>() {
                @Override
                public void onResult(Result<WidgetInfo, MobileMessagingError> result) {
                    sessionStorage().setConfigSyncResult(result);
                    if (result.isSuccess()) {
                        syncLivechatRegistrationId(result.getData());
                    }
                }
            });
        }
        else {
            MobileMessagingLogger.i(TAG, "Widget sync skipped. In-app chat is not activated or widget configuration is already synced");
        }
    }

    private void syncLivechatRegistrationId(WidgetInfo widgetInfo) {
        livechatRegistrationIdChecker().sync(
                widgetInfo.getId(),
                mobileMessagingCore().getPushRegistrationId(),
                widgetInfo.isCallsEnabled()
        );
    }
    //endregion

    //region InAppChat public functions
    @NonNull
    public static InAppChat getInstance(Context context) {
        if (instance == null) {
            mmCore = MobileMessagingCore.getInstance(context);
            instance = mmCore.getMessageHandlerModule(InAppChatImpl.class);
        }
        return instance;
    }

    @Override
    public void activate() {
        propertyHelper().saveBoolean(MobileMessagingChatProperty.IN_APP_CHAT_ACTIVATED, true);
    }

    @Override
    @NonNull
    public InAppChatScreenImpl inAppChatScreen() {
        if (inAppChatScreen == null) {
            inAppChatScreen = new InAppChatScreenImpl(context);
        }
        if (!isActivated()) {
            MobileMessagingLogger.e(TAG, "In-app chat wasn't activated, call activate()");
        }
        return inAppChatScreen;
    }

    @Override
    public void setActivitiesToStartOnMessageTap(Class<?>... activityClasses) {
        propertyHelper().saveClasses(MobileMessagingChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES, activityClasses);
    }

    @Override
    public void cleanup() {
        mobileApiResourceProvider = null;
        inAppChatSynchronizer = null;
        lcRegIgChecker = null;
        sessionStorage().clean();
        cleanupWidgetData();
        propertyHelper().remove(MobileMessagingChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTITHREAD);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTICHANNEL_CONVERSATION);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_CALLS_ENABLED);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_ACTIVATED);
        propertyHelper().remove(MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE);
        PreferenceHelper.remove(context, MobileMessagingProperty.DEFAULT_IN_APP_CHAT_PUSH_TITLE);
        PreferenceHelper.remove(context, MobileMessagingProperty.DEFAULT_IN_APP_CHAT_PUSH_BODY);
        resetMessageCounter();
    }

    public void showInAppChatFragment(FragmentManager fragmentManager, int containerId) {
        if (fragmentManager != null) {
            if (inAppChatWVFragment == null) inAppChatWVFragment = new InAppChatFragment();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            Fragment fragmentByTag = fragmentManager.findFragmentByTag(IN_APP_CHAT_FRAGMENT_TAG);
            //on any configuration change activity is recreated -> new fragment manager instance -> show() does nothing
            if (areFragmentsEquals(fragmentByTag, inAppChatWVFragment)) {
                fragmentTransaction.show(inAppChatWVFragment);
            } else {
                fragmentTransaction.add(containerId, inAppChatWVFragment, IN_APP_CHAT_FRAGMENT_TAG);
            }
            fragmentTransaction.commit();
        }
    }

    public void hideInAppChatFragment(FragmentManager fragmentManager) {
        hideInAppChatFragment(fragmentManager, false);
    }

    @Override
    public void hideInAppChatFragment(FragmentManager fragmentManager, Boolean disconnectChat) {
        if (fragmentManager != null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (inAppChatWVFragment != null) {
                inAppChatWVFragment.setDisconnectChatWhenHidden(disconnectChat);
                fragmentTransaction.hide(inAppChatWVFragment);
            }
            //on any configuration change activity is recreated -> new fragment manager instance -> remove "old" fragment found by tag
            Fragment fragmentByTag = fragmentManager.findFragmentByTag(IN_APP_CHAT_FRAGMENT_TAG);
            if (fragmentByTag != null && !areFragmentsEquals(fragmentByTag, inAppChatWVFragment)) {
                fragmentTransaction.remove(fragmentByTag);
            }
            fragmentTransaction.commit();
        }
    }

    private boolean areFragmentsEquals(Fragment f1, Fragment f2) {
        return f1 != null && f2 != null && f1.hashCode() == f2.hashCode();
    }

    @Override
    public void resetMessageCounter() {
        MobileMessagingLogger.d(TAG, "Resetting unread message counter to 0");
        propertyHelper().remove(MobileMessagingChatProperty.UNREAD_CHAT_MESSAGES_COUNT);
        inAppChatBroadcaster().unreadMessagesCounterUpdated(0);
    }

    @Override
    public int getMessageCounter() {
        return propertyHelper().findInt(MobileMessagingChatProperty.UNREAD_CHAT_MESSAGES_COUNT);
    }

    @Override
    public void setLanguage(String language) {
        setLanguage(language, new MobileMessaging.ResultListener<String>() {
            @Override
            public void onResult(Result<String, MobileMessagingError> result) {
                if (!result.isSuccess()) {
                    MobileMessagingLogger.e(TAG, "Set language error: " + result.getError().getMessage());
                }
            }
        });
    }

    @Override
    public void setLanguage(String language, MobileMessaging.ResultListener<String> resultListener) {
        try {
            LocalizationUtils localizationUtils = LocalizationUtils.getInstance(context);
            Locale locale = localizationUtils.localeFromString(language);
            String appliedLocale = locale.toString();
            propertyHelper().saveString(MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE, appliedLocale);
            if (inAppChatWVFragment != null) {
                inAppChatWVFragment.setLanguage(locale);
            }
            if (resultListener != null) {
                resultListener.onResult(new Result<>(appliedLocale));
            }
        } catch (Throwable t) {
            if (resultListener != null) {
                resultListener.onResult(new Result<>(MobileMessagingError.createFrom(t)));
            }
        }
    }

    @Override
    public void sendContextualData(@Nullable String data) {
        sendContextualData(data, MultithreadStrategy.ACTIVE, new MobileMessaging.ResultListener<Void>() {
            @Override
            public void onResult(Result<Void, MobileMessagingError> result) {
                if (!result.isSuccess()) {
                    MobileMessagingLogger.e(TAG, "Send contextual data error: " + result.getError().getMessage());
                }
            }
        });
    }

    @Override
    public void sendContextualData(@Nullable String data, @Nullable Boolean allMultiThreadStrategy) {
        sendContextualData(data, allMultiThreadStrategy, new MobileMessaging.ResultListener<Void>() {
            @Override
            public void onResult(Result<Void, MobileMessagingError> result) {
                if (!result.isSuccess()) {
                    MobileMessagingLogger.e(TAG, "Send contextual data error: " + result.getError().getMessage());
                }
            }
        });
    }

    @Override
    public void sendContextualData(@Nullable String data, @Nullable MultithreadStrategy flag) {
        sendContextualData(data, flag, new MobileMessaging.ResultListener<Void>() {
            @Override
            public void onResult(Result<Void, MobileMessagingError> result) {
                if (!result.isSuccess()) {
                    MobileMessagingLogger.e(TAG,"Send contextual data error: " + result.getError().getMessage());
                }
            }
        });
    }

    @Override
    public void sendContextualData(@Nullable String data, @Nullable Boolean allMultiThreadStrategy, @Nullable MobileMessaging.ResultListener<Void> resultListener) {
        MultithreadStrategy flag = Boolean.TRUE.equals(allMultiThreadStrategy) ? MultithreadStrategy.ALL : MultithreadStrategy.ACTIVE;
        sendContextualData(data, flag, resultListener);
    }

    @Override
    public void sendContextualData(@Nullable String data, @Nullable MultithreadStrategy flag, @Nullable MobileMessaging.ResultListener<Void> resultListener) {
        Result<Void, MobileMessagingError> result = null;
        try {
            if (data == null || data.isEmpty()) {
                sessionStorage().setContextualData(null);
                result = new Result<>(MobileMessagingError.createFrom(new IllegalArgumentException("Could not send contextual data. Data is null or empty.")));
            } else if (flag == null) {
                sessionStorage().setContextualData(null);
                result = new Result<>(MobileMessagingError.createFrom(new IllegalArgumentException("Could not send contextual data. Strategy flag is null.")));
            } else if (inAppChatWVFragment != null) {
                inAppChatWVFragment.sendContextualData(data, flag);
                result = new Result<>(null);
            } else {
                sessionStorage().setContextualData(new ContextualData(data, flag));
                MobileMessagingLogger.d(TAG,"Contextual data is stored, will be sent once chat is loaded.");
                result = new Result<>(null);
            }
        } catch (Throwable throwable) {
            result = new Result<>(MobileMessagingError.createFrom(throwable));
        } finally {
            if (result != null) {
                if (resultListener != null) {
                    resultListener.onResult(result);
                } else if (!result.isSuccess()) {
                    MobileMessagingLogger.e(TAG,"sendContextualData() failed: " + result.getError().getMessage());
                }
            }
        }
    }

    @Override
    public void setJwtProvider(InAppChat.JwtProvider jwtProvider) {
        sessionStorage().setJwtProvider(jwtProvider);
    }

    @Override
    @Nullable
    public InAppChat.JwtProvider getJwtProvider() {
        return sessionStorage().getJwtProvider();
    }

    @Override
    public void showThreadsList() {
        if (inAppChatWVFragment != null) {
            inAppChatWVFragment.showThreadList();
        } else {
            MobileMessagingLogger.e(TAG, "Function showThreadsList() skipped, InAppChatFragment has not been shown yet.");
        }
    }

    @Override
    public void setTheme(@Nullable InAppChatTheme theme) {
        sessionStorage().setTheme(theme);
    }

    @Override
    @Nullable
    public InAppChatTheme getTheme() {
        return sessionStorage().getTheme();
    }

    @Override
    public void setWidgetTheme(@Nullable String widgetThemeName) {
        sessionStorage().setWidgetTheme(widgetThemeName);
        if (inAppChatWVFragment != null && widgetThemeName != null) {
            inAppChatWVFragment.setWidgetTheme(widgetThemeName);
        }
    }

    @Override
    @Nullable
    public String getWidgetTheme() {
        return sessionStorage().getWidgetTheme();
    }

    @Override
    public String getDomain() {
        return sessionStorage().getDomain();
    }

    @Override
    public void setDomain(String domain) {
        sessionStorage().setDomain(domain);
    }

    @Override
    public void setChatPushTitle(@Nullable String title) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.DEFAULT_IN_APP_CHAT_PUSH_TITLE, title);
    }

    @Override
    public void setChatPushBody(@Nullable String body) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.DEFAULT_IN_APP_CHAT_PUSH_BODY, body);
    }

    @Override
    public void setEventsListener(InAppChatEventsListener inAppChatEventsListener) {
        sessionStorage().setInAppChatEventsListener(inAppChatEventsListener);
    }

    @Override
    public InAppChatEventsListener getEventsListener() {
        return sessionStorage().getInAppChatEventsListener();
    }
    //endregion

    // region private functions
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

    synchronized private MobileMessagingCore mobileMessagingCore() {
        if (mmCore == null) {
            mmCore = MobileMessagingCore.getInstance(context);
        }
        return mmCore;
    }

    synchronized private LivechatRegistrationChecker livechatRegistrationIdChecker() {
        if (lcRegIgChecker == null) {
            lcRegIgChecker = new LivechatRegistrationChecker(
                    context,
                    mobileMessagingCore(),
                    propertyHelper(),
                    inAppChatBroadcaster(),
                    mobileApiResourceProvider().getMobileApiAppInstance(context),
                    Executors.newSingleThreadExecutor()
            );
        }
        return lcRegIgChecker;
    }

    synchronized private SessionStorage sessionStorage() {
        return SessionStorage.INSTANCE;
    }

    private boolean isActivated() {
        return propertyHelper().findBoolean(MobileMessagingChatProperty.IN_APP_CHAT_ACTIVATED);
    }
    //endregion
}
