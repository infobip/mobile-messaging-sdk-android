package org.infobip.mobile.messaging.interactive;


import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

public class PredefinedNotificationCategories {

    public static Set<NotificationCategory> load() {
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

    private enum PredefinedCategoryIds {
        mm_accept_decline
    }
}
