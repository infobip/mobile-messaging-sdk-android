/*
 * NotificationChannelManager.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.resources.R;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.StringUtils;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

/**
 * Persists {@link NotificationChannelConfig}, creates configured channels and resolves the channel for each notification.
 * <p>
 * Internal SDK class, not intended for use by applications.
 */
public final class NotificationChannelManager {

    private static final class CachedConfig {
        final String json;
        final NotificationChannelConfig config;

        CachedConfig(String json, NotificationChannelConfig config) {
            this.json = json;
            this.config = config;
        }
    }

    private static volatile CachedConfig cachedConfig;
    private static volatile NotificationChannelResolver resolver;
    // set on every MobileMessaging.Builder.build(), kept separately so that rebuilding without a resolver
    // doesn't remove the one registered using MobileMessaging.setNotificationChannelResolver (e.g. in a plugin's native Application class)
    private static volatile NotificationChannelResolver builderResolver;

    private NotificationChannelManager() {
    }

    // region config and resolver

    public static void saveConfig(@NonNull Context context, @Nullable NotificationChannelConfig config) {
        if (config == null) {
            PreferenceHelper.remove(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_CONFIG);
            return;
        }
        PreferenceHelper.saveString(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_CONFIG, config.toJson());
    }

    /**
     * @return persisted config or null if not configured or not parsable
     */
    @Nullable
    public static NotificationChannelConfig loadConfig(@NonNull Context context) {
        String json = PreferenceHelper.findString(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_CONFIG);
        if (json == null) {
            return null;
        }

        CachedConfig cached = cachedConfig;
        if (cached != null && json.equals(cached.json)) {
            return cached.config;
        }

        NotificationChannelConfig config = null;
        try {
            config = NotificationChannelConfig.fromJson(json);
        } catch (Exception e) {
            MobileMessagingLogger.e("Could not parse notification channel config, SDK default notification channels will be used", e);
        }
        cachedConfig = new CachedConfig(json, config);
        return config;
    }

    public static void setResolver(@Nullable NotificationChannelResolver notificationChannelResolver) {
        resolver = notificationChannelResolver;
    }

    public static void setBuilderResolver(@Nullable NotificationChannelResolver notificationChannelResolver) {
        builderResolver = notificationChannelResolver;
    }

    /**
     * @return resolver set using MobileMessaging.Builder, otherwise resolver set using MobileMessaging.setNotificationChannelResolver
     */
    @Nullable
    public static NotificationChannelResolver getResolver() {
        NotificationChannelResolver channelResolver = builderResolver;
        return channelResolver != null ? channelResolver : resolver;
    }

