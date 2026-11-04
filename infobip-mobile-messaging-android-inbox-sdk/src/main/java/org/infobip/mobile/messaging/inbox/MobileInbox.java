/*
 * MobileInbox.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessaging;

import androidx.annotation.NonNull;

/**
 * Main interface for inbox.
 * External user IDs must be passed as raw strings, not URL-encoded values.
 * The SDK handles path encoding for fetch and delete requests.
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
     * Null, empty or whitespace-only tokens and external user IDs are rejected locally.
     * If supplied, the listener is invoked synchronously with validation error code {@code 20013}.
     * A blank token never falls back to Application Code authorization; use the tokenless overload instead.
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
     * Null, empty or whitespace-only external user IDs are rejected locally.
     * If supplied, the listener is invoked synchronously with validation error code {@code 20013}.
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
     * Asynchronously deletes one or more inbox messages using the same JWT authorization as inbox fetch.
     * Deletion is idempotent: missing and already deleted messages are reported as success.
     *
     * @throws UnsupportedOperationException synchronously if this implementation does not support deletion.
     *         The default implementation throws without invoking the listener, preserving compatibility
     *         with subclasses that predate deletion support.
     */
    public void deleteMessages(@NonNull String token, @NonNull String externalUserId,
                               @NonNull String[] messageIds, MobileMessaging.ResultListener<Void> listener) {
        throw new UnsupportedOperationException("Inbox deletion is not supported by this implementation");
    }

    /**
     * Asynchronously deletes one or more inbox messages using Application Code authorization.
     *
     * @throws UnsupportedOperationException synchronously if this implementation does not support deletion.
     *         The default implementation throws without invoking the listener, preserving compatibility
     *         with subclasses that predate deletion support.
     */
    public void deleteMessages(@NonNull String externalUserId, @NonNull String[] messageIds,
                               MobileMessaging.ResultListener<Void> listener) {
        throw new UnsupportedOperationException("Inbox deletion is not supported by this implementation");
    }

    public void deleteMessage(@NonNull String token, @NonNull String externalUserId,
                              @NonNull String messageId, MobileMessaging.ResultListener<Void> listener) {
        deleteMessages(token, externalUserId, new String[]{messageId}, listener);
    }

    public void deleteMessage(@NonNull String externalUserId, @NonNull String messageId,
                              MobileMessaging.ResultListener<Void> listener) {
        deleteMessages(externalUserId, new String[]{messageId}, listener);
    }

    /**
     * Asynchronously marks inbox messages as seen
     *
     * @param externalUserId External User ID is meant to be an ID of a user in an external (non-Infobip) service.
     * @param messageIds     array of inbox messages identifiers that need to be marked as seen.
     * @param listener       listener to report the result on
     * @see MobileMessaging.ResultListener
     */
    public abstract void setSeen(@NonNull String externalUserId, @NonNull String[] messageIds, MobileMessaging.ResultListener<String[]> listener);
}
