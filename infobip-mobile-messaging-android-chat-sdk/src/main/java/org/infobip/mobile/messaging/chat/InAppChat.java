package org.infobip.mobile.messaging.chat;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.chat.core.InAppChatNotificationInteractionHandler;
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApi;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetLanguage;
import org.infobip.mobile.messaging.chat.view.InAppChatEventsListener;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatTheme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

/**
 * Main interface for in-app chat communication
 */
@SuppressWarnings("unused")
public abstract class InAppChat {

    /**
     * Provides JSON Web Token (JWT), to give in-app chat ability to authenticate.
     *
     * @deprecated Use {@link org.infobip.mobile.messaging.chat.core.JwtProvider} instead
     */
    @Deprecated
    public interface JwtProvider {
        /**
         * Provides JSON Web Token (JWT), to give in-app chat ability to authenticate.
         * Function can be triggered multiple times during in-app chat lifetime, due to various events like screen orientation change, internet re-connection.
         * If you can ensure JWT expiration time is more than in-app chat lifetime, you can return cached token, otherwise
         * <b>it is important to provide fresh new token for each invocation.</b>
         *
         * @return JWT
         */
        @Nullable
        String provideJwt();
    }

    /**
     * Returns instance of chat api
     *
     * @param context android context
     * @return instance of chat api
     */
    @NonNull
    public synchronized static InAppChat getInstance(Context context) {
        return InAppChatImpl.getInstance(context);
    }

    /**
     * Activates In-app chat service, what ensures all chat necessary configurations are synced with the server.
     */
    public abstract void activate();

    /**
     * Checks whether the in-app chat is ready to be shown to the user.
     * <p>
     * In-app chat is considered ready when the widget configuration has been synced and
     * Infobip's unique push registration ID has been issued.
     * Widget configuration sync requires In-app chat service to be activated by calling {@code activate()} method.
     * </p>
     * @return {@code true} if the in-app chat is ready to be presented to the user,
     *         {@code false} otherwise.
     * @see org.infobip.mobile.messaging.chat.core.InAppChatEvent#IN_APP_CHAT_AVAILABILITY_UPDATED
     */
    public abstract boolean isChatAvailable();

    /**
     * Creates in-app chat screen
     *
     * @return chat view object
     * @see InAppChatScreen#show()
     */
    @NonNull
    public abstract InAppChatScreen inAppChatScreen();

    /**
     * Sets which activities to start when user taps on chat notification. Last one in array will be shown, others will be put to task stack.
     * <p>Library will also provide appropriate message together with intent, use following code to retrieve the message:</p>
     * * <pre>
     * {@code
     * Message message = Message.createFrom(intent);
     * }
     * </pre>
     *
     * @param activityClasses array of activities to put into task stack when message is tapped
     * @deprecated Use {@link InAppChat#setNotificationInteractionHandler(InAppChatNotificationInteractionHandler)} instead
     */
    @Deprecated
    public abstract void setActivitiesToStartOnMessageTap(Class<?>... activityClasses);

    /**
     * Cleans up all in-app chat data.
     * <p>NOTE: There is no need to invoke this method manually as library manages web view data</p>
     */
    public abstract void cleanup();

    /**
     * Adds in-app chat Fragment to the Activity or shows it if it was already added and hidden.
     *
     * @param fragmentManager manager to make interactions with Fragment
     * @param containerId     identifier of the container in-app chat Fragment is to be placed in
     * @deprecated <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat#assisted-approach">Assisted approach</a> to show fragment is deprecated, you are
     * supposed create and manage {@link org.infobip.mobile.messaging.chat.view.InAppChatFragment} on your own, so you have
     * <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat#full-ownership">full ownership</a>.
     */
    @Deprecated
    public abstract void showInAppChatFragment(FragmentManager fragmentManager, int containerId);

    /**
     * Hides in-app chat Fragment, so that all views, especially in-app chat webView,
     * stays in memory and chat connection is active while fragment is hidden.
     *
     * @param fragmentManager manager to make interactions with Fragment
     * @see InAppChat#hideInAppChatFragment(FragmentManager, Boolean)
     * @deprecated <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat#assisted-approach">Assisted approach</a> to show fragment is deprecated, you are
     * supposed create and manage {@link org.infobip.mobile.messaging.chat.view.InAppChatFragment} on your own, so you have
     * <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat#full-ownership">full ownership</a>.
     */
    @Deprecated
    public abstract void hideInAppChatFragment(FragmentManager fragmentManager);

