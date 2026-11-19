/*
 * NotificationChannelResolutionTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.notification;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.resources.R;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.json.JSONObject;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import androidx.core.app.NotificationCompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

/**
 * Uses the real notification manager, channels are created for the test package.
 */
public class NotificationChannelResolutionTest extends MobileMessagingTestCase {

    private static final String PAYLOAD_KEY = "androidChannelId";

    private NotificationManager notificationManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        assumeTrue(Build.VERSION.SDK_INT >= 26);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        deleteAllChannels();
    }

    @Override
    public void tearDown() throws Exception {
        if (notificationManager != null) {
            deleteAllChannels();
        }
        NotificationChannelManager.setResolver(null);
        NotificationChannelManager.setBuilderResolver(null);
        NotificationChannelManager.saveConfig(context, null);
        super.tearDown();
    }

    @Test
    public void noConfig_shouldUseSdkDefaultChannel_evenIfItDoesNotExist() {
        assertEquals(MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID, resolve(message("default", true), true, false));
        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_QUIET, resolve(message(null, false), false, false));
        assertNoFallbackChannel();
    }

    @Test
    public void noConfig_shouldIgnorePayloadChannelId() throws Exception {
        createChannel("payload");
        Message message = message("default", true);
        message.setCustomPayload(new JSONObject().put(PAYLOAD_KEY, "payload"));

        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID, resolve(message, false, false));
    }

    @Test
    public void singleChannel_shouldBeUsedForAllMessages() {
        saveConfig(NotificationChannelConfig.builder().withSingleChannel("single", "Single").build());
        createChannel("single");

        assertEquals("single", resolve(message("default", true), true, false));
        assertEquals("single", resolve(message(null, false), false, true));
    }

    @Test
    public void payloadChannel_shouldBeUsed_whenChannelExists() throws Exception {
        saveConfig(NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single")
                .withPayloadChannelIdKey(PAYLOAD_KEY)
                .build());
        createChannel("single");
        createChannel("payload");
        Message message = message("default", true);
        message.setCustomPayload(new JSONObject().put(PAYLOAD_KEY, "payload"));

        assertEquals("payload", resolve(message, false, false));
    }

    @Test
    public void payloadChannel_shouldBeIgnored_whenChannelDoesNotExist() throws Exception {
        saveConfig(NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single")
                .withPayloadChannelIdKey(PAYLOAD_KEY)
                .build());
        createChannel("single");
        Message message = message("default", true);
        message.setCustomPayload(new JSONObject().put(PAYLOAD_KEY, "not_existing"));

        assertEquals("single", resolve(message, false, false));
    }

    @Test
    public void payloadChannel_shouldBeIgnored_whenValueIsNotString() throws Exception {
        saveConfig(NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single")
                .withPayloadChannelIdKey(PAYLOAD_KEY)
                .build());
        createChannel("single");
        Message message = message("default", true);
        message.setCustomPayload(new JSONObject().put(PAYLOAD_KEY, 42));

        assertEquals("single", resolve(message, false, false));
    }

    @Test
    public void mapping_shouldRouteByMessageType() {
        // not "alerts": a deleted channel is restored with its old settings when created again,
        // which would break the importance check in NotificationChannelCreationTest
        saveConfig(NotificationChannelConfig.builder()
                .withoutDefaultChannels()
                .withChannelMapping(NotificationChannelMapping.builder()
                        .withChatChannelId("chat")
                        .withHighPriorityChannelId("heads_up")
                        .withSilentChannelId("silent")
                        .withDefaultChannelId("general")
                        .build())
                .build());
        createChannel("chat");
        createChannel("heads_up");
        createChannel("silent");
        createChannel("general");

        assertEquals("chat", resolve(message("default", true), true, true));
        assertEquals("heads_up", resolve(message("default", true), true, false));
        assertEquals("silent", resolve(message(null, false), false, false));
        // high priority while the app is in the background
        assertEquals("silent", resolve(message(null, false), true, false));
        assertEquals("general", resolve(message("default", true), false, false));
    }

    @Test
    public void mappingToMissingChannel_shouldUseSingleChannel() {
        saveConfig(NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single")
                .withChannelMapping(NotificationChannelMapping.builder().withChatChannelId("chat").build())
                .build());
        createChannel("single");

        assertEquals("single", resolve(message("default", true), false, true));
    }

    @Test
    public void mappingToMissingChannel_shouldUseNextMatchingMappedChannel() {
        saveConfig(NotificationChannelConfig.builder()
                .withoutDefaultChannels()
                .withChannelMapping(NotificationChannelMapping.builder()
                        .withChatChannelId("chat")
                        .withSilentChannelId("silent")
                        .withHighPriorityChannelId("heads_up")
                        .withDefaultChannelId("general")
                        .build())
                .build());
        createChannel("heads_up");
        createChannel("general");

        assertEquals("general", resolve(message("default", true), false, true));
        assertEquals("heads_up", resolve(message(null, false), true, true));
        assertEquals("general", resolve(message(null, false), false, false));
        assertNoFallbackChannel();
    }

    @Test
    public void mappingWithDefaults_shouldUseSdkDefaultChannel_forUnmappedMessages() {
        saveConfig(NotificationChannelConfig.builder()
                .withChannelMapping(NotificationChannelMapping.builder().withChatChannelId("chat").build())
                .build());
        createChannel(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_VIBRATION);

        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_VIBRATION, resolve(message(null, true), false, false));
    }

    @Test
    public void mappingToMissingChannel_withDefaults_shouldUseSdkDefaultChannel() {
        saveConfig(NotificationChannelConfig.builder()
                .withChannelMapping(NotificationChannelMapping.builder().withChatChannelId("chat").build())
                .build());
        createChannel(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID);

        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID, resolve(message("default", true), false, true));
        assertNoFallbackChannel();
    }

    @Test
    public void resolverReturningUnknownChannel_withMappedChannelMissing_shouldUseSdkDefaultChannel() {
        saveConfig(NotificationChannelConfig.builder()
                .withChannelMapping(NotificationChannelMapping.builder().withDefaultChannelId("general").build())
                .build());
        createChannel(MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID);
        NotificationChannelManager.setResolver(constantResolver("unknown"));

        assertEquals(MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID, resolve(message("default", true), true, false));
        assertNoFallbackChannel();
    }

    @Test
    public void mappingToMissingChannel_withoutDefaults_shouldUseFallbackChannel_evenIfSdkDefaultChannelExists() {
        saveConfig(NotificationChannelConfig.builder()
                .withoutDefaultChannels()
                .withChannelMapping(NotificationChannelMapping.builder().withChatChannelId("chat").build())
                .build());
        createChannel(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID);

        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID, resolve(message("default", true), false, true));
    }

    @Test
    public void withoutDefaultChannels_andNothingMapped_shouldCreateAndUseFallbackChannel() {
        saveConfig(NotificationChannelConfig.builder().withoutDefaultChannels().build());

        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID, resolve(message("default", true), false, false));

        assertOnlyFallbackChannel(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT, true, true);
    }

    @Test
    public void withoutDefaultChannels_andNothingMapped_vibrationOnlyMessage_shouldCreateAndUseFallbackChannelWithoutSound() {
        saveConfig(NotificationChannelConfig.builder().withoutDefaultChannels().build());

        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_VIBRATION, resolve(message(null, true), false, false));

        assertOnlyFallbackChannel(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_VIBRATION, NotificationManager.IMPORTANCE_DEFAULT, false, true);
    }

    @Test
    public void withoutDefaultChannels_andNothingMapped_soundOnlyMessage_shouldCreateAndUseFallbackChannelWithoutVibration() {
        saveConfig(NotificationChannelConfig.builder().withoutDefaultChannels().build());

        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_SOUND, resolve(message("default", false), false, false));

        assertOnlyFallbackChannel(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_SOUND, NotificationManager.IMPORTANCE_DEFAULT, true, false);
    }

    @Test
    public void withoutDefaultChannels_andNothingMapped_silentMessage_shouldCreateAndUseQuietFallbackChannel() {
        saveConfig(NotificationChannelConfig.builder().withoutDefaultChannels().build());

        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_QUIET, resolve(message(null, false), false, false));

        assertOnlyFallbackChannel(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_QUIET, NotificationManager.IMPORTANCE_DEFAULT, false, false);
    }

    @Test
    public void withoutDefaultChannels_andNothingMapped_headsUpMessage_shouldCreateAndUseHighPriorityFallbackChannel() {
        saveConfig(NotificationChannelConfig.builder().withoutDefaultChannels().build());

        assertEquals(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID, resolve(message("default", true), true, false));

        assertOnlyFallbackChannel(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH, true, true);
    }

    @Test
    public void withoutDefaultChannels_andNothingMapped_vibrationOnlyHeadsUpMessage_shouldCreateAndUseHighPriorityFallbackChannelWithoutSound() {
        saveConfig(NotificationChannelConfig.builder().withoutDefaultChannels().build());

        assertEquals(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_VIBRATION, resolve(message(null, true), true, false));

        assertOnlyFallbackChannel(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_VIBRATION, NotificationManager.IMPORTANCE_HIGH, false, true);
    }

    @Test
    public void withoutDefaultChannels_andNothingMapped_soundOnlyHeadsUpMessage_shouldCreateAndUseHighPriorityFallbackChannelWithoutVibration() {
        saveConfig(NotificationChannelConfig.builder().withoutDefaultChannels().build());

        assertEquals(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_SOUND, resolve(message("default", false), true, false));

        assertOnlyFallbackChannel(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_SOUND, NotificationManager.IMPORTANCE_HIGH, true, false);
    }

    @Test
    public void withoutDefaultChannels_andNothingMapped_silentHeadsUpMessage_shouldCreateAndUseQuietHighPriorityFallbackChannel() {
        saveConfig(NotificationChannelConfig.builder().withoutDefaultChannels().build());

        assertEquals(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_QUIET, resolve(message(null, false), true, false));

        assertOnlyFallbackChannel(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_QUIET, NotificationManager.IMPORTANCE_HIGH, false, false);
    }

    @Test
    public void createConfiguredChannels_shouldUpdateNamesOfExistingFallbackChannels() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withoutDefaultChannels().build();
        saveConfig(config);
        for (String sound : new String[]{"default", null}) {
            for (boolean vibrate : new boolean[]{true, false}) {
                for (boolean highPriority : new boolean[]{true, false}) {
                    resolve(message(sound, vibrate), highPriority, false);
                }
            }
        }
        for (String id : NotificationChannelRouter.getFallbackChannelIds()) {
            NotificationChannel channel = notificationManager.getNotificationChannel(id);
            channel.setName("Old name");
            notificationManager.createNotificationChannel(channel);
        }

        NotificationChannelManager.createConfiguredChannels(context, config);

        assertChannelName(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID, R.string.mm_notification_channel_fallback);
        assertChannelName(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_VIBRATION, R.string.mm_notification_channel_fallback_vibration);
        assertChannelName(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_SOUND, R.string.mm_notification_channel_fallback_sound);
        assertChannelName(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_QUIET, R.string.mm_notification_channel_fallback_quiet);
        assertChannelName(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID, R.string.mm_notification_channel_fallback_high_priority);
        assertChannelName(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_VIBRATION, R.string.mm_notification_channel_fallback_high_priority_vibration);
        assertChannelName(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_SOUND, R.string.mm_notification_channel_fallback_high_priority_sound);
        assertChannelName(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_QUIET, R.string.mm_notification_channel_fallback_high_priority_quiet);
    }

    @Test
    public void builderResolver_shouldTakePrecedenceOverStaticResolver() {
        createChannel("static");
        createChannel("builder");
        NotificationChannelManager.setResolver(constantResolver("static"));
        NotificationChannelManager.setBuilderResolver(constantResolver("builder"));

        assertEquals("builder", resolve(message("default", true), false, false));
    }

    @Test
    public void rebuildWithoutResolver_shouldRemoveBuilderResolver_butKeepStaticResolver() {
        createChannel("static");
        createChannel("builder");
        NotificationChannelManager.setResolver(constantResolver("static"));
        NotificationChannelManager.setBuilderResolver(constantResolver("builder"));

        // what MobileMessaging.Builder.build() does when withNotificationChannelResolver wasn't called
        NotificationChannelManager.setBuilderResolver(null);

        assertEquals("static", resolve(message("default", true), false, false));
    }

    @Test
    public void rebuildWithoutResolver_shouldUseSdkChannel_whenNoStaticResolver() {
        createChannel("builder");
        NotificationChannelManager.setBuilderResolver(constantResolver("builder"));

        NotificationChannelManager.setBuilderResolver(null);

        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID, resolve(message("default", true), false, false));
    }

    @Test
    public void resolver_shouldReceiveSdkSuggestedChannel_andItsResultShouldBeUsed() {
        createChannel(MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_SOUND);
        createChannel("resolved");
        final AtomicReference<NotificationChannelContext> captured = new AtomicReference<>();
        NotificationChannelManager.setResolver(new NotificationChannelResolver() {
            @Override
            public String resolveChannelId(NotificationChannelContext channelContext) {
                captured.set(channelContext);
                return "resolved";
            }
        });
        Message message = message("ding", false);

        assertEquals("resolved", resolve(message, true, false));

        NotificationChannelContext channelContext = captured.get();
        assertNotNull(channelContext);
        assertEquals(MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_SOUND, channelContext.getSdkSuggestedChannelId());
        assertEquals(message, channelContext.getMessage());
        assertEquals("ding", channelContext.getSound());
        assertEquals(true, channelContext.hasSound());
        assertEquals(true, channelContext.hasCustomSound());
        assertEquals(false, channelContext.isVibrationEnabled());
        assertEquals(true, channelContext.isHighPriority());
        assertEquals(false, channelContext.isInAppChatMessage());
    }

    @Test
    public void resolverReturningUnknownChannel_shouldUseSdkSuggestedChannel() {
        createChannel(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID);
        NotificationChannelManager.setResolver(constantResolver("unknown"));

        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID, resolve(message("default", true), false, false));
    }

    @Test
    public void resolverReturningBlankChannel_shouldUseSingleChannel() {
        saveConfig(NotificationChannelConfig.builder().withSingleChannel("single", "Single").build());
        createChannel("single");
        NotificationChannelManager.setResolver(constantResolver(""));

        assertEquals("single", resolve(message("default", true), false, false));
    }

    @Test
    public void resolver_shouldReceiveExistingChannel_whenMappedChannelIsMissing() {
        saveConfig(NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single")
                .withChannelMapping(NotificationChannelMapping.builder().withChatChannelId("chat").build())
                .build());
        createChannel("single");
        AtomicReference<String> suggested = new AtomicReference<>();
        NotificationChannelManager.setResolver(suggestionCapturingResolver(suggested, null));

        assertEquals("single", resolve(message("default", true), false, true));
        assertEquals("single", suggested.get());

        NotificationChannelManager.setResolver(constantResolver("unknown"));
        assertEquals("single", resolve(message("default", true), false, true));

        NotificationChannelManager.setResolver(throwingResolver());
        assertEquals("single", resolve(message("default", true), false, true));
    }

    @Test
    public void resolver_shouldReceiveFallbackChannel_whenNoChannelExists_whichIsCreatedOnlyIfUsed() {
        saveConfig(NotificationChannelConfig.builder().withoutDefaultChannels().build());
        createChannel("resolved");
        AtomicReference<String> suggested = new AtomicReference<>();
        NotificationChannelManager.setResolver(suggestionCapturingResolver(suggested, "resolved"));

        assertEquals("resolved", resolve(message("default", true), false, false));
        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID, suggested.get());
        assertNoFallbackChannel();

        NotificationChannelManager.setResolver(suggestionCapturingResolver(suggested, null));

        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID, resolve(message("default", true), false, false));
        assertNotNull(notificationManager.getNotificationChannel(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID));
    }

    @Test
    public void resolver_shouldReceiveHighPriorityFallbackChannel_forHeadsUpMessage_whichIsCreatedOnlyIfUsed() {
        saveConfig(NotificationChannelConfig.builder().withoutDefaultChannels().build());
        createChannel("resolved");
        AtomicReference<String> suggested = new AtomicReference<>();
        NotificationChannelManager.setResolver(suggestionCapturingResolver(suggested, "resolved"));

        assertEquals("resolved", resolve(message("default", true), true, false));
        assertEquals(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID, suggested.get());
        assertNoFallbackChannel();

        NotificationChannelManager.setResolver(suggestionCapturingResolver(suggested, null));

        assertEquals(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID, resolve(message("default", true), true, false));
        assertOnlyFallbackChannel(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH, true, true);
    }

    @Test
    public void throwingResolver_shouldUseSdkSuggestedChannel() {
        createChannel(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID);
        NotificationChannelManager.setResolver(throwingResolver());

        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID, resolve(message("default", true), false, false));
    }

    @Test
    public void resolverThrowingError_shouldUseSdkSuggestedChannel() {
        createChannel(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID);
        NotificationChannelManager.setResolver(new NotificationChannelResolver() {
            @Override
            public String resolveChannelId(NotificationChannelContext channelContext) {
                // e.g. after R8 removed a class the resolver uses
                throw new NoClassDefFoundError("test");
            }
        });

        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID, resolve(message("default", true), false, false));
    }

    @Test
    public void resolverReturningUnknownChannel_withoutAnyExistingChannel_shouldUseFallbackChannel() {
        NotificationChannelManager.setResolver(constantResolver("unknown"));

        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID, resolve(message("default", true), false, false));
        assertNotNull(notificationManager.getNotificationChannel(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID));
    }

    @Test
    public void notificationHandler_shouldUseResolvedChannel() throws Exception {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, true);
        PreferenceHelper.saveClass(context, MobileMessagingProperty.CALLBACK_ACTIVITY, Activity.class);
        saveConfig(NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single")
                .withChannelMapping(NotificationChannelMapping.builder().withChatChannelId("chat").build())
                .build());
        createChannel("single");
        createChannel("chat");
        Message chatMessage = message("default", true);
        chatMessage.setBody("SomeBody");
        chatMessage.setMessageType(Message.MESSAGE_TYPE_CHAT);
        Message message = message("default", true);
        message.setBody("SomeBody");

        BaseNotificationHandler notificationHandler = new BaseNotificationHandler(context);

        assertEquals("chat", getChannelId(notificationHandler.createNotificationCompatBuilder(chatMessage)));
        assertEquals("single", getChannelId(notificationHandler.createNotificationCompatBuilder(message)));
    }

    private String resolve(Message message, boolean isHighPriority, boolean isInAppChatMessage) {
        return NotificationChannelManager.resolveChannelId(context, message, isHighPriority, isInAppChatMessage);
    }

    private void saveConfig(NotificationChannelConfig config) {
        NotificationChannelManager.saveConfig(context, config);
    }

    private void createChannel(String id) {
        notificationManager.createNotificationChannel(new NotificationChannel(id, id, NotificationManager.IMPORTANCE_DEFAULT));
    }

    private void assertNoFallbackChannel() {
        for (String fallbackChannelId : NotificationChannelRouter.getFallbackChannelIds()) {
            assertNull(fallbackChannelId, notificationManager.getNotificationChannel(fallbackChannelId));
        }
    }

    /**
     * Asserts that the given fallback channel has the expected settings and that no other fallback channel was created.
     */
    private void assertOnlyFallbackChannel(String expectedChannelId, int expectedImportance, boolean expectedSound, boolean expectedVibration) {
        for (String fallbackChannelId : NotificationChannelRouter.getFallbackChannelIds()) {
            NotificationChannel channel = notificationManager.getNotificationChannel(fallbackChannelId);
            if (!fallbackChannelId.equals(expectedChannelId)) {
                assertNull(fallbackChannelId, channel);
                continue;
            }
            assertNotNull(channel);
            assertEquals(expectedImportance, channel.getImportance());
            assertEquals(expectedSound ? Settings.System.DEFAULT_NOTIFICATION_URI : null, channel.getSound());
            assertEquals(expectedVibration, channel.shouldVibrate());
        }
    }

    private void assertChannelName(String channelId, int nameResId) {
        assertEquals(context.getString(nameResId, SoftwareInformation.getAppName(context)),
                notificationManager.getNotificationChannel(channelId).getName().toString());
    }

    private void deleteAllChannels() {
        for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
            notificationManager.deleteNotificationChannel(channel.getId());
        }
    }

    private static Message message(String sound, boolean vibrate) {
        Message message = new Message();
        message.setSound(sound);
        message.setVibrate(vibrate);
        return message;
    }

    private static NotificationChannelResolver constantResolver(final String channelId) {
        return new NotificationChannelResolver() {
            @Override
            public String resolveChannelId(NotificationChannelContext channelContext) {
                return channelId;
            }
        };
    }

    private static NotificationChannelResolver throwingResolver() {
        return new NotificationChannelResolver() {
            @Override
            public String resolveChannelId(NotificationChannelContext channelContext) {
                throw new IllegalStateException("test");
            }
        };
    }

    /**
     * @param channelId channel to return, null to keep the SDK suggestion
     */
    private static NotificationChannelResolver suggestionCapturingResolver(final AtomicReference<String> suggested, final String channelId) {
        return new NotificationChannelResolver() {
            @Override
            public String resolveChannelId(NotificationChannelContext channelContext) {
                suggested.set(channelContext.getSdkSuggestedChannelId());
                return channelId != null ? channelId : channelContext.getSdkSuggestedChannelId();
            }
        };
    }

    // getting via reflection due to API issues on different android versions
    private static String getChannelId(NotificationCompat.Builder builder) throws Exception {
        Field field = NotificationCompat.Builder.class.getDeclaredField("mChannelId");
        field.setAccessible(true);
        return (String) field.get(builder);
    }
}