    /**
     * Remembers the ID of a custom sound channel set using withCustomNotificationChannel, so that its channels can be deleted
     * by {@link #deleteSdkDefaultChannels(Context)} even after the application stops setting it.
     */
    public static void rememberCustomSoundChannelId(@NonNull Context context, @Nullable String customSoundChannelBaseId) {
        if (StringUtils.isNotBlank(customSoundChannelBaseId)) {
            PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.CUSTOM_NOTIFICATION_CHANNEL_IDS, customSoundChannelBaseId);
        }
    }

    // endregion

    // region channel creation

    /**
     * Creates the single channel and application channels from config if they don't exist.
     * For existing channels only name and description are updated if they are set, importance, sound and vibration stay under user control.
     * Names of the SDK fallback channels are updated if the channels exist.
     */
    public static void createConfiguredChannels(@NonNull Context context, @Nullable NotificationChannelConfig config) {
        if (Build.VERSION.SDK_INT < 26 || config == null) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        if (config.getSingleChannel() != null) {
            createOrUpdateChannel(context, notificationManager, config.getSingleChannel());
        }
        for (NotificationChannelDefinition definition : config.getChannels()) {
            createOrUpdateChannel(context, notificationManager, definition);
        }

        for (String fallbackChannelId : NotificationChannelRouter.getFallbackChannelIds()) {
            NotificationChannel fallbackChannel = notificationManager.getNotificationChannel(fallbackChannelId);
            if (fallbackChannel != null) {
                fallbackChannel.setName(getFallbackChannelName(context, fallbackChannelId));
                notificationManager.createNotificationChannel(fallbackChannel);
            }
        }
    }

    /**
     * Android 8+ displays notifications as heads-up only in channels with high importance. The single channel is created with
     * default importance unless the application defines it otherwise, because with high importance also notifications displayed
     * while the application is in the foreground would be heads-up, so the application should make that choice.
     *
     * @return true if heads-up notifications are enabled, but the built-in routing posts them to the single channel which doesn't
     * have high importance, for a heads-up message with sound and vibration, without a payload channel, which isn't a chat message
     */
    public static boolean isHeadsUpHiddenBySingleChannel(@NonNull Context context, @Nullable NotificationChannelConfig config, boolean isHeadsUpEnabled) {
        if (Build.VERSION.SDK_INT < 26 || config == null || config.getSingleChannel() == null || !isHeadsUpEnabled) {
            return false;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return false;
        }

        List<String> candidateChannelIds = NotificationChannelRouter.getCandidateChannelIds(config, null, false, true,
                NotificationChannelDefinition.SOUND_DEFAULT, true, null);
        for (String channelId : candidateChannelIds) {
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            if (channel != null) {
                return channelId.equals(config.getSingleChannel().getId()) && channel.getImportance() < NotificationManager.IMPORTANCE_HIGH;
            }
        }
        return false;
    }

    @RequiresApi(26)
    private static void createOrUpdateChannel(@NonNull Context context, @NonNull NotificationManager notificationManager, @Nullable NotificationChannelDefinition definition) {
        if (definition == null || StringUtils.isBlank(definition.getId())) {
            MobileMessagingLogger.w("Notification channel definition without ID is ignored");
            return;
        }

        try {
            CharSequence name = resolveText(context, definition.getNameResource(), definition.getName());
            CharSequence description = resolveText(context, definition.getDescriptionResource(), definition.getDescription());

            NotificationChannel existingChannel = notificationManager.getNotificationChannel(definition.getId());
            if (existingChannel != null) {
                // without name in the definition, e.g. for a channel the application created itself, its name is kept
                if (name != null) {
                    existingChannel.setName(name);
                }
                if (description != null) {
                    existingChannel.setDescription(description.toString());
                }
                notificationManager.createNotificationChannel(existingChannel);
                return;
            }

            if (name == null) {
                name = SoftwareInformation.getAppName(context);
            }
            NotificationChannel channel = new NotificationChannel(definition.getId(), name, toAndroidImportance(definition.getImportance()));
            if (description != null) {
                channel.setDescription(description.toString());
            }
            channel.enableLights(true);
            channel.enableVibration(definition.isVibrationEnabled());
            channel.setShowBadge(definition.isShowBadge());
            setChannelSound(context, channel, definition.getSound());
            notificationManager.createNotificationChannel(channel);
        } catch (Exception e) {
            MobileMessagingLogger.e("Could not create notification channel " + definition.getId(), e);
        }
    }

    @RequiresApi(26)
    private static void setChannelSound(@NonNull Context context, @NonNull NotificationChannel channel, @Nullable String sound) {
        if (sound == null || NotificationChannelDefinition.SOUND_DEFAULT.equals(sound)) {
            return;
        }
        if (NotificationChannelDefinition.SOUND_NONE.equals(sound)) {
            channel.setSound(null, null);
            return;
        }
        if (ResourceLoader.loadResourceByName(context, "raw", sound) <= 0) {
            MobileMessagingLogger.w("Notification sound " + sound + " not found in res/raw, default sound is used for channel " + channel.getId());
            return;
        }
        Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/raw/" + sound);
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build();
        channel.setSound(uri, audioAttributes);
    }

    private static int toAndroidImportance(@Nullable NotificationChannelDefinition.Importance importance) {
        if (importance == null) {
            return NotificationManager.IMPORTANCE_DEFAULT;
        }
        switch (importance) {
            case HIGH:
                return NotificationManager.IMPORTANCE_HIGH;
            case LOW:
                return NotificationManager.IMPORTANCE_LOW;
            case MIN:
                return NotificationManager.IMPORTANCE_MIN;
            case DEFAULT:
            default:
                return NotificationManager.IMPORTANCE_DEFAULT;
        }
    }

    @Nullable
    private static CharSequence resolveText(@NonNull Context context, @Nullable String resourceName, @Nullable String text) {
        if (StringUtils.isNotBlank(resourceName)) {
            String resolved = ResourceLoader.loadStringResourceByName(context, resourceName);
            if (resolved != null) {
                return resolved;
            }
            MobileMessagingLogger.w("String resource " + resourceName + " not found");
        }
        return StringUtils.isNotBlank(text) ? text : null;
    }

    private static String getFallbackChannelName(@NonNull Context context, @NonNull String fallbackChannelId) {
        int resId;
        switch (fallbackChannelId) {
            case NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_VIBRATION:
                resId = R.string.mm_notification_channel_fallback_vibration;
                break;
            case NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_SOUND:
                resId = R.string.mm_notification_channel_fallback_sound;
                break;
            case NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_QUIET:
                resId = R.string.mm_notification_channel_fallback_quiet;
                break;
            case NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID:
                resId = R.string.mm_notification_channel_fallback_high_priority;
                break;
            case NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_VIBRATION:
                resId = R.string.mm_notification_channel_fallback_high_priority_vibration;
                break;
            case NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_SOUND:
                resId = R.string.mm_notification_channel_fallback_high_priority_sound;
                break;
            case NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_QUIET:
                resId = R.string.mm_notification_channel_fallback_high_priority_quiet;
                break;
            default:
                resId = R.string.mm_notification_channel_fallback;
                break;
        }
        return getChannelName(context, resId, SoftwareInformation.getAppName(context));
    }

    /**
     * Resolves name of an SDK channel from string resources, so that applications can override and translate it.
     * Falls back to the application name if the resource can't be resolved or formatted (e.g. invalid format specifier in an application override).
     *
     * @param resId   string resource with the {@code %1$s} placeholder
     * @param appName value for the placeholder
     */
    @NonNull
    public static String getChannelName(@NonNull Context context, @StringRes int resId, @NonNull String appName) {
        try {
            return context.getString(resId, appName);
        } catch (Resources.NotFoundException | IllegalFormatException e) {
            MobileMessagingLogger.e("Could not resolve notification channel name.", e);
            return appName;
        }
    }

    /**
     * Deletes SDK default channels and custom sound channels set using withCustomNotificationChannel, including previously set ones.
     * Channels used by the current config are not deleted. Channels used only by the resolver or by the custom payload key
     * are not known to the SDK and are deleted.
     *
     * @return true if at least one channel was deleted
     */
    public static boolean deleteSdkDefaultChannels(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return false;
        }

        NotificationChannelConfig config = loadConfig(context);
        if (config == null || config.areDefaultChannelsEnabled()) {
            MobileMessagingLogger.w("SDK default notification channels are in use and will not be deleted. Disable them first using withoutDefaultNotificationChannels() or withSingleNotificationChannel()");
            return false;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return false;
        }

        Set<String> customSoundChannelBaseIds = new HashSet<>(Arrays.asList(
                PreferenceHelper.findStringArray(context, MobileMessagingProperty.CUSTOM_NOTIFICATION_CHANNEL_IDS)));
        String currentCustomSoundChannelBaseId = PreferenceHelper.findString(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_ID);
        if (StringUtils.isNotBlank(currentCustomSoundChannelBaseId)) {
            customSoundChannelBaseIds.add(currentCustomSoundChannelBaseId);
        }

        Set<String> channelIds = new LinkedHashSet<>(NotificationChannelRouter.getSdkDefaultChannelIds(null));
        for (String customSoundChannelBaseId : customSoundChannelBaseIds) {
            channelIds.addAll(NotificationChannelRouter.getCustomSoundChannelIds(customSoundChannelBaseId));
        }
        // the application might reuse ID of a former custom sound channel for its own channel
        channelIds.removeAll(getConfiguredChannelIds(config));

        boolean deleted = false;
        for (String channelId : channelIds) {
            if (notificationManager.getNotificationChannel(channelId) != null) {
                notificationManager.deleteNotificationChannel(channelId);
                deleted = true;
            }
        }
        PreferenceHelper.remove(context, MobileMessagingProperty.CUSTOM_NOTIFICATION_CHANNEL_IDS);
        return deleted;
    }

    @NonNull
    private static Set<String> getConfiguredChannelIds(@NonNull NotificationChannelConfig config) {
        Set<String> ids = new HashSet<>();
        if (config.getSingleChannel() != null) {
            ids.add(config.getSingleChannel().getId());
        }
        for (NotificationChannelDefinition definition : config.getChannels()) {
            if (definition != null) {
                ids.add(definition.getId());
            }
        }
        NotificationChannelMapping mapping = config.getMapping();
        if (mapping != null) {
            ids.addAll(Arrays.asList(mapping.getDefaultChannelId(), mapping.getHighPriorityChannelId(),
                    mapping.getSilentChannelId(), mapping.getChatChannelId()));
        }
        ids.remove(null);
        return ids;
    }

    // endregion

    // region channel resolution

    /**
     * Resolves channel for the notification. Without config and resolver, the result is the same as in previous SDK versions
     * and channel existence is not checked. Otherwise, the returned channel is guaranteed to exist: the SDK suggestion is
     * the first existing candidate channel, or an SDK fallback channel if none exists, which is created only if it is used.
     * The resolver receives the suggestion and it is kept if the resolver fails or returns a channel which doesn't exist.
     */
    @NonNull
    static String resolveChannelId(@NonNull Context context,
                                   @NonNull Message message,
                                   boolean isHighPriority,
                                   boolean isInAppChatMessage) {
        String customSoundChannelBaseId = PreferenceHelper.findString(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_ID);
        NotificationChannelConfig config = loadConfig(context);
        NotificationChannelResolver channelResolver = getResolver();

        if (Build.VERSION.SDK_INT < 26 || (config == null && channelResolver == null)) {
            return NotificationChannelRouter.resolveDefaultChannelId(message.getSound(), message.isVibrate(), isHighPriority, customSoundChannelBaseId);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return NotificationChannelRouter.resolveDefaultChannelId(message.getSound(), message.isVibrate(), isHighPriority, customSoundChannelBaseId);
        }

        if (config != null && !config.areDefaultChannelsEnabled()) {
            customSoundChannelBaseId = null;
        }

        String payloadChannelId = getExistingPayloadChannelId(notificationManager, config, message);
        List<String> candidateChannelIds = NotificationChannelRouter.getCandidateChannelIds(config, payloadChannelId, isInAppChatMessage,
                isHighPriority, message.getSound(), message.isVibrate(), customSoundChannelBaseId);
        String suggestedChannelId = getFirstExistingChannelId(notificationManager, candidateChannelIds);
        boolean isFallbackSuggested = suggestedChannelId == null;
        if (isFallbackSuggested) {
            suggestedChannelId = NotificationChannelRouter.getFallbackChannelId(message.getSound(), message.isVibrate(), isHighPriority);
        }

        if (channelResolver != null) {
            boolean hasCustomSound = message.getSound() != null && !message.getSound().equals("default");
            NotificationChannelContext channelContext = new NotificationChannelContext(message, suggestedChannelId, message.getSound() != null,
                    hasCustomSound, message.isVibrate(), isHighPriority, isInAppChatMessage);
            String resolvedChannelId = null;
            try {
                resolvedChannelId = channelResolver.resolveChannelId(channelContext);
            } catch (Throwable t) {
                // also errors, e.g. NoClassDefFoundError after R8 removed a class the resolver uses, the notification must still be displayed
                MobileMessagingLogger.w("NotificationChannelResolver failed, using channel " + suggestedChannelId, t);
            }
            if (resolvedChannelId != null && !resolvedChannelId.equals(suggestedChannelId)) {
                if (channelExists(notificationManager, resolvedChannelId)) {
                    return resolvedChannelId;
                }
                MobileMessagingLogger.w("Notification channel " + resolvedChannelId + " from NotificationChannelResolver doesn't exist, using channel " + suggestedChannelId);
            }
        }

        if (isFallbackSuggested) {
            MobileMessagingLogger.w("Using fallback notification channel " + suggestedChannelId);
            return ensureFallbackChannel(context, notificationManager, message.getSound(), message.isVibrate(), isHighPriority);
        }
        return suggestedChannelId;
    }

    @Nullable
    @RequiresApi(26)
    private static String getFirstExistingChannelId(@NonNull NotificationManager notificationManager, @NonNull List<String> channelIds) {
        for (String channelId : channelIds) {
            if (channelExists(notificationManager, channelId)) {
                return channelId;
            }
            MobileMessagingLogger.w("Notification channel " + channelId + " doesn't exist");
        }
        return null;
    }

    @Nullable
    private static String getExistingPayloadChannelId(@NonNull NotificationManager notificationManager, @Nullable NotificationChannelConfig config, @NonNull Message message) {
        if (config == null || StringUtils.isBlank(config.getPayloadChannelIdKey())) {
            return null;
        }

        JSONObject customPayload = message.getCustomPayload();
        if (customPayload == null) {
            return null;
        }

        Object value = customPayload.opt(config.getPayloadChannelIdKey());
        if (!(value instanceof String) || StringUtils.isBlank((String) value)) {
            return null;
        }

        String channelId = (String) value;
        if (!channelExists(notificationManager, channelId)) {
            MobileMessagingLogger.w("Notification channel " + channelId + " from custom payload doesn't exist and will not be used");
            return null;
        }
        return channelId;
    }

    @RequiresApi(26)
    private static boolean channelExists(@NonNull NotificationManager notificationManager, @Nullable String channelId) {
        return StringUtils.isNotBlank(channelId) && notificationManager.getNotificationChannel(channelId) != null;
    }

    /**
     * Creates the fallback channel for the message's sound, vibration and priority if it doesn't exist yet. Like the matching
     * mm_default_channel variant, it plays the default sound only for messages with sound and vibrates only for messages with
     * vibration, so that the channel settings, which Android 8+ uses instead of the notification ones, don't change how the
     * message alerts. High priority fallback channels have high importance, so that heads-up notifications are displayed as such.
     *
     * @return ID of the fallback channel
     */
    @RequiresApi(26)
    private static String ensureFallbackChannel(@NonNull Context context, @NonNull NotificationManager notificationManager,
                                                @Nullable String sound, boolean isVibrate, boolean isHighPriority) {
        String channelId = NotificationChannelRouter.getFallbackChannelId(sound, isVibrate, isHighPriority);
        if (notificationManager.getNotificationChannel(channelId) == null) {
            int importance = isHighPriority ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, getFallbackChannelName(context, channelId), importance);
            channel.enableLights(true);
            channel.enableVibration(isVibrate);
            if (sound == null) {
                channel.setSound(null, null);
            }
            notificationManager.createNotificationChannel(channel);
        }
        return channelId;
    }

    // endregion
}
