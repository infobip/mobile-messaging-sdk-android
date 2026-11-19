/*
 * NotificationChannelDefinition.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.notification;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Definition of a notification channel which the SDK creates on behalf of the application.
 * <p>
 * Channel is created only if it doesn't exist yet. Importance, sound and vibration of an existing channel are never changed,
 * because Android keeps them under user control. Name and description are re-applied on every SDK initialization.
 * <p>
 * Name and description can be provided as string resource names (e.g. "channel_offers"), which are resolved
 * for the current locale, or as plain text.
 */
public final class NotificationChannelDefinition {

    /**
     * Sound value for a channel using the default notification sound.
     */
    public static final String SOUND_DEFAULT = "default";
    /**
     * Sound value for a channel without sound.
     */
    public static final String SOUND_NONE = "none";

    public enum Importance {
        @SerializedName(value = "high", alternate = "HIGH")
        HIGH,
        @SerializedName(value = "default", alternate = "DEFAULT")
        DEFAULT,
        @SerializedName(value = "low", alternate = "LOW")
        LOW,
        @SerializedName(value = "min", alternate = "MIN")
        MIN
    }

    private String id;
    private String name;
    private String nameResource;
    private String description;
    private String descriptionResource;
    private Importance importance;
    private String sound;
    private Boolean vibration;
    private Boolean showBadge;

    @SuppressWarnings("unused")
    private NotificationChannelDefinition() {
        // used by JSON deserialization
    }

    private NotificationChannelDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.nameResource = builder.nameResource;
        this.description = builder.description;
        this.descriptionResource = builder.descriptionResource;
        this.importance = builder.importance;
        this.sound = builder.sound;
        this.vibration = builder.vibration;
        this.showBadge = builder.showBadge;
    }

    public static Builder builder(@NonNull String id) {
        return new Builder(id);
    }

    public String getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getNameResource() {
        return nameResource;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getDescriptionResource() {
        return descriptionResource;
    }

    /**
     * @return importance or null if not set
     */
    @Nullable
    public Importance getImportance() {
        return importance;
    }

    /**
     * @return {@link #SOUND_DEFAULT}, {@link #SOUND_NONE}, name of the sound file in res/raw (without extension) or null for default sound
     */
    @Nullable
    public String getSound() {
        return sound;
    }

    public boolean isVibrationEnabled() {
        return vibration == null || vibration;
    }

    public boolean isShowBadge() {
        return showBadge == null || showBadge;
    }

    public static final class Builder {
        private final String id;
        private String name;
        private String nameResource;
        private String description;
        private String descriptionResource;
        private Importance importance;
        private String sound;
        private Boolean vibration;
        private Boolean showBadge;

        private Builder(@NonNull String id) {
            this.id = id;
        }

        /**
         * If neither name nor name resource is set, the application name is used when the SDK creates the channel,
         * and the name of an existing channel is not changed.
         *
         * @param name channel name visible to the user in system settings
         */
        public Builder withName(@Nullable String name) {
            this.name = name;
            return this;
        }

        /**
         * @param nameResource name of the string resource with channel name (e.g. "channel_offers"), takes precedence over {@link #withName(String)}
         */
        public Builder withNameResource(@Nullable String nameResource) {
            this.nameResource = nameResource;
            return this;
        }

        /**
         * @param description channel description visible to the user in system settings
         */
        public Builder withDescription(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * @param descriptionResource name of the string resource with channel description, takes precedence over {@link #withDescription(String)}
         */
        public Builder withDescriptionResource(@Nullable String descriptionResource) {
            this.descriptionResource = descriptionResource;
            return this;
        }

        public Builder withImportance(@Nullable Importance importance) {
            this.importance = importance;
            return this;
        }

        /**
         * @param sound {@link #SOUND_DEFAULT}, {@link #SOUND_NONE} or name of the sound file in res/raw (without extension)
         */
        public Builder withSound(@Nullable String sound) {
            this.sound = sound;
            return this;
        }

        public Builder withVibration(boolean vibration) {
            this.vibration = vibration;
            return this;
        }

        public Builder withShowBadge(boolean showBadge) {
            this.showBadge = showBadge;
            return this;
        }

        public NotificationChannelDefinition build() {
            return new NotificationChannelDefinition(this);
        }
    }
}