    /**
     * Hides in-app chat Fragment, so that all views, especially in-app chat webView, stays in memory.
     * You can control whether chat connection stays active while fragment is hidden.
     *
     * @param fragmentManager          manager to make interactions with Fragment
     * @param disconnectChatWhenHidden if true disconnects chat connection when fragment is hidden, otherwise chat connection stays active
     * @apiNote By chat connection you can control push notifications.
     * Push notifications are active only when chat connection is not active.
     * Disconnect chat if you want to receive push notifications while fragment is hidden.
     * Chat connection is re-established when {@link InAppChat#showInAppChatFragment(FragmentManager, int)} is called.
     * @see InAppChat#hideInAppChatFragment(FragmentManager)
     * @deprecated <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat#assisted-approach">Assisted approach</a> to show fragment is deprecated, you are
     * supposed create and manage {@link org.infobip.mobile.messaging.chat.view.InAppChatFragment} on your own, so you have
     * <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat#full-ownership">full ownership</a>.
     */
    @Deprecated
    public abstract void hideInAppChatFragment(FragmentManager fragmentManager, Boolean disconnectChatWhenHidden);

    /**
     * Resets current unread chat push message counter to zero. MM SDK automatically resets the counter when InAppChatFragment/Activity appears on screen.
     */
    public abstract void resetMessageCounter();

    /**
     * Returns current unread chat push message counter.
     */
    public abstract int getMessageCounter();

    /**
     * Set an in-app chat's language
     * @param language in locale format e.g.: en-US
     * @deprecated Use {@link InAppChat#setLanguage(LivechatWidgetLanguage)} instead
     */
    @Deprecated
    public abstract void setLanguage(String language);

    /**
     * Set an in-app chat's language
     *
     * @param language       in locale format e.g.: en-US
     * @param resultListener listener to report the result on
     * @deprecated Use {@link InAppChat#setLanguage(LivechatWidgetLanguage, MobileMessaging.ResultListener)} instead
     * @see MobileMessaging.ResultListener
     */
    @Deprecated
    public abstract void setLanguage(String language, MobileMessaging.ResultListener<String> resultListener);

    /**
     * Set an in-app chat's language
     *
     * @param language language is used by livechat widget and in-app chat native parts
     * @see MobileMessaging.ResultListener
     */
    public abstract void setLanguage(@NonNull LivechatWidgetLanguage language);

    /**
     * Set an in-app chat's language
     *
     * @param language       language is used by livechat widget and in-app chat native parts
     * @param resultListener listener to report the result on
     * @see MobileMessaging.ResultListener
     */
    public abstract void setLanguage(@NonNull LivechatWidgetLanguage language, @Nullable MobileMessaging.ResultListener<LivechatWidgetLanguage> resultListener);

    /**
     * Returns current in-app chat language
     *
     * @return current in-app chat language or default {@code LivechatWidgetLanguage.ENGLISH}
     */
    @NonNull
    public abstract LivechatWidgetLanguage getLanguage();

    /**
     * Set contextual data of the livechat widget.
     * If the function is called when the chat is loaded,
     * data will be sent immediately, otherwise they will be sent to the chat once it is loaded.
     * Every function invocation will overwrite the previous contextual data.
     *
     * @param data                   contextual data in the form of JSON string
     * @param allMultiThreadStrategy multithread strategy flag, true -> ALL, false -> ACTIVE
     * @deprecated Use {@link InAppChat#sendContextualData(String, MultithreadStrategy, MobileMessaging.ResultListener)} instead
     */
    @Deprecated
    public abstract void sendContextualData(@Nullable String data, @Nullable Boolean allMultiThreadStrategy);

    /**
     * Set contextual data of the livechat widget.
     * If the function is called when the chat is loaded,
     * data will be sent immediately, otherwise they will be sent to the chat once it is loaded.
     * Every function invocation will overwrite the previous contextual data.
     *
     * @param data contextual data in the form of JSON string
     * @param flag multithread strategy {@code MultithreadStrategy}
     */
    public abstract void sendContextualData(@Nullable String data, @Nullable MultithreadStrategy flag);

    /**
     * Set contextual data of the livechat widget with false ({@code MultithreadStrategy.ACTIVE}) value for multithread strategy.
     * If the function is called when the chat is loaded,
     * data will be sent immediately, otherwise they will be sent to the chat once it is loaded.
     * Every function invocation will overwrite the previous contextual data.
     *
     * @param data contextual data in the form of JSON string
     */
    public abstract void sendContextualData(@Nullable String data);

