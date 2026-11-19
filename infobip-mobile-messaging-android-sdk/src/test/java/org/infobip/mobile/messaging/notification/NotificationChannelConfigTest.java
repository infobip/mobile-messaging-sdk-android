/*
 * NotificationChannelConfigTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.notification;

import com.google.gson.Gson;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NotificationChannelConfigTest {

    // same shape as planned for React Native, Flutter and Cordova plugins
    private static final String WRAPPER_JSON = "{\n" +
            "  \"withoutDefaultChannels\": true,\n" +
            "  \"singleChannel\": { \"id\": \"marketing\", \"name\": \"Offers\", \"description\": \"Promotions and offers\" },\n" +
            "  \"mapping\": { \"default\": \"general\", \"highPriority\": \"alerts\", \"silent\": \"silent\", \"chat\": \"chat\" },\n" +
            "  \"payloadChannelIdKey\": \"androidChannelId\",\n" +
            "  \"channels\": [\n" +
            "    { \"id\": \"alerts\", \"name\": \"Alerts\", \"nameResource\": \"channel_alerts\", \"importance\": \"high\", \"sound\": \"default\", \"vibration\": true },\n" +
            "    { \"id\": \"general\", \"name\": \"General\", \"importance\": \"default\", \"sound\": \"none\", \"vibration\": false, \"showBadge\": false }\n" +
            "  ],\n" +
            "  \"unknownKey\": \"ignored\"\n" +
            "}";

    @Test
    public void shouldParseWrapperJson_withJsonSerializer() {
        assertWrapperConfig(new JsonSerializer().deserialize(WRAPPER_JSON, NotificationChannelConfig.class));
    }

    @Test
    public void shouldParseWrapperJson_withGson() {
        assertWrapperConfig(new Gson().fromJson(WRAPPER_JSON, NotificationChannelConfig.class));
    }

    @Test
    public void shouldParseWrapperJson_withFromJson() {
        assertWrapperConfig(NotificationChannelConfig.fromJson(WRAPPER_JSON));
    }

    @Test
    public void shouldRoundTrip_withJsonSerializer() {
        NotificationChannelConfig config = NotificationChannelConfig.fromJson(WRAPPER_JSON);

        String json = new JsonSerializer().serialize(config);

        assertWrapperConfig(new JsonSerializer().deserialize(json, NotificationChannelConfig.class));
    }

    @Test
    public void shouldRoundTrip_withGson() {
        NotificationChannelConfig config = new Gson().fromJson(WRAPPER_JSON, NotificationChannelConfig.class);

        String json = new Gson().toJson(config);

        assertWrapperConfig(new Gson().fromJson(json, NotificationChannelConfig.class));
    }

    @Test
    public void shouldRoundTrip_builderConfig() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withoutDefaultChannels()
                .withSingleChannel("marketing", "Offers", "Promotions and offers")
                .withChannelMapping(NotificationChannelMapping.builder()
                        .withDefaultChannelId("general")
                        .withHighPriorityChannelId("alerts")
                        .withSilentChannelId("silent")
                        .withChatChannelId("chat")
                        .build())
                .withPayloadChannelIdKey("androidChannelId")
                .addChannel(NotificationChannelDefinition.builder("alerts")
                        .withName("Alerts")
                        .withNameResource("channel_alerts")
                        .withImportance(NotificationChannelDefinition.Importance.HIGH)
                        .withSound(NotificationChannelDefinition.SOUND_DEFAULT)
                        .withVibration(true)
                        .build())
                .addChannel(NotificationChannelDefinition.builder("general")
                        .withName("General")
                        .withImportance(NotificationChannelDefinition.Importance.DEFAULT)
                        .withSound(NotificationChannelDefinition.SOUND_NONE)
                        .withVibration(false)
                        .withShowBadge(false)
                        .build())
                .build();

        String json = config.toJson();

        assertTrue(json.contains("\"importance\":\"high\""));
        assertTrue(json.contains("\"highPriority\":\"alerts\""));
        assertWrapperConfig(NotificationChannelConfig.fromJson(json));
    }

    @Test
    public void shouldParseUppercaseImportance() {
        NotificationChannelConfig config = NotificationChannelConfig.fromJson("{\"channels\":[{\"id\":\"a\",\"importance\":\"LOW\"},{\"id\":\"b\",\"importance\":\"min\"}]}");

        assertEquals(NotificationChannelDefinition.Importance.LOW, config.getChannels().get(0).getImportance());
        assertEquals(NotificationChannelDefinition.Importance.MIN, config.getChannels().get(1).getImportance());
    }

    @Test
    public void emptyJson_shouldHaveDefaultsEnabled() {
        NotificationChannelConfig config = NotificationChannelConfig.fromJson("{}");

        assertNotNull(config);
        assertTrue(config.areDefaultChannelsEnabled());
        assertFalse(config.isWithoutDefaultChannels());
        assertNull(config.getSingleChannel());
        assertNull(config.getMapping());
        assertNull(config.getPayloadChannelIdKey());
        assertTrue(config.getChannels().isEmpty());
    }

    @Test
    public void nullJson_shouldReturnNull() {
        assertNull(NotificationChannelConfig.fromJson(null));
    }

    @Test
    public void emptyBuilder_shouldSerializeToEmptyObject() {
        assertEquals("{}", NotificationChannelConfig.builder().build().toJson());
    }

    @Test
    public void definitionWithoutOptionalValues_shouldUseDefaults() {
        NotificationChannelDefinition definition = NotificationChannelConfig.fromJson("{\"channels\":[{\"id\":\"a\"}]}").getChannels().get(0);

        assertEquals("a", definition.getId());
        assertNull(definition.getImportance());
        assertNull(definition.getSound());
        assertTrue(definition.isVibrationEnabled());
        assertTrue(definition.isShowBadge());
    }

    @Test
    public void builderFromConfig_shouldKeepValues() {
        NotificationChannelConfig original = NotificationChannelConfig.builder()
                .withPayloadChannelIdKey("key")
                .addChannel(NotificationChannelDefinition.builder("a").build())
                .build();

        NotificationChannelConfig config = NotificationChannelConfig.builder(original)
                .withSingleChannel("single", "Single")
                .withoutDefaultChannels()
                .build();

        assertEquals("key", config.getPayloadChannelIdKey());
        assertEquals(1, config.getChannels().size());
        assertEquals("single", config.getSingleChannel().getId());
        assertTrue(config.isWithoutDefaultChannels());
    }

    private static void assertWrapperConfig(NotificationChannelConfig config) {
        assertNotNull(config);
        assertTrue(config.isWithoutDefaultChannels());
        assertFalse(config.areDefaultChannelsEnabled());

        assertEquals("marketing", config.getSingleChannel().getId());
        assertEquals("Offers", config.getSingleChannel().getName());
        assertEquals("Promotions and offers", config.getSingleChannel().getDescription());

        assertEquals("general", config.getMapping().getDefaultChannelId());
        assertEquals("alerts", config.getMapping().getHighPriorityChannelId());
        assertEquals("silent", config.getMapping().getSilentChannelId());
        assertEquals("chat", config.getMapping().getChatChannelId());

        assertEquals("androidChannelId", config.getPayloadChannelIdKey());

        assertEquals(2, config.getChannels().size());
        NotificationChannelDefinition alerts = config.getChannels().get(0);
        assertEquals("alerts", alerts.getId());
        assertEquals("Alerts", alerts.getName());
        assertEquals("channel_alerts", alerts.getNameResource());
        assertEquals(NotificationChannelDefinition.Importance.HIGH, alerts.getImportance());
        assertEquals(NotificationChannelDefinition.SOUND_DEFAULT, alerts.getSound());
        assertTrue(alerts.isVibrationEnabled());
        assertTrue(alerts.isShowBadge());

        NotificationChannelDefinition general = config.getChannels().get(1);
        assertEquals("general", general.getId());
        assertEquals(NotificationChannelDefinition.Importance.DEFAULT, general.getImportance());
        assertEquals(NotificationChannelDefinition.SOUND_NONE, general.getSound());
        assertFalse(general.isVibrationEnabled());
        assertFalse(general.isShowBadge());
    }
}
