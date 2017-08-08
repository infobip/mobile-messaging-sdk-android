package org.infobip.mobile.messaging.interactive;

import android.content.Context;

/**
 * @author tjuric
 * @since 04/08/17.
 */
public abstract class MobileInteractive {

    /**
     * Gets an instance of MobileInteractive after it is initialized.
     * </p>
     * If the app was killed and there is no instance available, it will return a temporary instance based on current context.
     *
     * @param context android context object.
     * @return instance of MobileGeo.
     */
    public synchronized static MobileInteractive getInstance(Context context) {
        return MobileInteractiveImpl.getInstance(context);
    }

    /**
     * It will configure interactive notification categories along with their actions. Maximum of three (3) actions are shown in the
     * default notification layout. Actions are displayed in the order they've been set.
     * <p>
     * Handle action click event by registering broadcast for {@link InteractiveEvent#NOTIFICATION_ACTION_TAPPED} event or set a callback
     * activity that will be triggered for actions that have {@link NotificationAction.Builder#withBringingAppToForeground(boolean)}
     * configured.
     * <pre>
     *  NotificationAction action1 = new NotificationAction.Builder()
     *      .withId("decline")
     *      .withTitleResourceId(R.string.decline)
     *      .withIcon(R.drawable.decline)
     *      .build();
     *
     * NotificationAction action2 = new NotificationAction.Builder()
     *      .withId("accept")
     *      .withTitleResourceId(R.string.accept)
     *      .withIcon(R.drawable.accept)
     *      .withBringingAppToForeground(true)
     *      .build();
     *
     * NotificationCategory notificationCategory = new NotificationCategory("category_confirm", action1, action2);
     * new MobileInteractive().setNotificationCategories(notificationCategory);
     * </pre>
     * <p/>
     */
    public abstract void setNotificationCategories(NotificationCategory... notificationCategories);
}
