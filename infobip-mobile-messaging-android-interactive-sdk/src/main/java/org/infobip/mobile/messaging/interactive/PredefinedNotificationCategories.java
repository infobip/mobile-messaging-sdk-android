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
        final NotificationAction mmDecline = new NotificationAction.Builder(true)
                .withId(PredefinedActionIds.mm_decline.name())
                .withIcon(R.drawable.mm_ic_button_decline)
                .withTitleResourceId(R.string.mm_button_decline)
                .build();

        final NotificationAction mmAccept = new NotificationAction.Builder(true)
                .withId(PredefinedActionIds.mm_accept.name())
                .withIcon(R.drawable.mm_ic_button_accept)
                .withTitleResourceId(R.string.mm_button_accept)
                .withBringingAppToForeground(true)
                .build();

        return new NotificationCategory(true, PredefinedCategoryIds.mm_accept_decline.name(), mmDecline, mmAccept);
    }

    private enum PredefinedActionIds {
        mm_accept, mm_decline
    }

    private enum PredefinedCategoryIds {
        mm_accept_decline
    }
}
