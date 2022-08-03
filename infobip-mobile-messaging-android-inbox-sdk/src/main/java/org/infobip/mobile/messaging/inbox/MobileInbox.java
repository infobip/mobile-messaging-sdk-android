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
     *
     * @param token access token (JWT in a strictly predefined format) required for current user to have access to the Inbox messages.
     * @param externalUserId External User ID is meant to be an ID of a user in an external (non-Infobip) service.
     * @param filterOptions filtering options applied to messages list in response. Nullable, will return default number of messages
     * @param messageResultListener listener to report the result on
     * @see MobileMessaging.ResultListener
     */
    public abstract void fetchInbox(String token, @NonNull String externalUserId, MobileInboxFilterOptions filterOptions, MobileMessaging.ResultListener<Inbox> messageResultListener);

    /**
     * Asynchronously fetches inbox data for authorised user. Uses application Code for authorization.
     *
     * This version of API uses Application Code (or API key) based authorization. This is not the most secure way of authorization because it is heavily dependent on how secure is your Application Code stored on a device.
     * If the security is a crucial aspect, consider using the alternative `fetchInbox` API that is based on access token authorization.
     * @param externalUserId External User ID is meant to be an ID of a user in an external (non-Infobip) service.
     * @param filterOptions filtering options applied to messages list in response. Nullable, will return default number of messages
     * @param messageResultListener listener to report the result on
     * @see MobileMessaging.ResultListener
     */

    public abstract void fetchInbox(@NonNull String externalUserId, MobileInboxFilterOptions filterOptions, MobileMessaging.ResultListener<Inbox> messageResultListener);

    /**
     * Asynchronously marks inbox messages as seen
     *
     * @param listener listener to report the result on
     * @see MobileMessaging.ResultListener
     * @param externalUserId External User ID is meant to be an ID of a user in an external (non-Infobip) service.
     * @param messageIds array of inbox messages identifiers that need to be marked as seen.
     */
    public abstract void setSeen(MobileMessaging.ResultListener<String[]> listener, @NonNull String externalUserId, @NonNull String... messageIds);
}
