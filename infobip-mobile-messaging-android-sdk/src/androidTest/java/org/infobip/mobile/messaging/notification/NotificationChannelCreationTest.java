/*
 * NotificationChannelCreationTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.MobileMessagingTestable;
import org.infobip.mobile.messaging.resources.R;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Uses the real notification manager, channels are created for the test package.
 */
public class NotificationChannelCreationTest extends MobileMessagingTestCase {

    private static final String CUSTOM_CHANNEL_ID = "custom";
    private static final String TEST_SOUND = "mm_test_sound";

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
        super.tearDown();
    }

    @Test
    public void noConfig_shouldCreateSdkDefaultChannels() {
        createCore();

        assertEquals(new HashSet<>(NotificationChannelRouter.getSdkDefaultChannelIds(null)), getChannelIds());
    }

    @Test
    public void noConfig_withCustomSound_shouldCreateCustomHighPriorityChannels() {
        // initCustomChannels reads the notification settings itself, it doesn't rely on initDefaultChannels initializing the field first
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.HEADSUP_NOTIFICATIONS_ENABLED, true);
        setCustomNotificationChannel();

        createCore();

        assertEquals(new HashSet<>(NotificationChannelRouter.getSdkDefaultChannelIds(CUSTOM_CHANNEL_ID)), getChannelIds());
        assertEquals(NotificationManager.IMPORTANCE_HIGH, notificationManager.getNotificationChannel(CUSTOM_CHANNEL_ID + "_high_priority").getImportance());
        assertEquals(NotificationManager.IMPORTANCE_HIGH, notificationManager.getNotificationChannel(CUSTOM_CHANNEL_ID + "_high_priority, vibration").getImportance());
    }

    @Test
    public void headsUpDisabled_shouldNotCreateHighPriorityChannels() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.HEADSUP_NOTIFICATIONS_ENABLED, false);
        setCustomNotificationChannel();

        createCore();

        Set<String> ids = getChannelIds();
        for (String id : ids) {
            assertFalse(id, id.contains("high_priority"));
        }
        assertTrue(ids.containsAll(Arrays.asList(CUSTOM_CHANNEL_ID, CUSTOM_CHANNEL_ID + "_vibration")));
    }

    @Test
    public void singleChannel_shouldCreateOnlySingleChannel() {
        setCustomNotificationChannel();
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single", "Single description")
                .build());

        createCore();

        assertEquals(new HashSet<>(Arrays.asList("single")), getChannelIds());
        NotificationChannel channel = notificationManager.getNotificationChannel("single");
        assertEquals("Single", channel.getName().toString());
        assertEquals("Single description", channel.getDescription());
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.getImportance());
    }

    @Test
    public void singleChannelWithDefaultImportance_shouldHideHeadsUp_onlyIfHeadsUpIsEnabled() {
        NotificationChannelConfig config = NotificationChannelConfig.builder().withSingleChannel("single", "Single").build();
        NotificationChannelManager.saveConfig(context, config);

        createCore();

        assertTrue(NotificationChannelManager.isHeadsUpHiddenBySingleChannel(context, config, true));
        assertFalse(NotificationChannelManager.isHeadsUpHiddenBySingleChannel(context, config, false));
    }

    @Test
    public void singleChannelWithHighImportance_shouldNotHideHeadsUp() {
        // own ID, Android restores the importance of a deleted channel which other tests create with default importance
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withSingleChannel(NotificationChannelDefinition.builder("single_heads_up")
                        .withName("Single")
                        .withImportance(NotificationChannelDefinition.Importance.HIGH)
                        .build())
                .build();
        NotificationChannelManager.saveConfig(context, config);

        createCore();

        assertFalse(NotificationChannelManager.isHeadsUpHiddenBySingleChannel(context, config, true));
    }

    @Test
    public void singleChannelWithDefaultImportance_shouldNotHideHeadsUp_whenMappedHighPriorityChannelExists() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single")
                .withChannelMapping(NotificationChannelMapping.builder().withHighPriorityChannelId("alerts").build())
                .addChannel(NotificationChannelDefinition.builder("alerts")
                        .withName("Alerts")
                        .withImportance(NotificationChannelDefinition.Importance.HIGH)
                        .build())
                .build();
        NotificationChannelManager.saveConfig(context, config);

        createCore();

        assertFalse(NotificationChannelManager.isHeadsUpHiddenBySingleChannel(context, config, true));
    }

    @Test
    public void singleChannelWithDefaultImportance_shouldHideHeadsUp_whenMappedHighPriorityChannelDoesNotExist() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single")
                .withChannelMapping(NotificationChannelMapping.builder().withHighPriorityChannelId("not_created").build())
                .build();
        NotificationChannelManager.saveConfig(context, config);

        createCore();

        assertTrue(NotificationChannelManager.isHeadsUpHiddenBySingleChannel(context, config, true));
    }

    @Test
    public void singleChannelWithDefaultImportance_shouldNotHideHeadsUp_whenMappedDefaultChannelExists() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single")
                .withChannelMapping(NotificationChannelMapping.builder().withDefaultChannelId("general_mapped").build())
                .addChannel(NotificationChannelDefinition.builder("general_mapped").withName("General").build())
                .build();
        NotificationChannelManager.saveConfig(context, config);

        createCore();

        assertFalse(NotificationChannelManager.isHeadsUpHiddenBySingleChannel(context, config, true));
    }

    @Test
    public void singleChannelWithDefaultImportance_shouldHideHeadsUp_whenMappedDefaultChannelIsTheSingleChannel() {
        NotificationChannelConfig config = NotificationChannelConfig.builder()
                .withSingleChannel("single", "Single")
                .withChannelMapping(NotificationChannelMapping.builder().withDefaultChannelId("single").build())
                .build();
        NotificationChannelManager.saveConfig(context, config);

        createCore();

        assertTrue(NotificationChannelManager.isHeadsUpHiddenBySingleChannel(context, config, true));
    }

    @Test
    public void withoutDefaultChannels_shouldCreateNoChannels() {
        setCustomNotificationChannel();
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder().withoutDefaultChannels().build());

        createCore();

        assertTrue(getChannelIds().isEmpty());
    }

    @Test
    public void mappingOnly_shouldStillCreateSdkDefaultChannels() {
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder()
                .withChannelMapping(NotificationChannelMapping.builder().withChatChannelId("chat").build())
                .build());

        createCore();

        assertEquals(new HashSet<>(NotificationChannelRouter.getSdkDefaultChannelIds(null)), getChannelIds());
    }

    @Test
    public void addChannel_shouldCreateChannelsWithDefinedProperties() {
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder()
                .withoutDefaultChannels()
                .addChannel(NotificationChannelDefinition.builder("alerts")
                        .withName("Alerts")
                        .withImportance(NotificationChannelDefinition.Importance.HIGH)
                        .withSound(TEST_SOUND)
                        .build())
                .addChannel(NotificationChannelDefinition.builder("quiet")
                        .withName("Quiet")
                        .withImportance(NotificationChannelDefinition.Importance.LOW)
                        .withSound(NotificationChannelDefinition.SOUND_NONE)
                        .withVibration(false)
                        .withShowBadge(false)
                        .build())
                .build());

        createCore();

        assertEquals(new HashSet<>(Arrays.asList("alerts", "quiet")), getChannelIds());

        NotificationChannel alerts = notificationManager.getNotificationChannel("alerts");
        assertEquals(NotificationManager.IMPORTANCE_HIGH, alerts.getImportance());
        assertNotNull(alerts.getSound());
        assertTrue(alerts.getSound().toString().endsWith("/raw/" + TEST_SOUND));
        assertTrue(alerts.shouldVibrate());

        NotificationChannel quiet = notificationManager.getNotificationChannel("quiet");
        assertEquals(NotificationManager.IMPORTANCE_LOW, quiet.getImportance());
        assertNull(quiet.getSound());
        assertFalse(quiet.shouldVibrate());
        assertFalse(quiet.canShowBadge());
    }

    @Test
    public void addChannel_withNameResource_shouldUseResolvedString() {
        String resourceName = context.getResources().getResourceEntryName(R.string.mm_button_accept);
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder()
                .withoutDefaultChannels()
                .addChannel(NotificationChannelDefinition.builder("localized")
                        .withName("Not used")
                        .withNameResource(resourceName)
                        .build())
                .build());

        createCore();

        assertEquals(context.getString(R.string.mm_button_accept), notificationManager.getNotificationChannel("localized").getName().toString());
    }

    @Test
    public void addChannel_withMissingNameResource_shouldUseName() {
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder()
                .withoutDefaultChannels()
                .addChannel(NotificationChannelDefinition.builder("named")
                        .withName("Plain name")
                        .withNameResource("mm_not_existing_resource")
                        .build())
                .build());

        createCore();

        assertEquals("Plain name", notificationManager.getNotificationChannel("named").getName().toString());
    }

    @Test
    public void existingChannel_shouldUpdateOnlyNameAndDescription() {
        NotificationChannel existing = new NotificationChannel("existing", "Old name", NotificationManager.IMPORTANCE_HIGH);
        existing.setSound(null, null);
        existing.enableVibration(false);
        notificationManager.createNotificationChannel(existing);
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder()
                .withSingleChannel(NotificationChannelDefinition.builder("existing")
                        .withName("New name")
                        .withDescription("New description")
                        .withImportance(NotificationChannelDefinition.Importance.LOW)
                        .withSound(NotificationChannelDefinition.SOUND_DEFAULT)
                        .withVibration(true)
                        .build())
                .build());

        createCore();

        NotificationChannel updated = notificationManager.getNotificationChannel("existing");
        assertEquals("New name", updated.getName().toString());
        assertEquals("New description", updated.getDescription());
        assertEquals(NotificationManager.IMPORTANCE_HIGH, updated.getImportance());
        assertNull(updated.getSound());
        assertFalse(updated.shouldVibrate());
    }

    @Test
    public void existingChannel_withoutNameInDefinition_shouldKeepItsName() {
        notificationManager.createNotificationChannel(new NotificationChannel("app_owned", "Allgemein", NotificationManager.IMPORTANCE_DEFAULT));
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder()
                .withoutDefaultChannels()
                .addChannel(NotificationChannelDefinition.builder("app_owned").build())
                .build());

        createCore();

        assertEquals("Allgemein", notificationManager.getNotificationChannel("app_owned").getName().toString());
    }

    @Test
    public void reinitWithDifferentConfig_shouldApplyNewConfig() {
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder().withSingleChannel("first", "First").build());
        createCore();

        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder().withSingleChannel("second", "Second").build());
        createCore();

        // channels are never deleted automatically
        assertEquals(new HashSet<>(Arrays.asList("first", "second")), getChannelIds());
    }

    @Test
    public void removedConfig_shouldRestoreSdkDefaultChannels() {
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder().withSingleChannel("single", "Single").build());
        NotificationChannelManager.saveConfig(context, null);

        createCore();

        assertNull(NotificationChannelManager.loadConfig(context));
        assertEquals(new HashSet<>(NotificationChannelRouter.getSdkDefaultChannelIds(null)), getChannelIds());
    }

    @Test
    public void invalidPersistedConfig_shouldCreateSdkDefaultChannels() {
        PreferenceHelper.saveString(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_CONFIG, "not a json");

        createCore();

        assertEquals(new HashSet<>(NotificationChannelRouter.getSdkDefaultChannelIds(null)), getChannelIds());
    }

    @Test
    public void deleteSdkDefaultChannels_shouldDeleteOnlySdkChannels_whenDefaultsDisabled() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.HEADSUP_NOTIFICATIONS_ENABLED, true);
        setCustomNotificationChannel();
        createCore();
        notificationManager.createNotificationChannel(new NotificationChannel("app_channel", "App", NotificationManager.IMPORTANCE_DEFAULT));
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder().withSingleChannel("single", "Single").build());
        createCore();

        assertTrue(mobileMessagingCore.deleteSdkDefaultNotificationChannels());

        assertEquals(new HashSet<>(Arrays.asList("single", "app_channel")), getChannelIds());
    }

    @Test
    public void deleteSdkDefaultChannels_shouldReturnFalse_whenNothingToDelete() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.HEADSUP_NOTIFICATIONS_ENABLED, true);
        createCore();
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder().withSingleChannel("single", "Single").build());
        createCore();

        assertTrue(mobileMessagingCore.deleteSdkDefaultNotificationChannels());
        assertFalse(mobileMessagingCore.deleteSdkDefaultNotificationChannels());

        assertEquals(new HashSet<>(Arrays.asList("single")), getChannelIds());
    }

    @Test
    public void deleteSdkDefaultChannels_shouldDeleteCustomSoundChannels_afterCustomChannelIsNoLongerSet() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.HEADSUP_NOTIFICATIONS_ENABLED, true);
        setCustomNotificationChannel();
        createCore();
        assertTrue(getChannelIds().containsAll(NotificationChannelRouter.getCustomSoundChannelIds(CUSTOM_CHANNEL_ID)));

        clearCustomNotificationChannel();
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder().withoutDefaultChannels().build());
        createCore();

        assertTrue(mobileMessagingCore.deleteSdkDefaultNotificationChannels());

        assertTrue(getChannelIds().isEmpty());
    }

    @Test
    public void deleteSdkDefaultChannels_shouldDeleteCustomSoundChannels_afterCleanup() {
        setCustomNotificationChannel();
        createCore();

        // e.g. application code change
        PreferenceHelper.remove(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_ID);
        PreferenceHelper.remove(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_AUDIO);
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder().withoutDefaultChannels().build());
        createCore();

        assertTrue(mobileMessagingCore.deleteSdkDefaultNotificationChannels());

        assertTrue(getChannelIds().isEmpty());
    }

    @Test
    public void deleteSdkDefaultChannels_shouldNotDeleteChannelReusedByConfig() {
        setCustomNotificationChannel();
        createCore();

        clearCustomNotificationChannel();
        NotificationChannelManager.saveConfig(context, NotificationChannelConfig.builder().withSingleChannel(CUSTOM_CHANNEL_ID, "Custom").build());
        createCore();

        assertTrue(mobileMessagingCore.deleteSdkDefaultNotificationChannels());

        assertEquals(new HashSet<>(Arrays.asList(CUSTOM_CHANNEL_ID)), getChannelIds());
    }

    @Test
    public void deleteSdkDefaultChannels_shouldNotDelete_whenDefaultsInUse() {
        createCore();

        assertFalse(mobileMessagingCore.deleteSdkDefaultNotificationChannels());

        assertEquals(new HashSet<>(NotificationChannelRouter.getSdkDefaultChannelIds(null)), getChannelIds());
    }

    private void createCore() {
        mobileMessagingCore = MobileMessagingTestable.create(context, broadcaster, mobileApiResourceProvider, firebaseAppProvider);
    }

    private void setCustomNotificationChannel() {
        PreferenceHelper.saveString(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_ID, CUSTOM_CHANNEL_ID);
        PreferenceHelper.saveString(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_NAME, "Custom");
        PreferenceHelper.saveString(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_AUDIO, TEST_SOUND);
    }

    // same as MobileMessagingCore.setCustomNotificationChannel(context, null, null, null) called by MobileMessaging.Builder.build()
    private void clearCustomNotificationChannel() {
        NotificationChannelManager.rememberCustomSoundChannelId(context, PreferenceHelper.findString(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_ID));
        PreferenceHelper.remove(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_ID);
        PreferenceHelper.remove(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_NAME);
        PreferenceHelper.remove(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_AUDIO);
    }

    private Set<String> getChannelIds() {
        Set<String> ids = new HashSet<>();
        for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
            ids.add(channel.getId());
        }
        return ids;
    }

    private void deleteAllChannels() {
        for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
            notificationManager.deleteNotificationChannel(channel.getId());
        }
    }
}
