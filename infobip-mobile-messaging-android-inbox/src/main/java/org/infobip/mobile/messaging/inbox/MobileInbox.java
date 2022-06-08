package org.infobip.mobile.messaging.inbox;

import android.content.Context;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessaging;

/**
 * Main interface for inbox
 */
public abstract class MobileInbox {

    /**
     * Returns instance of inbox api
     *
     * @param context android context
     * @return instance of inbox api
     */
    public synchronized static MobileInbox getInstance(Context context) {
        return MobileInboxImpl.getInstance(context);
    }

    /**
     * Asynchronously fetches inbox data for authorised user.
     * @param token authentication token required for current user to have access to the Inbox messages.
     * @param externalUserId external User ID is meant to be an ID of a user in an external (non-Infobip) service.
     * @param filterOptions filtering options applied to messages list in response.
     */
    public abstract void fetchInbox(String token, @NonNull String externalUserId, MobileInboxFilterOptions filterOptions, MobileMessaging.ResultListener<Inbox> messageResultListener);

    /**
     * Asynchronously fetches inbox data for authorised user. Uses application Code for authorization.
     * TODO: warning
     * @param externalUserId external User ID is meant to be an ID of a user in an external (non-Infobip) service.
     * @param filterOptions filtering options applied to messages list in response.
     */
    //MM-5082
//    public abstract void doFetchInbox(@NonNull String externalUserId, MobileInboxFilterOptions filterOptions, MobileMessaging.ResultListener<Inbox> messageResultListener);

}
