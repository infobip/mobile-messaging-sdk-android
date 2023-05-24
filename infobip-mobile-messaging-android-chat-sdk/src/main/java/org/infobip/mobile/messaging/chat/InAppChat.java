package org.infobip.mobile.messaging.chat;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatDarkMode;

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
        String provideJwt();
    }

    /**
     * Returns instance of chat api
     *
     * @param context android context
     * @return instance of chat api
     */
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
     * Hides in-app chat Fragment, so that all views, especially in-app chat webView, stays in memory.
     * @param fragmentManager manager to make interactions with Fragment
     */
    public abstract void hideInAppChatFragment(FragmentManager fragmentManager);

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
    public abstract InAppChat.JwtProvider getJwtProvider();

    /**
     * Navigates to THREAD_LIST view in multithread widget if in-app chat is shown as Fragment.
     * @see org.infobip.mobile.messaging.chat.core.InAppChatWidgetView
     */
    public abstract void showThreadsList();

    public abstract void setDarkMode(@Nullable InAppChatDarkMode darkMode);

}
