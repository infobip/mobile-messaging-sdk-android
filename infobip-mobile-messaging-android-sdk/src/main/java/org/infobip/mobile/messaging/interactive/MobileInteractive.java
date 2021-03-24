package org.infobip.mobile.messaging.interactive;

import android.content.Context;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.Message;

import java.util.Set;

/**
 * @author tjuric
 * @since 04/08/17.
 */
public abstract class MobileInteractive {

    /**
     * Gets an instance of MobileInteractive after it is initialized.
     * <br>
     * If the app was killed and there is no instance available, it will return a temporary instance based on current context.
     *
     * @param context android context object.
     * @return instance of MobileInteractive.
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
     * <br>
     */
    public abstract void setNotificationCategories(NotificationCategory... notificationCategories);

    /**
     * Gets NotificationCategory object for provided category ID. Category ID can be obtained from Message object
     * with {@link Message#getCategory()} method. Works with both custom and predefined categories.
     *
     * @param categoryId Given category ID
     * @return NotificationCategory instance
     */
    public abstract NotificationCategory getNotificationCategory(String categoryId);

    /**
     * Gets map of notification categories. Key is category ID, value is related NotificatioCategory.
     *
     * @return map of configured categories.
     */
    public abstract Set<NotificationCategory> getNotificationCategories();

    /**
     * Triggers default SDK actions (for example, mark message as seen) performed by SDK on
     * {@link InteractiveEvent#NOTIFICATION_ACTION_TAPPED} event.
     *
     * @param action     Action to use for performing default actions. One that matches the category ID
     * @param message    Message object
     */
    public abstract void triggerSdkActionsFor(NotificationAction action, Message message);

    /**
     * Cleans up MobileInteractive installation and removes custom categories.
     */
    public abstract void cleanup();

    /**
     * Displays in-app notification for provided message, if it's not expired yet.
     * {@link Message#getInAppStyle()} should be {@link org.infobip.mobile.messaging.Message.InAppStyle#MODAL}.
     * @param message   Message object
     */
    public abstract void displayInAppDialogFor(@NonNull Message message);
}
