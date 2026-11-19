/*
 * NotificationChannelRouter.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.notification;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Resolves notification channel IDs without checking whether the channels exist. Free of Android dependencies.
 * <p>
 * Internal SDK class, not intended for use by applications.
 */
public final class NotificationChannelRouter {

    public static final String MM_FALLBACK_CHANNEL_ID = "mm_fallback_channel";
    public static final String MM_FALLBACK_CHANNEL_ID_VIBRATION = "mm_fallback_channel_vibration";
    public static final String MM_FALLBACK_CHANNEL_ID_SOUND = "mm_fallback_channel_sound";
    public static final String MM_FALLBACK_CHANNEL_ID_QUIET = "mm_fallback_channel_quiet";
    public static final String MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID = "mm_fallback_channel_high_priority";
    public static final String MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_VIBRATION = "mm_fallback_channel_high_priority_vibration";
    public static final String MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_SOUND = "mm_fallback_channel_high_priority_sound";
    public static final String MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_QUIET = "mm_fallback_channel_high_priority_quiet";

    private static final String CUSTOM_VIBRATION_SUFFIX = "_vibration";
    private static final String CUSTOM_HIGH_PRIORITY_SUFFIX = "_high_priority";
    // contains a comma, but must not be renamed, otherwise users would lose their settings of already created channels
    private static final String CUSTOM_HIGH_PRIORITY_VIBRATION_SUFFIX = "_high_priority, vibration";

    private NotificationChannelRouter() {
    }

    /**
     * Built-in channel resolution assuming that all channels exist, first match wins: custom payload channel,
     * mapping (chat, silent, high priority, default), single channel, SDK default channels if enabled, SDK fallback channel.
     *
     * @return first of {@link #getCandidateChannelIds}, SDK fallback channel if there are none
     */
    @NonNull
    public static String resolveChannelId(@Nullable NotificationChannelConfig config,
                                          @Nullable String payloadChannelId,
                                          boolean isInAppChatMessage,
                                          boolean isHighPriority,
                                          @Nullable String sound,
                                          boolean isVibrate,
                                          @Nullable String customSoundChannelBaseId) {
        List<String> candidateChannelIds = getCandidateChannelIds(config, payloadChannelId, isInAppChatMessage, isHighPriority,
                sound, isVibrate, customSoundChannelBaseId);
        return candidateChannelIds.isEmpty() ? getFallbackChannelId(sound, isVibrate, isHighPriority) : candidateChannelIds.get(0);
    }

    /**
     * Channels which match the notification, in this order: custom payload channel, mapping (chat, silent, high priority, default),
     * single channel, SDK default channels if enabled. The first existing one is used, so a configured channel which doesn't exist
     * is skipped as if it wasn't configured. SDK fallback channel is not included.
     * <p>
     * Silent is checked before high priority, because with heads-up notifications enabled every notification displayed
     * while the application is in the background is high priority.
     *
     * @param config                   persisted notification channel config, null if not configured
     * @param payloadChannelId         channel ID from the message custom payload, null if not provided or not existing
     * @param isInAppChatMessage       true for In-app chat messages
     * @param isHighPriority           true if the notification should be displayed as heads-up
     * @param sound                    message sound, "default", custom sound name or null for no sound
     * @param isVibrate                true if the message should vibrate
     * @param customSoundChannelBaseId ID of the custom sound channel set using withCustomNotificationChannel, null if not set
     * @return channel IDs without duplicates, empty if no channel matches
     */
    @NonNull
    public static List<String> getCandidateChannelIds(@Nullable NotificationChannelConfig config,
                                                      @Nullable String payloadChannelId,
                                                      boolean isInAppChatMessage,
                                                      boolean isHighPriority,
                                                      @Nullable String sound,
                                                      boolean isVibrate,
                                                      @Nullable String customSoundChannelBaseId) {
        List<String> ids = new ArrayList<>();
        if (config == null) {
            ids.add(resolveDefaultChannelId(sound, isVibrate, isHighPriority, customSoundChannelBaseId));
            return ids;
        }

        addCandidate(ids, payloadChannelId);

        NotificationChannelMapping mapping = config.getMapping();
        if (mapping != null) {
            if (isInAppChatMessage) {
                addCandidate(ids, mapping.getChatChannelId());
            }
            if (isSilent(sound, isVibrate)) {
                addCandidate(ids, mapping.getSilentChannelId());
            }
            if (isHighPriority) {
                addCandidate(ids, mapping.getHighPriorityChannelId());
            }
            addCandidate(ids, mapping.getDefaultChannelId());
        }

        NotificationChannelDefinition singleChannel = config.getSingleChannel();
        if (singleChannel != null) {
            addCandidate(ids, singleChannel.getId());
        }

        if (config.areDefaultChannelsEnabled()) {
            addCandidate(ids, resolveDefaultChannelId(sound, isVibrate, isHighPriority, customSoundChannelBaseId));
        }
        return ids;
    }

