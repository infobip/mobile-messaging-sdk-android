package org.infobip.mobile.messaging.chat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatDarkMode;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatTheme;

/**
 * Main interface for in-app chat communication
 */
@SuppressWarnings("unused")
public abstract class InAppChat {

    /**
     * Provides JSON Web Token (JWT), to give in-app chat ability to authenticate.
     */
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
     * Activates in-app chat service.
     */
    public abstract void activate();

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
     */
    public abstract void setActivitiesToStartOnMessageTap(Class... activityClasses);

    /**
     * Cleans up all InAppChat data.
     * <p>NOTE: There is no need to invoke this method manually as library manages web view data</p>
     */
    public abstract void cleanup();

    /**
     * Adds in-app chat Fragment to the Activity or shows it if it was already added and hidden.
     * @param fragmentManager manager to make interactions with Fragment
     * @param containerId identifier of the container in-app chat Fragment is to be placed in
     */
    public abstract void showInAppChatFragment(FragmentManager fragmentManager, int containerId);

    /**
     * Hides in-app chat Fragment, so that all views, especially in-app chat webView,
     * stays in memory and chat connection is active while fragment is hidden.
     * @param fragmentManager manager to make interactions with Fragment
     * @see InAppChat#hideInAppChatFragment(FragmentManager, Boolean)
     */
    public abstract void hideInAppChatFragment(FragmentManager fragmentManager);

    /**
     * Hides in-app chat Fragment, so that all views, especially in-app chat webView, stays in memory.
     * You can control whether chat connection stays active while fragment is hidden.
     *
     * @apiNote By chat connection you can control push notifications.
     * Push notifications are active only when chat connection is not active.
     * Disconnect chat if you want to receive push notifications while fragment is hidden.
     * Chat connection is re-established when {@link InAppChat#showInAppChatFragment(FragmentManager, int)} is called.
     *
     * @param fragmentManager manager to make interactions with Fragment
     * @param disconnectChatWhenHidden if true disconnects chat connection when fragment is hidden, otherwise chat connection stays active
     * @see InAppChat#hideInAppChatFragment(FragmentManager)
     */
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
     * Set the language of the widget
     *
     * @param language in locale format e.g.: en-US
     */
    public abstract void setLanguage(String language);

    /**
     * Set the language of the widget
     *
     * @param language       in locale format e.g.: en-US
     * @param resultListener listener to report the result on
     * @see MobileMessaging.ResultListener
     */
    public abstract void setLanguage(String language, MobileMessaging.ResultListener<String> resultListener);

    /**
     * Set contextual data of the widget
     *
     * @param data                   contextual data in the form of JSON string
     * @param allMultiThreadStrategy multithread strategy flag, true -> ALL, false -> ACTIVE
     */
    public abstract void sendContextualData(String data, Boolean allMultiThreadStrategy);

    /**
     * Set contextual data of the widget with false (ACTIVE) value for multithread strategy
     *
     * @param data contextual data in the form of JSON string
     */
    public abstract void sendContextualData(String data);

    /**
     * Set contextual data of the widget
     *
     * @param data                   contextual data in the form of JSON string
     * @param allMultiThreadStrategy multithread strategy flag, true -> ALL, false -> ACTIVE
     * @param resultListener         listener to report the result on
     * @see MobileMessaging.ResultListener
     */
    public abstract void sendContextualData(String data, Boolean allMultiThreadStrategy, MobileMessaging.ResultListener<Void> resultListener);

    /**
     * Set {@link JwtProvider} to give in-app chat ability to authenticate.
     *
     * @param jwtProvider provider instance
     * @see JwtProvider
     */
    public abstract void setJwtProvider(InAppChat.JwtProvider jwtProvider);

    /**
     * Returns instance of {@link JwtProvider}
     *
     * @return instance of {@link JwtProvider}
     * @see JwtProvider
     */
    @Nullable
    public abstract InAppChat.JwtProvider getJwtProvider();

    /**
     * Navigates to THREAD_LIST view in multithread widget if in-app chat is shown as Fragment.
     * @see org.infobip.mobile.messaging.chat.core.InAppChatWidgetView
     */
    public abstract void showThreadsList();

    /**
     * Set color mode used by in-app chat. Available {@link InAppChatDarkMode} options:
     * <p>DARK_MODE_YES - force dark colors</p>
     * <p>DARK_MODE_NO - force light colors</p>
     * <p>DARK_MODE_FOLLOW_SYSTEM - use same colors as system-wide mode has</p>
     * @deprecated Use combination of InAppChat.setWidgetTheme() and InAppChat.setTheme() instead to achieve dark theme. This function will be removed in a future release.
     *
     * @param darkMode to be set, null value removes setting and use light theme as default one.
     */
    @Deprecated(since = "12.4.0", forRemoval = true)
    public abstract void setDarkMode(@Nullable InAppChatDarkMode darkMode);

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
     * Set the theme of the Livechat Widget.
     * You can define widget themes in <a href="https://portal.infobip.com/apps/livechat/widgets">Live chat widget setup page</a> in Infobip Portal, section `Advanced customization`.
     * Please check widget <a href="https://www.infobip.com/docs/live-chat/widget-customization">documentation</a> for more details.
     *
     * @param widgetThemeName unique theme name, empty or blank value is ignored
     */
    public abstract void setWidgetTheme(@Nullable String widgetThemeName);

    /**
     * Get current Livechat Widget theme.
     *
     * @return applied theme name of Livechat Widget
     */
    @Nullable
    public abstract String getWidgetTheme();

    /**
     * Set the domain of the Livechat Widget.
     *
     * @param domain domain of the widget
     */
    public abstract void setDomain(String domain);

    /**
     * Get current domain of the Livechat Widget.
     *
     * @return domain of the widget
     */
    @Nullable
    public abstract String getDomain();

}
