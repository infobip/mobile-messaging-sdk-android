/*
 * NotificationChannelConfig.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.notification;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Declarative configuration of notification channels used by the SDK (Android 8.0, API 26 and above).
 * <p>
 * Without this configuration the SDK creates and uses its default notification channels. With it, the application can:
 * <ul>
 * <li>use a single notification channel for all notifications - {@link Builder#withSingleChannel(String, String, String)}</li>
 * <li>prevent the SDK from creating its default channels - {@link Builder#withoutDefaultChannels()}</li>
 * <li>map notification kinds to channels - {@link Builder#withChannelMapping(NotificationChannelMapping)}</li>
 * <li>select the channel per message using a key from the message custom payload - {@link Builder#withPayloadChannelIdKey(String)}</li>
 * <li>let the SDK create application channels - {@link Builder#addChannel(NotificationChannelDefinition)}</li>
 * </ul>
 * Channel for each notification is resolved in this order, the first matching channel which exists is used:
 * <ol>
 * <li>channel ID from the custom payload key</li>
 * <li>mapping: chat, silent, high priority, default</li>
 * <li>single channel</li>
 * <li>SDK default channels, if enabled</li>
 * <li>SDK fallback channel, mm_fallback_channel_high_priority for heads-up notifications, otherwise mm_fallback_channel,
 * with the _vibration, _sound or _quiet suffix for messages with vibration only, sound only or neither</li>
 * </ol>
 * The SDK never deletes notification channels automatically. Configuration is persisted, so it is used also when a notification
 * is displayed before the SDK is initialized (e.g. when the application process is started by a push message).
 * <p>
 * The configuration is JSON serializable, so that it can be provided by cross-platform plugins:
 * <pre>
 * {@code {
 *   "withoutDefaultChannels": true,
 *   "singleChannel": { "id": "marketing", "name": "Offers", "description": "Promotions and offers" },
 *   "mapping": { "default": "general", "highPriority": "alerts", "silent": "silent", "chat": "chat" },
 *   "payloadChannelIdKey": "androidChannelId",
 *   "channels": [ { "id": "alerts", "name": "Alerts", "importance": "high", "sound": "default", "vibration": true } ]
 * }}
 * </pre>
 *
 * @see org.infobip.mobile.messaging.MobileMessaging.Builder#withNotificationChannelConfig(NotificationChannelConfig)
 * @see NotificationChannelResolver
 */
public final class NotificationChannelConfig {

    private Boolean withoutDefaultChannels;
    private NotificationChannelDefinition singleChannel;
    private NotificationChannelMapping mapping;
    private String payloadChannelIdKey;
    private List<NotificationChannelDefinition> channels;

    @SuppressWarnings("unused")
    private NotificationChannelConfig() {
        // used by JSON deserialization
    }

    private NotificationChannelConfig(Builder builder) {
        this.withoutDefaultChannels = builder.withoutDefaultChannels ? Boolean.TRUE : null;
        this.singleChannel = builder.singleChannel;
        this.mapping = builder.mapping;
        this.payloadChannelIdKey = builder.payloadChannelIdKey;
        this.channels = builder.channels.isEmpty() ? null : new ArrayList<>(builder.channels);
    }

    public static Builder builder() {
        return new Builder(null);
    }

    /**
     * @param config configuration to copy values from
     * @return builder pre-filled with values of the given configuration
     */
    public static Builder builder(@Nullable NotificationChannelConfig config) {
        return new Builder(config);
    }

    /**
     * Parses configuration from JSON, e.g. when provided by a cross-platform plugin.
     *
     * @param json configuration JSON
     * @return parsed configuration or null if JSON is null
     */
    @Nullable
    public static NotificationChannelConfig fromJson(@Nullable String json) {
        if (json == null) {
            return null;
        }
        return new JsonSerializer().deserialize(json, NotificationChannelConfig.class);
    }

    @NonNull
    public String toJson() {
        return new JsonSerializer().serialize(this);
    }

    /**
     * @return true if the SDK creates and uses its default notification channels, false if they are disabled
     * using {@link Builder#withoutDefaultChannels()} or {@link Builder#withSingleChannel(String, String, String)}
     */
    public boolean areDefaultChannelsEnabled() {
        return !isWithoutDefaultChannels() && singleChannel == null;
    }

    public boolean isWithoutDefaultChannels() {
        return Boolean.TRUE.equals(withoutDefaultChannels);
    }

    @Nullable
    public NotificationChannelDefinition getSingleChannel() {
        return singleChannel;
    }

    @Nullable
    public NotificationChannelMapping getMapping() {
        return mapping;
    }

