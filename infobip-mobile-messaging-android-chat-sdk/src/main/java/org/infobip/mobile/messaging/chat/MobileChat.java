package org.infobip.mobile.messaging.chat;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.chat.core.MobileChatImpl;
import org.json.JSONObject;

/**
 * Main interface for chat communication
 *
 * @author sslavin
 * @since 05/10/2017.
 */
@SuppressWarnings("unused")
public abstract class MobileChat {

    /**
     * @see ChatEvent#CHAT_MESSAGE_VIEW_ACTION_TAPPED
     */
    public static final String EXTRA_ACTION_ID = "org.infobip.mobile.messaging.chat.EXTRA_ACTION_ID";

    /**
     * Returns unstance of chat api
     * @param context android context
     * @return instance of chat api
     */
    public static MobileChat getInstance(Context context) {
        return MobileMessagingCore.getInstance(context).getMessageHandlerModule(MobileChatImpl.class);
    }

    /**
     * Sends message to chat
     * @param text text of message
     */
    public abstract void sendMessage(String text);

    /**
     * Sends message to chat
     * @param text text of message
     * @param customData any custom data attached to message
     */
    public abstract void sendMessage(String text, JSONObject customData);

    /**
     * Sends message to chat
     * @param text text of message
     * @param listener callback that receives the result
     */
    public abstract void sendMessage(String text, MobileMessaging.ResultListener<ChatMessage> listener);

    /**
     * Sends message to chat
     * @param text text of message
     * @param customData any custom data attached to message
     * @param listener callback that receives the result
     */
    public abstract void sendMessage(String text, JSONObject customData, MobileMessaging.ResultListener<ChatMessage> listener);

    /**
     * Marks chat message as read
     * @param id message id
     */
    public abstract void markMessageRead(String id);

    /**
     * Marks all stored messages as read
     */
    public abstract void markAllMessagesRead();

    /**
     * Sets user info for current user
     * @param info participant information
     */
    public abstract void setUserInfo(ChatParticipant info);

    /**
     * Sets user info for current user
     * @param info participant information
     * @param listener callback that receives the result of operation
     */
    public abstract void setUserInfo(ChatParticipant info, MobileMessaging.ResultListener<ChatParticipant> listener);

    /**
     * Returns user profile cached locally
     * @return user profile
     */
    public abstract ChatParticipant getUserInfo();

    /**
     * Fetches current profile from server
     * (prefer {@link #getUserInfo()} if user profile is changed only on mobile)
     * @param listener callback that receives user profile
     */
    public abstract void fetchUserInfo(MobileMessaging.ResultListener<ChatParticipant> listener);

    /**
     * Fetches current profile from server and caches it in library
     * (prefer {@link #getUserInfo()} if user profile is changed only on mobile)
     */
    public abstract void fetchUserInfo();

    /**
     * Returns message storage to access messages stored on a device
     * @return chat message storage
     */
    public abstract ChatMessageStorage getChatMessageStorage();

    /**
     * Creates chat view activity.
     * @see MobileChatView#show()
     * @return chat view object
     */
    public abstract MobileChatView chatView();

    /**
     * Sets which activities to start when user taps on chat notification. Last one in array will be show, others will be put to task stack.
     * </p>Library will also provide appropriate message together with intent, use following code to retrieve the message:</p>
     * * <pre>
     * {@code
     * ChatMessage message = ChatMessage.createFrom(intent);
     * }
     * </pre>
     * @param activityClasses array of activities to put into task stack when message is tapped
     */
    public abstract void setActivitiesToStartOnMessageTap(Class... activityClasses);
}