    /**
     * Set contextual data of the livechat widget.
     * If the function is called when the chat is loaded,
     * data will be sent immediately, otherwise they will be sent to the chat once it is loaded.
     * Every function invocation will overwrite the previous contextual data.
     *
     * @param data                   contextual data in the form of JSON string
     * @param allMultiThreadStrategy multithread strategy flag, true -> ALL, false -> ACTIVE
     * @param resultListener         listener to report the result on
     * @see MobileMessaging.ResultListener
     * @deprecated Use {@link InAppChat#sendContextualData(String, MultithreadStrategy, MobileMessaging.ResultListener)} instead
     */
    @Deprecated
    public abstract void sendContextualData(@Nullable String data, @Nullable Boolean allMultiThreadStrategy, @Nullable MobileMessaging.ResultListener<Void> resultListener);

    /**
     * Set contextual data of the livechat widget.
     * If the function is called when the chat is loaded,
     * data will be sent immediately, otherwise they will be sent to the chat once it is loaded.
     * Every function invocation will overwrite the previous contextual data.
     *
     * @param data contextual data in the form of JSON string
     * @param flag multithread strategy {@code MultithreadStrategy}
     * @param resultListener listener to report the result on
     * @see MobileMessaging.ResultListener
     */
    public abstract void sendContextualData(@Nullable String data, @Nullable MultithreadStrategy flag, @Nullable MobileMessaging.ResultListener<Void> resultListener);

    /**
     * Set {@link JwtProvider} to give in-app chat ability to authenticate.
     *
     * @deprecated Use {@link InAppChat#setWidgetJwtProvider(org.infobip.mobile.messaging.chat.core.JwtProvider)} instead
     *
     * @param jwtProvider provider instance
     * @see JwtProvider
     */
    @Deprecated
    public abstract void setJwtProvider(InAppChat.JwtProvider jwtProvider);

    /**
     * Returns instance of {@link JwtProvider}
     *
     * @deprecated Use {@link JwtProvider InAppChat#getJwtProvider()} instead
     *
     * @return instance of {@link JwtProvider}
     * @see JwtProvider
     */
    @Nullable
    @Deprecated
    public abstract InAppChat.JwtProvider getJwtProvider();

    /**
     * Set {@link JwtProvider} to give in-app chat ability to authenticate.
     *
     * @param jwtProvider provider instance
     * @see JwtProvider
     */
    public abstract void setWidgetJwtProvider(org.infobip.mobile.messaging.chat.core.JwtProvider jwtProvider);

    /**
     * Returns instance of {@link JwtProvider}
     *
     * @return instance of {@link JwtProvider}
     * @see JwtProvider
     */
    @Nullable
    public abstract org.infobip.mobile.messaging.chat.core.JwtProvider getWidgetJwtProvider();

    /**
     * Navigates to THREAD_LIST view in multithread widget if in-app chat is shown as Fragment.
     *
     * @see org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView
     * @deprecated <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat#assisted-approach">Assisted approach</a> to show fragment is deprecated, you are
     * supposed create and manage {@link org.infobip.mobile.messaging.chat.view.InAppChatFragment} on your own, so you have
     * <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat#full-ownership">full ownership</a>
     * and can use {@code InAppChatFragment.showThreadList()}.
     */
    @Deprecated
    public abstract void showThreadsList();

    /**
     * Set theme, it is alternative to `IB_AppTheme.Chat` defined in xml, if set xml values are replaced.
     * It allows to customise native views of In-app chat.
     *
     * @param theme data object holding all style attributes
     */
    public abstract void setTheme(@Nullable InAppChatTheme theme);

    /**
     * Get current theme. Theme is alternative to `IB_AppTheme.Chat` defined in xml.
     *
     * @return theme data object holding all style attributes
     */
    @Nullable
    public abstract InAppChatTheme getTheme();

    /**
     * Sets a livechat widget's theme.
     * You can define widget themes in <a href="https://portal.infobip.com/apps/livechat/widgets">Live chat widget setup page</a> in Infobip Portal, section `Advanced customization`.
     * Please check widget <a href="https://www.infobip.com/docs/live-chat/widget-customization">documentation</a> for more details.
     *
     * <p>You must set widget theme before chat is shown. Function does not change theme once chat is already presented - in runtime.</p>
     *
     * @param widgetThemeName unique theme name, empty or blank value is ignored
     */
    public abstract void setWidgetTheme(@Nullable String widgetThemeName);

    /**
     * Get current livechat widget theme.
     *
     * @return applied theme name of livechat widget
     */
    @Nullable
    public abstract String getWidgetTheme();

    /**
     * Set the domain of the livechat widget.
     *
     * @param domain domain of the widget
     */
    public abstract void setDomain(String domain);

    /**
     * Get current domain of the livechat widget.
     *
     * @return domain of the widget
     */
    @Nullable
    public abstract String getDomain();

    /**
     * Set custom title for in-app chat notifications
     *
     * @param title custom title to be set for notifications
     */
    public abstract void setChatPushTitle(@Nullable String title);

