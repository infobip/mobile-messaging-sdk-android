/*
 * NotificationChannelRouterTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.notification;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotificationChannelRouterTest {

    private static final String SOUND_DEFAULT = "default";
    private static final String SOUND_CUSTOM = "ding";

    private final NotificationChannelMapping fullMapping = NotificationChannelMapping.builder()
            .withChatChannelId("chat")
            .withHighPriorityChannelId("alerts")
            .withSilentChannelId("silent")
            .withDefaultChannelId("general")
            .build();

    // region no config - must be identical to previous SDK versions

    @Test
    public void noConfig_shouldRouteToDefaultChannels() {
        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID, resolve(null, null, false, false, SOUND_DEFAULT, true, null));
        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_SOUND, resolve(null, null, false, false, SOUND_DEFAULT, false, null));
        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_VIBRATION, resolve(null, null, false, false, null, true, null));
        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_QUIET, resolve(null, null, false, false, null, false, null));
        assertEquals(MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID, resolve(null, null, false, true, SOUND_DEFAULT, true, null));
        assertEquals(MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_SOUND, resolve(null, null, false, true, SOUND_DEFAULT, false, null));
        assertEquals(MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_VIBRATION, resolve(null, null, false, true, null, true, null));
        assertEquals(MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_QUIET, resolve(null, null, false, true, null, false, null));
    }

    @Test
    public void noConfig_shouldRouteToCustomSoundChannels() {
        assertEquals("custom", resolve(null, null, false, false, SOUND_CUSTOM, false, "custom"));
        assertEquals("custom_vibration", resolve(null, null, false, false, SOUND_CUSTOM, true, "custom"));
        assertEquals("custom_high_priority", resolve(null, null, false, true, SOUND_CUSTOM, false, "custom"));
        assertEquals("custom_high_priority, vibration", resolve(null, null, false, true, SOUND_CUSTOM, true, "custom"));
    }

    @Test
    public void noConfig_customSoundWithoutCustomChannel_shouldRouteToDefaultChannel() {
        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID, resolve(null, null, false, false, SOUND_CUSTOM, true, null));
    }

    @Test
    public void noConfig_defaultSoundWithCustomChannel_shouldRouteToDefaultChannel() {
        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID, resolve(null, null, false, false, SOUND_DEFAULT, true, "custom"));
    }

    @Test
    public void noConfig_shouldIgnorePayloadChannelAndChat() {
        assertEquals(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID, resolve(null, "payload", true, false, SOUND_DEFAULT, true, null));
    }

    // endregion

    // region resolution order

    @Test
    public void step1_payloadChannel_shouldWinOverEverything() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withPayloadChannelIdKey("androidChannelId")
                .withChannelMapping(fullMapping)
                .withSingleChannel("single", "Single")
                .build();

        assertEquals("payload", resolve(config, "payload", true, true, null, false, null));
    }

    @Test
    public void step1_blankPayloadChannel_shouldBeIgnored() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withChannelMapping(fullMapping).build();

        assertEquals("general", resolve(config, " ", false, false, SOUND_DEFAULT, true, null));
    }

    @Test
    public void step2_chat_shouldUseChatChannel() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withChannelMapping(fullMapping).build();

        assertEquals("chat", resolve(config, null, true, true, null, false, null));
    }

    @Test
    public void step3_silent_shouldUseSilentChannel() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withChannelMapping(fullMapping).build();

        assertEquals("silent", resolve(config, null, false, false, null, false, null));
    }

    @Test
    public void step3_silentHighPriority_shouldUseSilentChannel() {
        // with heads-up enabled, every notification displayed while the app is in the background is high priority
        NotificationChannelConfig config = NotificationChannelConfig.builder().withChannelMapping(fullMapping).build();

        assertEquals("silent", resolve(config, null, false, true, null, false, null));
    }

    @Test
    public void step3_vibrateOnly_shouldNotUseSilentChannel() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withChannelMapping(fullMapping).build();

        assertEquals("general", resolve(config, null, false, false, null, true, null));
        assertEquals("alerts", resolve(config, null, false, true, null, true, null));
    }

    @Test
    public void step4_highPriority_shouldUseHighPriorityChannel() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withChannelMapping(fullMapping).build();

        assertEquals("alerts", resolve(config, null, false, true, SOUND_DEFAULT, true, null));
    }

    @Test
    public void step4_silentHighPriority_withoutSilentMapping_shouldUseHighPriorityChannel() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withChannelMapping(NotificationChannelMapping.builder()
                        .withHighPriorityChannelId("alerts")
                        .withDefaultChannelId("general")
                        .build())
                .build();

        assertEquals("alerts", resolve(config, null, false, true, null, false, null));
    }

    @Test
    public void step5_default_shouldUseDefaultChannel() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withChannelMapping(fullMapping).build();

        assertEquals("general", resolve(config, null, false, false, SOUND_DEFAULT, true, null));
    }

    @Test
    public void step2to5_missingMappingEntries_shouldBeSkipped() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withChannelMapping(NotificationChannelMapping.builder().withDefaultChannelId("general").build())
                .build();

        assertEquals("general", resolve(config, null, true, true, null, false, null));
    }

    @Test
    public void step6_singleChannel_shouldBeUsedWhenMappingDoesNotMatch() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single")
                .withChannelMapping(NotificationChannelMapping.builder().withChatChannelId("chat").build())
                .build();

        assertEquals("single", resolve(config, null, false, true, SOUND_CUSTOM, true, "custom"));
        assertEquals("chat", resolve(config, null, true, true, SOUND_CUSTOM, true, "custom"));
    }

    @Test
    public void step7_defaultChannelsEnabled_shouldUseLegacyRouting() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withChannelMapping(NotificationChannelMapping.builder().withChatChannelId("chat").build())
                .build();

        assertEquals(MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID_QUIET, resolve(config, null, false, true, null, false, null));
        assertEquals("custom_vibration", resolve(config, null, false, false, SOUND_CUSTOM, true, "custom"));
    }

    @Test
    public void step8_withoutDefaultChannels_shouldUseFallbackChannelWithMessageSoundAndVibration() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withoutDefaultChannels().build();

        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID, resolve(config, null, false, false, SOUND_DEFAULT, true, null));
        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_SOUND, resolve(config, null, false, false, SOUND_DEFAULT, false, null));
        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_VIBRATION, resolve(config, null, false, false, null, true, null));
        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_QUIET, resolve(config, null, false, false, null, false, null));
        // fallback channels can't play a custom sound, the default one is played instead
        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID, resolve(config, null, false, false, SOUND_CUSTOM, true, null));
        assertEquals(NotificationChannelRouter.MM_FALLBACK_CHANNEL_ID_SOUND, resolve(config, null, false, false, SOUND_CUSTOM, false, null));
    }

    @Test
    public void step8_withoutDefaultChannels_highPriority_shouldUseHighPriorityFallbackChannelWithMessageSoundAndVibration() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withoutDefaultChannels().build();

        assertEquals(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID, resolve(config, null, false, true, SOUND_DEFAULT, true, null));
        assertEquals(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_SOUND, resolve(config, null, false, true, SOUND_DEFAULT, false, null));
        assertEquals(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_VIBRATION, resolve(config, null, false, true, null, true, null));
        assertEquals(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID_QUIET, resolve(config, null, false, true, null, false, null));
        assertEquals(NotificationChannelRouter.MM_FALLBACK_HIGH_PRIORITY_CHANNEL_ID, resolve(config, null, true, true, SOUND_CUSTOM, true, null));
    }

    // endregion

    // region candidates - the first existing one is used

    @Test
    public void candidates_shouldContainAllMatchingChannelsInResolutionOrder() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withPayloadChannelIdKey("androidChannelId")
                .withChannelMapping(fullMapping)
                .withSingleChannel("single", "Single")
                .build();

        assertEquals(Arrays.asList("payload", "chat", "silent", "alerts", "general", "single"),
                candidates(config, "payload", true, true, null, false, null));
        assertEquals(Arrays.asList("chat", "general", "single"),
                candidates(config, null, true, false, SOUND_DEFAULT, true, null));
    }

    @Test
    public void candidates_withDefaultChannelsEnabled_shouldEndWithSdkDefaultChannel() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withChannelMapping(fullMapping).build();

        assertEquals(Arrays.asList("alerts", "general", MobileMessagingCore.MM_DEFAULT_HIGH_PRIORITY_CHANNEL_ID),
                candidates(config, null, false, true, SOUND_DEFAULT, true, null));
        assertEquals(Arrays.asList("general", "custom_vibration"),
                candidates(config, null, false, false, SOUND_CUSTOM, true, "custom"));
    }

    @Test
    public void candidates_shouldNotContainDuplicates() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withChannelMapping(NotificationChannelMapping.builder()
                        .withChatChannelId("general")
                        .withDefaultChannelId("general")
                        .build())
                .withSingleChannel("general", "General")
                .build();

        assertEquals(Collections.singletonList("general"), candidates(config, "general", true, false, SOUND_DEFAULT, true, null));
    }

    @Test
    public void candidates_withoutDefaultChannels_andNothingMapped_shouldBeEmpty() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withoutDefaultChannels().build();

        assertTrue(candidates(config, null, false, true, SOUND_DEFAULT, true, null).isEmpty());
    }

    @Test
    public void candidates_noConfig_shouldContainOnlySdkDefaultChannel() {
        assertEquals(Collections.singletonList(MobileMessagingCore.MM_DEFAULT_CHANNEL_ID_QUIET),
                candidates(null, "payload", true, false, null, false, null));
    }

    // endregion

    @Test
    public void config_defaultChannelsEnabled() {
        assertTrue(NotificationChannelConfig.builder().build().areDefaultChannelsEnabled());
        assertTrue(NotificationChannelConfig.builder().withChannelMapping(fullMapping).build().areDefaultChannelsEnabled());
        assertFalse(NotificationChannelConfig.builder().withoutDefaultChannels().build().areDefaultChannelsEnabled());
        assertFalse(NotificationChannelConfig.builder().withSingleChannel("single", "Single").build().areDefaultChannelsEnabled());
    }

    @Test
    public void sdkDefaultChannelIds_shouldContainOnlySdkChannels() {
        List<String> withoutCustom = NotificationChannelRouter.getSdkDefaultChannelIds(null);
        List<String> withCustom = NotificationChannelRouter.getSdkDefaultChannelIds("custom");

        assertEquals(8, withoutCustom.size());
        for (String id : withoutCustom) {
            assertTrue(id.startsWith("mm_default_channel"));
        }
        assertEquals(12, withCustom.size());
        assertTrue(withCustom.contains("custom"));
        assertTrue(withCustom.contains("custom_vibration"));
        assertTrue(withCustom.contains("custom_high_priority"));
        assertTrue(withCustom.contains("custom_high_priority, vibration"));
        for (String fallbackChannelId : NotificationChannelRouter.getFallbackChannelIds()) {
            assertFalse(withCustom.contains(fallbackChannelId));
        }
    }

    @Test
    public void fallbackChannelIds_shouldContainOneChannelForEachSoundVibrationAndPriority() {
        Set<String> resolvedFallbackChannelIds = new HashSet<>();
        for (String sound : Arrays.asList(SOUND_DEFAULT, null)) {
            for (boolean isVibrate : new boolean[]{true, false}) {
                for (boolean isHighPriority : new boolean[]{true, false}) {
                    resolvedFallbackChannelIds.add(NotificationChannelRouter.getFallbackChannelId(sound, isVibrate, isHighPriority));
                }
            }
        }

        assertEquals(8, NotificationChannelRouter.getFallbackChannelIds().size());
        assertEquals(new HashSet<>(NotificationChannelRouter.getFallbackChannelIds()), resolvedFallbackChannelIds);
    }

    @Test
    public void customSoundChannelIds_shouldBeEmpty_whenNotSet() {
        assertTrue(NotificationChannelRouter.getCustomSoundChannelIds(null).isEmpty());
        assertTrue(NotificationChannelRouter.getCustomSoundChannelIds(" ").isEmpty());
        assertEquals(4, NotificationChannelRouter.getCustomSoundChannelIds("custom").size());
    }

    private static String resolve(NotificationChannelConfig config, String payloadChannelId, boolean chat, boolean highPriority,
                                  String sound, boolean vibrate, String customSoundChannelBaseId) {
        return NotificationChannelRouter.resolveChannelId(config, payloadChannelId, chat, highPriority, sound, vibrate, customSoundChannelBaseId);
    }

    private static List<String> candidates(NotificationChannelConfig config, String payloadChannelId, boolean chat, boolean highPriority,
                                           String sound, boolean vibrate, String customSoundChannelBaseId) {
        return NotificationChannelRouter.getCandidateChannelIds(config, payloadChannelId, chat, highPriority, sound, vibrate, customSoundChannelBaseId);
    }
}
