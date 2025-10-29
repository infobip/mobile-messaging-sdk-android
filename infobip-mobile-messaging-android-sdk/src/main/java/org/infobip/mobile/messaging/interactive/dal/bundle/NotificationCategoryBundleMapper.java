/*
 * NotificationCategoryBundleMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.dal.bundle;


import android.os.Bundle;

import org.infobip.mobile.messaging.dal.bundle.BundleMapper;
import org.infobip.mobile.messaging.interactive.NotificationCategory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class NotificationCategoryBundleMapper extends BundleMapper {

    private static final String BUNDLED_CATEGORY_TAG = NotificationCategoryBundleMapper.class.getName() + ".category";

    /**
     * De-serializes notification category object from bundle
     *
     * @param bundle where to load data from
     * @return new notification category object
     */
    public static
    @Nullable
    NotificationCategory notificationCategoryFromBundle(@NonNull Bundle bundle) {
        return objectFromBundle(bundle, BUNDLED_CATEGORY_TAG, NotificationCategory.class);
    }

    /**
     * Serializes notification category object into bundle
     *
     * @param notificationCategory object to serialize
     * @return bundle with notification category
     */
    public static
    @NonNull
    Bundle notificationCategoryToBundle(@NonNull NotificationCategory notificationCategory) {
        return objectToBundle(notificationCategory, BUNDLED_CATEGORY_TAG);
    }
}
