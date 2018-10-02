package org.infobip.mobile.messaging.interactive.predefined;


import org.infobip.mobile.messaging.resources.R;
import org.infobip.mobile.messaging.interactive.NotificationAction;

public class PredefinedNotificationAction {

    protected static NotificationAction accept() {
        return new NotificationAction.Builder(true)
                .withId(PredefinedActionIds.mm_accept.name())
                .withIcon(R.drawable.mm_ic_button_accept)
                .withTitleResourceId(R.string.mm_button_accept)
                .withBringingAppToForeground(true)
                .withMoMessage()
                .build();
    }

    protected static NotificationAction decline() {
        return new NotificationAction.Builder(true)
                .withId(PredefinedActionIds.mm_decline.name())
                .withIcon(R.drawable.mm_ic_button_decline)
                .withTitleResourceId(R.string.mm_button_decline)
                .withMoMessage()
                .build();
    }

    protected static NotificationAction open() {
        return new NotificationAction.Builder(true)
                .withId(PredefinedActionIds.mm_open.name())
                .withTitleResourceId(R.string.mm_button_open)
                .withMoMessage()
                .build();
    }

    protected static NotificationAction cancel() {
        return new NotificationAction.Builder(true)
                .withId(PredefinedActionIds.mm_cancel.name())
                .withTitleResourceId(R.string.mm_button_cancel)
                .withMoMessage()
                .build();
    }

    protected static NotificationAction cancelWithAndroidResource() {
        return new NotificationAction.Builder(true)
                .withId(PredefinedActionIds.mm_cancel.name())
                .withTitleResourceId(android.R.string.cancel)
                .withMoMessage()
                .build();
    }

    protected static NotificationAction[] defaultInAppActions() {
        return new NotificationAction[]{
                PredefinedNotificationAction.cancel(),
                PredefinedNotificationAction.open()
        };
    }

    protected static NotificationAction[] defaultInAppActionsWhenNoResources() {
        return new NotificationAction[]{
                PredefinedNotificationAction.cancelWithAndroidResource()
        };
    }

    protected enum PredefinedActionIds {
        mm_accept, mm_decline, mm_open, mm_cancel
    }
}
