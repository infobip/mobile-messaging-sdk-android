/*
 * NotificationActionBundleMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.dal.bundle;


import android.os.Bundle;

import org.infobip.mobile.messaging.dal.bundle.BundleMapper;
import org.infobip.mobile.messaging.interactive.NotificationAction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class NotificationActionBundleMapper extends BundleMapper {

    private static final String BUNDLED_ACTION_TAG = NotificationActionBundleMapper.class.getName() + ".action";

    /**
     * De-serializes notification action object from bundle
     *
     * @param bundle where to load data from
     * @return new notification action object
     */
    public static
    @Nullable
    NotificationAction notificationActionFromBundle(@NonNull Bundle bundle) {
        return objectFromBundle(bundle, BUNDLED_ACTION_TAG, NotificationAction.class);
    }

    /**
     * Serializes notification action object into bundle
     *
     * @param notificationAction object to serialize
     * @return bundle with notification action
     */
    public static
    @NonNull
    Bundle notificationActionToBundle(@NonNull NotificationAction notificationAction) {
        return objectToBundle(notificationAction, BUNDLED_ACTION_TAG);
    }
}
