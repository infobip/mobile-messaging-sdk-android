/*
 * NotificationChannelMapping.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.notification;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.Nullable;

/**
 * Maps notification kinds to channel IDs. All channel IDs are optional. Channels have to exist on the device:
 * create them in the application or declare them using {@link NotificationChannelConfig.Builder#addChannel(NotificationChannelDefinition)}.
 * <p>
 * The first matching rule is used: chat, silent (no sound and no vibration), high priority (heads-up), default.
 * Silent is matched before high priority, because with heads-up notifications enabled every notification displayed
 * while the application is in the background is high priority. A rule whose channel doesn't exist is skipped.
 */
public final class NotificationChannelMapping {

    @SerializedName("default")
    private String defaultChannelId;
    @SerializedName("highPriority")
    private String highPriorityChannelId;
    @SerializedName("silent")
    private String silentChannelId;
    @SerializedName("chat")
    private String chatChannelId;

    @SuppressWarnings("unused")
    private NotificationChannelMapping() {
        // used by JSON deserialization
    }

    private NotificationChannelMapping(Builder builder) {
        this.defaultChannelId = builder.defaultChannelId;
        this.highPriorityChannelId = builder.highPriorityChannelId;
        this.silentChannelId = builder.silentChannelId;
        this.chatChannelId = builder.chatChannelId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public String getDefaultChannelId() {
        return defaultChannelId;
    }

    @Nullable
    public String getHighPriorityChannelId() {
        return highPriorityChannelId;
    }

    @Nullable
    public String getSilentChannelId() {
        return silentChannelId;
    }

    @Nullable
    public String getChatChannelId() {
        return chatChannelId;
    }

    public static final class Builder {
        private String defaultChannelId;
        private String highPriorityChannelId;
        private String silentChannelId;
        private String chatChannelId;

        private Builder() {
        }

        /**
         * @param channelId channel used for notifications not matched by any other rule
         */
        public Builder withDefaultChannelId(@Nullable String channelId) {
            this.defaultChannelId = channelId;
            return this;
        }

        /**
         * @param channelId channel used for heads-up notifications, silent ones use the silent channel if it is set
         */
        public Builder withHighPriorityChannelId(@Nullable String channelId) {
            this.highPriorityChannelId = channelId;
            return this;
        }

        /**
         * @param channelId channel used for notifications without sound and vibration
         */
        public Builder withSilentChannelId(@Nullable String channelId) {
            this.silentChannelId = channelId;
            return this;
        }

        /**
         * @param channelId channel used for In-app chat messages, see {@link org.infobip.mobile.messaging.Message#isChatMessage()}.
         *                  Other messages, e.g. with the "Open chatbot in LiveChat" action, use the other rules.
         */
        public Builder withChatChannelId(@Nullable String channelId) {
            this.chatChannelId = channelId;
            return this;
        }

        public NotificationChannelMapping build() {
            return new NotificationChannelMapping(this);
        }
    }
}
