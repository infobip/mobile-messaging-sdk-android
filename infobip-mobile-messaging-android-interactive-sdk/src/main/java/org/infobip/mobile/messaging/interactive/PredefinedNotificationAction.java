package org.infobip.mobile.messaging.interactive;


public class PredefinedNotificationAction {

    public static NotificationAction accept() {
        return new NotificationAction.Builder(true)
                .withId(PredefinedActionIds.mm_accept.name())
                .withIcon(R.drawable.mm_ic_button_accept)
                .withTitleResourceId(R.string.mm_button_accept)
                .withBringingAppToForeground(true)
                .withMoMessage()
                .build();
    }

    public static NotificationAction decline() {
        return new NotificationAction.Builder(true)
                .withId(PredefinedActionIds.mm_decline.name())
                .withIcon(R.drawable.mm_ic_button_decline)
                .withTitleResourceId(R.string.mm_button_decline)
                .withMoMessage()
                .build();
    }

    public static NotificationAction open() {
        return new NotificationAction.Builder(true)
                .withId(PredefinedActionIds.mm_open.name())
                .withTitleResourceId(R.string.mm_button_open)
                .withMoMessage()
                .build();
    }

    public static NotificationAction cancel() {
        return new NotificationAction.Builder(true)
                .withId(PredefinedActionIds.mm_cancel.name())
                .withTitleResourceId(R.string.mm_button_cancel)
                .withMoMessage()
                .build();
    }

    public static NotificationAction[] defaultInAppActions() {
        return new NotificationAction[]{
                PredefinedNotificationAction.cancel(),
                PredefinedNotificationAction.open()
        };
    }

    private enum PredefinedActionIds {
        mm_accept, mm_decline, mm_open, mm_cancel
    }
}
