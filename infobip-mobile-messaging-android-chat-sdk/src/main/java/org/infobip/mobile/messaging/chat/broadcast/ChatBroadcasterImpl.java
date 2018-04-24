package org.infobip.mobile.messaging.chat.broadcast;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.chat.ChatEvent;
import org.infobip.mobile.messaging.chat.MobileChat;

/**
 * @author sslavin
 * @since 09/10/2017.
 */

public class ChatBroadcasterImpl implements ChatBroadcaster {

    private final Context context;
    private final LocalBroadcastManagerWrapper localBroadcastManagerWrapper;
    private final ChatBundleMapper mapper;

    public ChatBroadcasterImpl(Context context) {
        this.context = context;
        this.localBroadcastManagerWrapper = new LocalBroadcastManagerWrapper(context);
        this.mapper = new ChatBundleMapper();
    }

    @VisibleForTesting
    ChatBroadcasterImpl(Context context, LocalBroadcastManagerWrapper localBroadcastManagerWrapper, ChatBundleMapper mapper) {
        this.context = context;
        this.localBroadcastManagerWrapper = localBroadcastManagerWrapper;
        this.mapper = mapper;
    }

    @Override
    public void chatMessageReceived(ChatMessage message) {
        sendIntent(prepareIntent(ChatEvent.CHAT_MESSAGE_RECEIVED)
                .putExtras(mapper.chatMessageToBundle(message)));
    }

    @Override
    public void chatMessageSent(ChatMessage message) {
        sendIntent(prepareIntent(ChatEvent.CHAT_MESSAGE_SENT)
                .putExtras(mapper.chatMessageToBundle(message)));
    }

    @Override
    public void chatMessageTapped(ChatMessage message) {
        sendIntent(prepareIntent(ChatEvent.CHAT_MESSAGE_TAPPED)
                .putExtras(mapper.chatMessageToBundle(message)));
    }

    @Override
    public void chatMessageViewActionTapped(ChatMessage message, String actionId) {
        sendIntent(prepareIntent(ChatEvent.CHAT_MESSAGE_VIEW_ACTION_TAPPED)
            .putExtras(mapper.chatMessageToBundle(message))
            .putExtra(MobileChat.EXTRA_ACTION_ID, actionId));
    }

    @Override
    public void userInfoSynchronized(ChatParticipant info) {
        sendIntent(prepareIntent(ChatEvent.CHAT_USER_INFO_SYNCHRONIZED)
                .putExtras(mapper.chatParticipantToBundle(info)));
    }

    // region private methods

    private Intent prepareIntent(ChatEvent event) {
        return prepareIntent(event.getKey());
    }

    private Intent prepareIntent(String event) {
        return new Intent(event)
                .setPackage(context.getPackageName());
    }

    private void sendIntent(Intent intent) {
        context.sendBroadcast(intent);
        localBroadcastManagerWrapper.sendBroadcast(intent);
    }

    // endregion
}