    /**
     * Same variants as SDK default channels, because Android 8+ uses the sound and vibration of the channel, not of the notification.
     *
     * @return high priority fallback channel for heads-up notifications, otherwise fallback channel with default importance,
     * the variant with the message's sound and vibration
     */
    @NonNull
    public static String getFallbackChannelId(@Nullable String sound, boolean isVibrate, boolean isHighPriority) {
        boolean soundEnabled = sound != null;
        if (isHighPriority) {
            if (soundEnabled) {
                return isVibrate ? MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID : MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_SOUND;
            }
            return isVibrate ? MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_VIBRATION : MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_QUIET;
        }
        if (soundEnabled) {
            return isVibrate ? MM_FALLBACK_CHANNEL_ID : MM_FALLBACK_CHANNEL_ID_SOUND;
        }
        return isVibrate ? MM_FALLBACK_CHANNEL_ID_VIBRATION : MM_FALLBACK_CHANNEL_ID_QUIET;
    }

    /**
     * @return IDs of all SDK fallback channels
     */
    @NonNull
    public static List<String> getFallbackChannelIds() {
        return Arrays.asList(
                MM_FALLBACK_CHANNEL_ID,
                MM_FALLBACK_CHANNEL_ID_VIBRATION,
                MM_FALLBACK_CHANNEL_ID_SOUND,
                MM_FALLBACK_CHANNEL_ID_QUIET,
                MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID,
                MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_VIBRATION,
                MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_SOUND,
                MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_QUIET
        );
    }

    /**
     * Routing to SDK default channels and to the custom sound channels set using withCustomNotificationChannel.
     */
    @NonNull
    public static String resolveDefaultChannelId(@Nullable String sound, boolean isVibrate, boolean isHighPriority, @Nullable String customSoundChannelBaseId) {
        boolean hasCustomSound = sound != null && !sound.equals("default");
        if (hasCustomSound && customSoundChannelBaseId != null) {
            return getCustomSoundChannelId(customSoundChannelBaseId, isVibrate, isHighPriority);
        }
        return getDefaultChannelId(sound != null, isVibrate, isHighPriority);
    }

    /**
     * @param customSoundChannelBaseId ID of the custom sound channel set using withCustomNotificationChannel
     * @return ID of the custom sound channel variant with the given vibration and priority
     */
    @NonNull
    public static String getCustomSoundChannelId(@NonNull String customSoundChannelBaseId, boolean isVibrate, boolean isHighPriority) {
        if (isHighPriority) {
            return customSoundChannelBaseId + (isVibrate ? CUSTOM_HIGH_PRIORITY_VIBRATION_SUFFIX : CUSTOM_HIGH_PRIORITY_SUFFIX);
        }
        return customSoundChannelBaseId + (isVibrate ? CUSTOM_VIBRATION_SUFFIX : "");
    }

    @NonNull
    public static String getDefaultChannelId(boolean soundEnabled, boolean isVibrate, boolean isHighPriority) {
        if (isHighPriority) {
            if (soundEnabled) {
                return isVibrate ? MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID : MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_SOUND;
            }
            return isVibrate ? MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_VIBRATION : MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_QUIET;
        }
        if (soundEnabled) {
            return isVibrate ? MobileMessagingCore.MM_DEFAULT_CHANNEL_ID : MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_SOUND;
        }
        return isVibrate ? MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_VIBRATION : MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_QUIET;
    }

    /**
     * @param customSoundChannelBaseId ID of the custom sound channel set using withCustomNotificationChannel, null if not set
     * @return IDs of all channels the SDK creates when its default channels are enabled
     */
    @NonNull
    public static List<String> getSdkDefaultChannelIds(@Nullable String customSoundChannelBaseId) {
        List<String> ids = new ArrayList<>(Arrays.asList(
                MobileMessagingCore.MM_DEFAULT_CHANNEL_ID,
                MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_VIBRATION,
                MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_SOUND,
                MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_QUIET,
                MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID,
                MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_VIBRATION,
                MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_SOUND,
                MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_QUIET
        ));
        ids.addAll(getCustomSoundChannelIds(customSoundChannelBaseId));
        return ids;
    }

    /**
     * @param customSoundChannelBaseId ID of the custom sound channel set using withCustomNotificationChannel, null if not set
     * @return IDs of all channels the SDK creates for the custom sound channel, empty if not set
     */
    @NonNull
    public static List<String> getCustomSoundChannelIds(@Nullable String customSoundChannelBaseId) {
        if (StringUtils.isBlank(customSoundChannelBaseId)) {
            return new ArrayList<>();
        }
        return Arrays.asList(
                getCustomSoundChannelId(customSoundChannelBaseId, false, false),
                getCustomSoundChannelId(customSoundChannelBaseId, true, false),
                getCustomSoundChannelId(customSoundChannelBaseId, false, true),
                getCustomSoundChannelId(customSoundChannelBaseId, true, true)
        );
    }

    private static void addCandidate(@NonNull List<String> ids, @Nullable String channelId) {
        if (StringUtils.isNotBlank(channelId) && !ids.contains(channelId)) {
            ids.add(channelId);
        }
    }

    private static boolean isSilent(@Nullable String sound, boolean isVibrate) {
        return sound == null && !isVibrate;
    }
}
