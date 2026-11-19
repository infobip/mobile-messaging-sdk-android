/*
 * NotificationChannelScenario.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.notification.NotificationChannelConfig;
import org.infobip.mobile.messaging.notification.NotificationChannelDefinition;
import org.infobip.mobile.messaging.notification.NotificationChannelMapping;
import org.infobip.mobile.messaging.notification.NotificationChannelResolver;
import org.json.JSONObject;

/**
 * SDK notification channel setups for manual testing.
 * The selected scenario is stored in the app's own preferences and applied in {@link NotificationChannelsApplication}.
 */
enum NotificationChannelScenario {

    NO_CONFIG("1. Nothing configured",
            "SDK defaults: the 8 mm_default_channel* channels, no fallback channel.") {
        @Override
        void configure(MobileMessaging.Builder builder) {
        }
    },

    CUSTOM_SOUND("2. Custom sound channel",
            "withCustomNotificationChannel(\"custom\", \"Custom\", \"ding\"): defaults plus custom, custom_vibration, " +
                    "custom_high_priority and \"custom_high_priority, vibration\".") {
        @Override
        void configure(MobileMessaging.Builder builder) {
            builder.withCustomNotificationChannel(CUSTOM_SOUND_CHANNEL_ID, "Custom", SOUND_DING);
        }
    },

    SINGLE_CHANNEL("3. Single channel",
            "withSingleNotificationChannel(\"demo_main\", ...): only demo_main, every notification goes there.") {
        @Override
        void configure(MobileMessaging.Builder builder) {
            builder.withSingleNotificationChannel("demo_main", "Demo", "All demo notifications");
        }
    },

    SINGLE_CHANNEL_WITH_SOUND("3b. Single channel with custom sound",
            "withSingleChannel(definition): only demo_ding (high importance, ding sound, no vibration), " +
                    "every notification goes there regardless of the message sound or vibration.") {
        @Override
        void configure(MobileMessaging.Builder builder) {
            // separate channel ID, sound and vibration of the existing demo_main channel can't be changed
            builder.withNotificationChannelConfig(NotificationChannelConfig.builder()
                    .withSingleChannel(NotificationChannelDefinition.builder("demo_ding")
                            .withName("Demo with sound")
                            .withDescription("All demo notifications with the ding sound")
                            .withImportance(NotificationChannelDefinition.Importance.HIGH)
                            .withSound(SOUND_DING)
                            .withVibration(false)
                            .build())
                    .build());
        }
    },

    FULL_CONFIG("4. Full config",
            "No defaults, mapping (alerts / quiet / general / chat), payload key \"androidChannelId\", " +
                    "channels alerts, general, quiet, promo and chat.") {
        @Override
        void configure(MobileMessaging.Builder builder) {
            builder.withNotificationChannelConfig(fullConfig(true));
        }
    },

    FULL_CONFIG_WITHOUT_DEFAULT_MAPPING("4b. Full config without default mapping",
            "Same as 4, but without mapping.default: normal messages which aren't heads-up (foreground, no banner) " +
                    "go to mm_fallback_channel, or to its _vibration or _sound variant, created on first use.") {
        @Override
        void configure(MobileMessaging.Builder builder) {
            builder.withNotificationChannelConfig(fullConfig(false));
        }
    },

    WITHOUT_DEFAULT_CHANNELS("4c. Without default channels",
            "withoutDefaultNotificationChannels() only: heads-up messages go to mm_fallback_channel_high_priority, " +
                    "all others to mm_fallback_channel, each with a _vibration, _sound or _quiet variant matching the message, " +
                    "created on first use.") {
        @Override
        void configure(MobileMessaging.Builder builder) {
            builder.withoutDefaultNotificationChannels();
        }
    },

    RESOLVER("5. Native resolver",
            "SDK defaults plus a resolver: type=order goes to \"orders\" (create it below), type=crash throws.") {
        @Override
        void configure(MobileMessaging.Builder builder) {
            builder.withNotificationChannelResolver(RESOLVER_IMPL);
        }
    },

    FULL_CONFIG_WITH_RESOLVER("5b. Full config and resolver",
            "Config from 4 plus the resolver from 5. Unknown resolver result falls back to the config's channel.") {
        @Override
        void configure(MobileMessaging.Builder builder) {
            builder.withNotificationChannelConfig(fullConfig(true))
                    .withNotificationChannelResolver(RESOLVER_IMPL);
        }
    };

    static final String SOUND_DING = "ding";
    static final String CUSTOM_SOUND_CHANNEL_ID = "custom";
    static final String ORDERS_CHANNEL_ID = "orders";
    static final String PAYLOAD_CHANNEL_ID_KEY = "androidChannelId";

    private static final String PREFS_NAME = "notification_channels_demo";
    private static final String PREF_SCENARIO = "scenario";

    private static final NotificationChannelResolver RESOLVER_IMPL = context -> {
        JSONObject customPayload = context.getMessage().getCustomPayload();
        String type = customPayload != null ? customPayload.optString("type") : "";
        Log.d(NotificationChannelsApplication.TAG, "Resolver: suggested=" + context.getSdkSuggestedChannelId()
                + " highPriority=" + context.isHighPriority()
                + " chat=" + context.isInAppChatMessage()
                + " sound=" + context.getSound()
                + " vibration=" + context.isVibrationEnabled()
                + " type=" + type);
        if ("crash".equals(type)) {
            throw new IllegalStateException("Resolver failure requested by the message");
        }
        return "order".equals(type) ? ORDERS_CHANNEL_ID : context.getSdkSuggestedChannelId();
    };

    final String title;
    final String description;

    NotificationChannelScenario(String title, String description) {
        this.title = title;
        this.description = description;
    }

    abstract void configure(MobileMessaging.Builder builder);

    static NotificationChannelScenario load(Context context) {
        String name = prefs(context).getString(PREF_SCENARIO, NO_CONFIG.name());
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return NO_CONFIG;
        }
    }

    void save(Context context) {
        // commit, the process is restarted right after
        prefs(context).edit().putString(PREF_SCENARIO, name()).commit();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static NotificationChannelConfig fullConfig(boolean withDefaultMapping) {
        NotificationChannelMapping.Builder mapping = NotificationChannelMapping.builder()
                .withHighPriorityChannelId("alerts")
                .withSilentChannelId("quiet")
                .withChatChannelId("chat");
        if (withDefaultMapping) {
            mapping.withDefaultChannelId("general");
        }

        return NotificationChannelConfig.builder()
                .withoutDefaultChannels()
                .withPayloadChannelIdKey(PAYLOAD_CHANNEL_ID_KEY)
                .withChannelMapping(mapping.build())
                .addChannel(NotificationChannelDefinition.builder("alerts")
                        .withName("Alerts")
                        .withDescription("Heads-up notifications with the ding sound")
                        .withImportance(NotificationChannelDefinition.Importance.HIGH)
                        .withSound(SOUND_DING)
                        .build())
                .addChannel(NotificationChannelDefinition.builder("general")
                        .withName("General")
                        .build())
                .addChannel(NotificationChannelDefinition.builder("quiet")
                        .withName("Quiet")
                        .withImportance(NotificationChannelDefinition.Importance.LOW)
                        .withSound(NotificationChannelDefinition.SOUND_NONE)
                        .withVibration(false)
                        .build())
                .addChannel(NotificationChannelDefinition.builder("promo")
                        .withName("Promotions")
                        .build())
                .addChannel(NotificationChannelDefinition.builder("chat")
                        .withName("Chat")
                        .withImportance(NotificationChannelDefinition.Importance.HIGH)
                        .build())
                .build();
    }
}
