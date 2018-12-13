package org.infobip.mobile.messaging.chat.core;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatMessageStorage;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.chat.MobileChat;
import org.infobip.mobile.messaging.chat.broadcast.ChatBroadcaster;
import org.infobip.mobile.messaging.chat.broadcast.ChatBroadcasterImpl;
import org.infobip.mobile.messaging.chat.broadcast.ChatBundleMapper;
import org.infobip.mobile.messaging.chat.properties.MobileChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.chat.repository.MessageRepositoryImpl;
import org.infobip.mobile.messaging.chat.repository.ParticipantRepositoryImpl;
import org.infobip.mobile.messaging.chat.repository.RepositoryMapper;
import org.infobip.mobile.messaging.interactive.MobileInteractive;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.platform.Time;
import org.json.JSONObject;

import java.util.Set;

import static org.infobip.mobile.messaging.chat.ChatEvent.CHAT_MESSAGE_TAPPED;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class MobileChatImpl extends MobileChat implements MessageHandlerModule {

    private Context context;
    private MobileMessagingCore mobileMessagingCore;
    private MobileInteractive mobileInteractive;
    private ObjectMapper objectMapper = new ObjectMapper();
    private RepositoryMapper repositoryMapper = new RepositoryMapper();
    private ChatBundleMapper bundleMapper = new ChatBundleMapper();
    private ChatBroadcaster broadcaster;
    private UserProfileManager userProfileManager;
    private ChatMessageStorageImpl chatMessageStorage;
    private MobileChatViewImpl mobileChatView;
    private PropertyHelper propertyHelper;
    private ParticipantRepositoryImpl participantRepository;

    @Override
    public void init(Context appContext) {
        this.context = appContext;
    }

    @Override
    public boolean handleMessage(Message message) {
        if (!isChatMessage(message)) {
            return false;
        }

        ChatMessage chatMessage = objectMapper.fromBaseMessage(message, userProfileManager().isUsersMessage(message));
        chatMessageStorage().save(chatMessage);
        broadcaster().chatMessageReceived(chatMessage);
        mobileMessagingCore().getNotificationHandler().displayNotification(message);
        return true;
    }

    @Override
    public boolean messageTapped(Message message) {
        if (!isChatMessage(message)) {
            return false;
        }

        ChatMessage chatMessage = objectMapper.fromBaseMessage(message, userProfileManager().isUsersMessage(message));
        broadcaster().chatMessageTapped(chatMessage);
        doCoreTappedActions(chatMessage);
        return true;
    }

    @Override
    public void sendMessage(String text) {
        sendChatMessage(text, null, null);
    }

    @Override
    public void sendMessage(String text, JSONObject customData) {
        sendChatMessage(text, customData, null);
    }

    @Override
    public void sendMessage(String text, MobileMessaging.ResultListener<ChatMessage> listener) {
        sendChatMessage(text, null, listener);
    }

    @Override
    public void sendMessage(String text, JSONObject customData, MobileMessaging.ResultListener<ChatMessage> listener) {
        sendChatMessage(text, customData, listener);
    }

    @Override
    public void markMessageRead(String id) {
        chatMessageStorage().markRead(id);
        mobileMessagingCore().setMessagesSeenDontStore(id);
    }

    @Override
    public void markAllMessagesRead() {
        Set<String> ids = chatMessageStorage().markAllRead();
        mobileMessagingCore().setMessagesSeenDontStore(ids.toArray(new String[ids.size()]));
    }

    @Override
    public void setUserInfo(ChatParticipant info) {
        setChatUserInfo(info, null);
    }

    @Override
    public void setUserInfo(ChatParticipant info, MobileMessaging.ResultListener<ChatParticipant> listener) {
        setChatUserInfo(info, listener);
    }

    @Override
    public ChatParticipant getUserInfo() {
        return userProfileManager().get();
    }

    @Override
    public void fetchUserInfo() {
        fetchUserInfo(null);
    }

    @Override
    public void fetchUserInfo(final MobileMessaging.ResultListener<ChatParticipant> listener) {
        mobileMessagingCore().fetchUserData(new MobileMessaging.ResultListener<UserData>() {
            @Override
            public void onResult(UserData result) {
                if (result == null) {
                    onError(MobileMessagingError.createFrom(new RuntimeException()));
                } else if (listener != null) {
                    listener.onResult(objectMapper.fromUserData(result));
                }
            }

            @Override
            public void onError(MobileMessagingError e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    @Override
    public ChatMessageStorage getChatMessageStorage() {
        return chatMessageStorage();
    }

    @Override
    public MobileChatViewImpl chatView() {
        if (mobileChatView == null) {
            mobileChatView = new MobileChatViewImpl(context);
        }
        return mobileChatView;
    }

    @Override
    public void setActivitiesToStartOnMessageTap(Class... activityClasses) {
        propertyHelper().saveClasses(MobileChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES, activityClasses);
    }

    // region private methods

    private void sendChatMessage(String text, JSONObject customData, final MobileMessaging.ResultListener<ChatMessage> listener) {
        ChatMessage message = new ChatMessage();
        message.setBody(text);
        message.setAuthor(userProfileManager().get());
        message.setCustomData(customData);
        message.setReceivedAt(Time.now());
        message.setYours(true);
        chatMessageStorage().save(message);
        Message baseMessage = objectMapper.toBaseMessage(message);
        mobileMessagingCore().sendMessagesDontStore(new MobileMessaging.ResultListener<Message[]>() {
            @Override
            public void onResult(Message[] result) {
                if (result == null || result.length == 0) {
                    onError(MobileMessagingError.createFrom(new RuntimeException()));
                } else {
                    ChatMessage sentMessage = objectMapper.fromBaseMessage(result[0], userProfileManager().isUsersMessage(result[0]));
                    chatMessageStorage().save(sentMessage);
                    broadcaster().chatMessageSent(sentMessage);
                    if (listener != null) {
                        listener.onResult(sentMessage);
                    }
                }
            }

            @Override
            public void onError(MobileMessagingError e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        }, baseMessage);
    }

    private void setChatUserInfo(ChatParticipant info, final MobileMessaging.ResultListener<ChatParticipant> listener) {
        userProfileManager().save(info);
        UserData userData = objectMapper.toUserData(info);
        mobileMessagingCore().saveUserData(userData, new MobileMessaging.ResultListener<UserData>() {
            @Override
            public void onResult(UserData result) {
                if (result == null) {
                    onError(MobileMessagingError.createFrom(new RuntimeException()));
                } else {
                    ChatParticipant participant = objectMapper.fromUserData(result);
                    userProfileManager.save(participant);
                    broadcaster().userInfoSynchronized(participant);
                    if (listener != null) {
                        listener.onResult(participant);
                    }
                }
            }

            @Override
            public void onError(MobileMessagingError e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    private static boolean isChatMessage(Message message) {
        return message.getCustomPayload() != null &&
                message.getCustomPayload().optBoolean("isChat", false);
    }

    synchronized private MobileMessagingCore mobileMessagingCore() {
        if (mobileMessagingCore == null) {
            mobileMessagingCore = MobileMessagingCore.getInstance(context);
        }
        return mobileMessagingCore;
    }

    synchronized private ChatBroadcaster broadcaster() {
        if (broadcaster == null) {
            broadcaster = new ChatBroadcasterImpl(context);
        }
        return broadcaster;
    }

    synchronized private UserProfileManager userProfileManager() {
        if (userProfileManager == null) {
            userProfileManager = new UserProfileManager(context);
        }
        return userProfileManager;
    }

    synchronized private ChatMessageStorageImpl chatMessageStorage() {
        if (chatMessageStorage == null) {
            chatMessageStorage = new ChatMessageStorageImpl(new MessageRepositoryImpl(context), participantRepository(), repositoryMapper);
        }
        return chatMessageStorage;
    }

    synchronized private ParticipantRepositoryImpl participantRepository() {
        if (participantRepository == null) {
            participantRepository = new ParticipantRepositoryImpl(context);
        }
        return participantRepository;
    }

    synchronized private MobileInteractive mobileInteractive() {
        if (mobileInteractive == null) {
            mobileInteractive = MobileInteractive.getInstance(context);
        }
        return mobileInteractive;
    }

    synchronized private PropertyHelper propertyHelper() {
        if (propertyHelper == null) {
            propertyHelper = new PropertyHelper(context);
        }
        return propertyHelper;
    }

    @Override
    public void applicationInForeground() {
        // do nothing
    }

    @Override
    public void cleanup() {
        chatMessageStorage().deleteAll();
        participantRepository().clear();
        propertyHelper().remove(MobileChatProperty.USER_NAME_DIALOG_SHOWN);
    }

    @Override
    public void logoutUser() {
        chatMessageStorage().deleteAll();
        participantRepository().clear();
        propertyHelper().remove(MobileChatProperty.USER_NAME_DIALOG_SHOWN);
    }

    public void processTappedAction(ChatMessage chatMessage, NotificationAction action) {
        broadcaster().chatMessageViewActionTapped(chatMessage, action.getId());
        mobileInteractive().triggerSdkActionsFor(action, objectMapper.toBaseMessage(chatMessage));
    }

    @NonNull
    private TaskStackBuilder stackBuilderForNotificationTap(ChatMessage message) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        Bundle messageBundle = bundleMapper.chatMessageToBundle(message);
        Class classes[] = propertyHelper().findClasses(MobileChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES);
        if (classes != null) {
            for (Class cls : classes) {
                stackBuilder.addNextIntent(new Intent(context, cls)
                        .setAction(CHAT_MESSAGE_TAPPED.getKey())
                        .putExtras(messageBundle));
            }
        }

        NotificationSettings notificationSettings = mobileMessagingCore().getNotificationSettings();
        if (stackBuilder.getIntentCount() == 0 && notificationSettings != null && notificationSettings.getCallbackActivity() != null) {
            stackBuilder.addNextIntent(new Intent(context, notificationSettings.getCallbackActivity())
                    .setAction(CHAT_MESSAGE_TAPPED.getKey())
                    .putExtras(messageBundle));
        }

        return stackBuilder;
    }

    private void doCoreTappedActions(ChatMessage chatMessage) {
        NotificationSettings notificationSettings = mobileMessagingCore().getNotificationSettings();
        if (notificationSettings != null && notificationSettings.markSeenOnTap()) {
            markMessageRead(chatMessage.getId());
        }

        TaskStackBuilder stackBuilder = stackBuilderForNotificationTap(chatMessage);
        if (stackBuilder.getIntentCount() != 0) {
            stackBuilder.startActivities();
        }
    }

    // endregion
}
