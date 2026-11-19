/*
 * NotificationChannelResolver.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.notification;

import androidx.annotation.NonNull;

/**
 * Lets the application decide which notification channel is used for each notification displayed by the SDK
 * (push messages and In-app chat messages). Used only on Android 8.0 (API 26) and above.
 * <p>
 * The resolver is called synchronously while the notification is being built, so it must be fast and must not block.
 * It is kept in memory only, so register it on every application start, e.g. in {@code Application.onCreate()}, using
 * {@link org.infobip.mobile.messaging.MobileMessaging.Builder#withNotificationChannelResolver(NotificationChannelResolver)}
 * or {@link org.infobip.mobile.messaging.MobileMessaging#setNotificationChannelResolver(NotificationChannelResolver)}.
 * <p>
 * The returned channel must already exist. If it does not exist, is blank or the resolver throws,
 * the SDK uses {@link NotificationChannelContext#getSdkSuggestedChannelId()}.
 *
 * <pre>
 * {@code MobileMessaging.setNotificationChannelResolver(context ->
 *         context.isInAppChatMessage() ? "chat" : context.getSdkSuggestedChannelId());}
 * </pre>
 *
 * @see NotificationChannelConfig
 */
public interface NotificationChannelResolver {

    /**
     * Resolves notification channel ID for the notification which is about to be displayed.
     *
     * @param context information about the notification
     * @return ID of an existing notification channel, or {@link NotificationChannelContext#getSdkSuggestedChannelId()} to keep the SDK choice
     */
    @NonNull
    String resolveChannelId(@NonNull NotificationChannelContext context);
}