    @Nullable
    public String getPayloadChannelIdKey() {
        return payloadChannelIdKey;
    }

    @NonNull
    public List<NotificationChannelDefinition> getChannels() {
        if (channels == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(channels);
    }

    public static final class Builder {
        private boolean withoutDefaultChannels;
        private NotificationChannelDefinition singleChannel;
        private NotificationChannelMapping mapping;
        private String payloadChannelIdKey;
        private final List<NotificationChannelDefinition> channels = new ArrayList<>();

        private Builder(@Nullable NotificationChannelConfig config) {
            if (config == null) {
                return;
            }
            this.withoutDefaultChannels = config.isWithoutDefaultChannels();
            this.singleChannel = config.singleChannel;
            this.mapping = config.mapping;
            this.payloadChannelIdKey = config.payloadChannelIdKey;
            this.channels.addAll(config.getChannels());
        }

        /**
         * SDK will not create its default notification channels (mm_default_channel*).
         * Notifications will use channels from mapping, single channel, custom payload or {@link NotificationChannelResolver}.
         * If none of them provides an existing channel, the SDK fallback channel is used: mm_fallback_channel_high_priority
         * for heads-up notifications, otherwise mm_fallback_channel, with the _vibration, _sound or _quiet suffix for messages
         * with vibration only, sound only or neither, same as SDK default channels.
         * Custom notification channel set using
         * {@link org.infobip.mobile.messaging.MobileMessaging.Builder#withCustomNotificationChannel(String, String, String)} is ignored.
         */
        public Builder withoutDefaultChannels() {
            this.withoutDefaultChannels = true;
            return this;
        }

        /**
         * All notifications will use a single notification channel which the SDK creates. SDK default channels are not created.
         * The SDK creates the channel with default importance (an existing channel keeps its importance), so on Android 8.0 and above
         * notifications in it aren't displayed as heads-up, see {@link #withSingleChannel(NotificationChannelDefinition)}.
         *
         * @param id   channel ID
         * @param name channel name visible to the user in system settings
         */
        public Builder withSingleChannel(@NonNull String id, @NonNull String name) {
            return withSingleChannel(id, name, null);
        }

        /**
         * All notifications will use a single notification channel which the SDK creates. SDK default channels are not created.
         * The SDK creates the channel with default importance (an existing channel keeps its importance), so on Android 8.0 and above
         * notifications in it aren't displayed as heads-up, see {@link #withSingleChannel(NotificationChannelDefinition)}.
         *
         * @param id          channel ID
         * @param name        channel name visible to the user in system settings
         * @param description channel description visible to the user in system settings
         */
        public Builder withSingleChannel(@NonNull String id, @NonNull String name, @Nullable String description) {
            return withSingleChannel(NotificationChannelDefinition.builder(id)
                    .withName(name)
                    .withDescription(description)
                    .build());
        }

        /**
         * All notifications will use a single notification channel which the SDK creates. SDK default channels are not created.
         * Use {@link NotificationChannelDefinition.Builder#withNameResource(String)} to provide a localized channel name.
         * Android 8.0 and above displays notifications as heads-up only in channels with high importance. Use
         * {@link NotificationChannelDefinition.Importance#HIGH} to display heads-up notifications, then also notifications displayed
         * while the application is in the foreground are heads-up. Android doesn't change the importance of an existing channel, also of
         * a deleted one created again, so use a new channel ID to switch existing installations to heads-up. SDK logs a warning if
         * heads-up notifications are enabled, but end up in a single channel without high importance.
         *
         * @param definition channel definition
         */
        public Builder withSingleChannel(@NonNull NotificationChannelDefinition definition) {
            this.singleChannel = definition;
            return this;
        }

        /**
         * @param mapping channels to use for different kinds of notifications
         */
        public Builder withChannelMapping(@Nullable NotificationChannelMapping mapping) {
            this.mapping = mapping;
            return this;
        }

        /**
         * Notification is displayed in the channel whose ID is provided in the message custom payload under the given key,
         * if such channel exists. Otherwise, the next resolution step is used.
         *
         * @param customPayloadKey key in the message custom payload, e.g. "androidChannelId"
         */
        public Builder withPayloadChannelIdKey(@Nullable String customPayloadKey) {
            this.payloadChannelIdKey = customPayloadKey;
            return this;
        }

        /**
         * Adds a notification channel which the SDK creates on behalf of the application, if it doesn't exist yet.
         *
         * @param definition channel definition
         */
        public Builder addChannel(@NonNull NotificationChannelDefinition definition) {
            this.channels.add(definition);
            return this;
        }

        public NotificationChannelConfig build() {
            return new NotificationChannelConfig(this);
        }
    }
}
