package org.infobip.mobile.messaging.chat.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.chat.broadcast.ChatBroadcaster;
import org.infobip.mobile.messaging.chat.broadcast.ChatBroadcasterImpl;

/**
 * @author sslavin
 * @since 10/10/2017.
 */

public class CoreBroadcastReceiver extends BroadcastReceiver {

    private ChatBroadcaster broadcaster;
    private ObjectMapper mapper;
    private UserProfileManager userProfileManager;

    @SuppressWarnings("unused")
    public CoreBroadcastReceiver() {
        this.mapper = new ObjectMapper();
    }

    @VisibleForTesting
    public CoreBroadcastReceiver(ChatBroadcaster broadcaster, ObjectMapper mapper, UserProfileManager userProfileManager) {
        this.broadcaster = broadcaster;
        this.mapper = mapper;
        this.userProfileManager = userProfileManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        if (!Event.USER_DATA_REPORTED.getKey().equals(intent.getAction())) {
            return;
        }

        UserData userData = UserData.createFrom(intent.getExtras());
        ChatParticipant participant = mapper.fromUserData(userData);
        userProfileManager(context).save(participant);
        broadcaster(context).userInfoSynchronized(participant);
    }

    // region private methods

    private ChatBroadcaster broadcaster(Context context) {
        if (broadcaster == null) {
            broadcaster = new ChatBroadcasterImpl(context);
        }
        return broadcaster;
    }

    private UserProfileManager userProfileManager(Context context) {
        if (userProfileManager == null) {
            userProfileManager = new UserProfileManager(context);
        }
        return userProfileManager;
    }

    // endregion
}