    /**
     * Get current custom in-app chat notifications title
     *
     * @return custom title for chat notifications
     */
    @Nullable
    public abstract String getChatPushTitle();

    /**
     * Set custom body for in-app chat notifications
     *
     * @param body custom body to be set for notifications
     */
    public abstract void setChatPushBody(@Nullable String body);

    /**
     * Get current custom in-app chat notifications body
     *
     * @return custom body for chat notifications
     */
    @Nullable
    public abstract String getChatPushBody();

    /**
     * Set {@link InAppChatEventsListener} to listen for various in-app chat events.
     * It allows you to observer chat related events when you show in-app chat using {@code InAppChat.inAppChatScreen().show()} or
     * {@code InAppChat.showInAppChatFragment(fragmentManager, containerId)} functions.
     *
     * @param inAppChatEventsListener listener to report the events on
     */
    public abstract void setEventsListener(InAppChatEventsListener inAppChatEventsListener);

    /**
     * Get current {@link InAppChatEventsListener}.
     *
     * @return {@link InAppChatEventsListener} listener to report the events on
     * @see org.infobip.mobile.messaging.chat.view.InAppChatEventsListener
     */
    public abstract InAppChatEventsListener getEventsListener();

    /**
     * Get instance of {@link LivechatWidgetApi} to interact with livechat widget
     * without need to show either {@code InAppChatView} or {@code InAppChatFragment} in UI.
     *
     * Before using this API, you must ensure that the In-app chat service is activated by calling {@link InAppChat#activate()}.
     * You can also check if the {@code LivechatWidgetApi} is ready to use by {@link InAppChat#isChatAvailable()}
     * or {@code InAppChatEvent.IN_APP_CHAT_AVAILABILITY_UPDATED} broadcast event.
     *
     * @return {@link LivechatWidgetApi} instance
     * @see org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApi
     */
    public abstract LivechatWidgetApi getLivechatWidgetApi();

    /**
     * Sets a custom {@link InAppChatNotificationInteractionHandler} to handle user interactions
     * with in-app chat-related notifications.
     * <p><strong>If not set, the default tap handler will be used.</strong></p>
     *
     * <p>The handler is triggered when the user interacts with any of the following:</p>
     * <ul>
     *     <li>An in-app chat message push notification</li>
     *     <li>A <a href="https://www.infobip.com/docs/mobile-app-messaging/push-notification">push notification</a> with the "Open chatbot in LiveChat" tap action</li>
     *     <li>A <a href="https://www.infobip.com/docs/mobile-app-messaging/push-notification#mirror-push-notifications">mirror push notification</a> with the primary button action "Open chatbot in LiveChat"</li>
     *     <li>An <a href="https://www.infobip.com/docs/mobile-app-messaging/send-in-app-message#create-in-app-message"> in-app message</a> with the "Open chatbot in LiveChat" tap action</li>
     * </ul>
     *
     * @param notificationTapHandler the handler to process in-app chat notification taps
     */
    public abstract void setNotificationInteractionHandler(@Nullable InAppChatNotificationInteractionHandler notificationTapHandler);

    /**
     * Returns the currently set {@link InAppChatNotificationInteractionHandler}, which handles user interactions with in-app chat notifications.
     *
     * @return the {@link InAppChatNotificationInteractionHandler} currently handling in-app chat notification taps, or {@code null} if none is set
     * @see org.infobip.mobile.messaging.chat.core.InAppChatNotificationInteractionHandler
     */
    @Nullable
    public abstract InAppChatNotificationInteractionHandler getNotificationInteractionHandler();

    /**
     * Returns the default {@link InAppChatNotificationInteractionHandler}.
     *
     * <p>
     * When the user taps an in-app chat message push notification, the default handler opens the callback activity
     * defined in {@link org.infobip.mobile.messaging.NotificationSettings#getCallbackActivity()}.
     * It may also start activities defined in {@link InAppChat#setActivitiesToStartOnMessageTap(Class[])},
     * although this method is deprecated and will be removed in a future release, along with this behavior.
     * </p>
     *
     * <p>
     * For any push notification or in-app message with the "Open chatbot in LiveChat" tap action,
     * the default handler opens {@link org.infobip.mobile.messaging.chat.view.InAppChatActivity}.
     * </p>
     *
     * @return the default {@link InAppChatNotificationInteractionHandler} used to handle in-app chat notification taps
     * @see org.infobip.mobile.messaging.chat.core.InAppChatNotificationInteractionHandler
     * @see org.infobip.mobile.messaging.NotificationSettings#getCallbackActivity()
     */
    @NonNull
    public abstract InAppChatNotificationInteractionHandler getDefaultNotificationInteractionHandler();

}
