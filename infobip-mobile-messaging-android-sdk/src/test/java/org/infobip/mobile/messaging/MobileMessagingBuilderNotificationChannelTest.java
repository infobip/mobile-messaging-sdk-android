/*
 * MobileMessagingBuilderNotificationChannelTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.notification.NotificationChannelConfig;
import org.infobip.mobile.messaging.notification.NotificationChannelDefinition;
import org.infobip.mobile.messaging.notification.NotificationChannelMapping;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MobileMessagingBuilderNotificationChannelTest {

    private final NotificationChannelConfig config = NotificationChannelConfig.builder()
            .withPayloadChannelIdKey("androidChannelId")
            .withChannelMapping(NotificationChannelMapping.builder().withChatChannelId("chat").build())
            .addChannel(NotificationChannelDefinition.builder("chat").withName("Chat").build())
            .build();

    @Test
    public void withoutConvenienceMethods_shouldUseConfigAsIs() {
        assertSame(config, merge(config, null, false));
        assertNull(merge(null, null, false));
    }

    @Test
    public void singleChannel_shouldBeAppliedOnTopOfConfig() {
        NotificationChannelConfig merged = merge(config, NotificationChannelDefinition.builder("single").withName("Single").build(), false);

        assertEquals("single", merged.getSingleChannel().getId());
        assertFalse(merged.areDefaultChannelsEnabled());
        assertEquals("chat", merged.getMapping().getChatChannelId());
        assertEquals("androidChannelId", merged.getPayloadChannelIdKey());
        assertEquals(1, merged.getChannels().size());
    }

    @Test
    public void singleChannel_shouldReplaceSingleChannelOfConfig() {
        NotificationChannelConfig configWithSingleChannel = NotificationChannelConfig.builder().withSingleChannel("config", "Config").build();

        NotificationChannelConfig merged = merge(configWithSingleChannel, NotificationChannelDefinition.builder("builder").withName("Builder").build(), false);

        assertEquals("builder", merged.getSingleChannel().getId());
    }

    @Test
    public void withoutDefaultChannels_shouldBeAppliedOnTopOfConfig() {
        assertTrue(config.areDefaultChannelsEnabled());

        NotificationChannelConfig merged = merge(config, null, true);

        assertTrue(merged.isWithoutDefaultChannels());
        assertFalse(merged.areDefaultChannelsEnabled());
        assertEquals("chat", merged.getMapping().getChatChannelId());
    }

    @Test
    public void convenienceMethods_shouldWorkWithoutConfig() {
        NotificationChannelConfig merged = merge(null, NotificationChannelDefinition.builder("single").withName("Single").build(), true);

        assertEquals("single", merged.getSingleChannel().getId());
        assertTrue(merged.isWithoutDefaultChannels());
        assertNull(merged.getMapping());
    }

    private static NotificationChannelConfig merge(NotificationChannelConfig config, NotificationChannelDefinition singleChannel, boolean withoutDefaultChannels) {
        return MobileMessaging.Builder.mergeNotificationChannelConfig(config, singleChannel, withoutDefaultChannels);
    }
}
