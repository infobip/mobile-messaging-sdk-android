/*
 * NotificationChannelContext.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.notification;

import org.infobip.mobile.messaging.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Information about the notification which is passed to {@link NotificationChannelResolver}.
 */
public final class NotificationChannelContext {

    private final Message message;
    private final String sdkSuggestedChannelId;
    private final boolean hasSound;
    private final boolean hasCustomSound;
    private final boolean isVibrationEnabled;
    private final boolean isHighPriority;
    private final boolean isInAppChatMessage;

    NotificationChannelContext(@NonNull Message message,
                               @NonNull String sdkSuggestedChannelId,
                               boolean hasSound,
                               boolean hasCustomSound,
                               boolean isVibrationEnabled,
                               boolean isHighPriority,
                               boolean isInAppChatMessage) {
        this.message = message;
        this.sdkSuggestedChannelId = sdkSuggestedChannelId;
        this.hasSound = hasSound;
        this.hasCustomSound = hasCustomSound;
        this.isVibrationEnabled = isVibrationEnabled;
        this.isHighPriority = isHighPriority;
        this.isInAppChatMessage = isInAppChatMessage;
    }

    /**
     * @return message for which the notification is displayed (category, custom payload, internal data...)
     */
    @NonNull
    public Message getMessage() {
        return message;
    }

    /**
     * @return ID of the existing channel the SDK would use, resolved from {@link NotificationChannelConfig} or from the default SDK channels.
     * If none of them exists, ID of the SDK fallback channel, which is created if it is used.
     */
    @NonNull
    public String getSdkSuggestedChannelId() {
        return sdkSuggestedChannelId;
    }

    /**
     * @return true if the message should be displayed with sound (default or custom)
     */
    public boolean hasSound() {
        return hasSound;
    }

    /**
     * @return true if the message requests a custom sound
     */
    public boolean hasCustomSound() {
        return hasCustomSound;
    }

    /**
     * @return custom sound name, "default" or null if the message has no sound
     */
    @Nullable
    public String getSound() {
        return message.getSound();
    }

    /**
     * @return true if the message should vibrate
     */
    public boolean isVibrationEnabled() {
        return isVibrationEnabled;
    }

    /**
     * @return true if the notification should be displayed as heads-up notification
     */
    public boolean isHighPriority() {
        return isHighPriority;
    }

    /**
     * @return true if the message is an In-app chat message, see {@link Message#isChatMessage()}. A message which only opens
     * the In-app chat on tap, e.g. with the "Open chatbot in LiveChat" action, is not a chat message.
     */
    public boolean isInAppChatMessage() {
        return isInAppChatMessage;
    }
}
