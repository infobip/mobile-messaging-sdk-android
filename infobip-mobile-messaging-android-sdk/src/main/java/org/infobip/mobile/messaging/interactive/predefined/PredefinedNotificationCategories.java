/*
 * PredefinedNotificationCategories.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.predefined;


import org.infobip.mobile.messaging.interactive.NotificationCategory;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;

public class PredefinedNotificationCategories {

    protected static Set<NotificationCategory> load() {
        final NotificationCategory mmAcceptDecline = mmAcceptDecline();

        final Set<NotificationCategory> predefinedCategories = new HashSet<>();
        predefinedCategories.add(mmAcceptDecline);

        return predefinedCategories;
    }

    @NonNull
    private static NotificationCategory mmAcceptDecline() {
        return new NotificationCategory(
                true,
                PredefinedCategoryIds.mm_accept_decline.name(),
                PredefinedNotificationAction.decline(),
                PredefinedNotificationAction.accept());
    }

    protected enum PredefinedCategoryIds {
        mm_accept_decline
    }
}
