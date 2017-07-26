package org.infobip.mobile.messaging.notification;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.NotificationSettings;

import static org.infobip.mobile.messaging.notification.InteractiveCategory.MM_INTERACTIVE_ID_PREFIX;

/**
 * Notification action class
 */
public class NotificationAction {
    private String id;
    private String title;
    private int icon;
    private boolean bringsAppToForeground;

    private NotificationAction(String id, String title, int icon, boolean bringsAppToForeground) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.bringsAppToForeground = bringsAppToForeground;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getIcon() {
        return icon;
    }

    public boolean bringsAppToForeground() {
        return bringsAppToForeground;
    }

    public static final class Builder {
        private String id;
        private String title;
        private int icon;
        private boolean bringsAppToForeground;

        /**
         * Id of an action.
         *
         * @param id Distinguishes actions used in categories. "mm_" prefix is reserved for Mobile Messaging IDs and cannot be used.
         */
        public Builder withId(@NonNull String id) {
            validateWithParam(id);
            if (id.startsWith(MM_INTERACTIVE_ID_PREFIX)) {
                throw new IllegalArgumentException(String.format("'%s' prefix is reserved for Mobile Messaging library", MM_INTERACTIVE_ID_PREFIX));
            }

            this.id = id;
            return this;
        }

        /**
         * Title of the action button.
         *
         * @param title Text displayed on notification action button.
         */
        public Builder withTitle(@NonNull String title) {
            validateWithParam(title);
            this.title = title;
            return this;
        }

        /**
         * Starting in Android N, actions are shown without icons in order to accommodate more text. An icon should still be provided
         * because devices with earlier versions of the OS continue to rely on it, as will Android Wear and Android Auto devices.
         *
         * @param icon Icon displayed on action button
         */
        public Builder withIcon(int icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Brings app to foreground on action click. {@link InteractiveCategory} and action ID will be forwarded in intent to
         * activity that's set up with {@link NotificationSettings.Builder#withCallbackActivity(Class)}
         *
         * @param bringAppToForeground Sets app to foreground with 'true' value
         */
        public Builder withBringingAppToForeground(boolean bringAppToForeground) {
            this.bringsAppToForeground = bringAppToForeground;
            return this;
        }

        public NotificationAction build() {
            return new NotificationAction(id, title, icon, bringsAppToForeground);
        }

        private void validateWithParam(Object o) {
            if (null != o) {
                return;
            }
            throw new IllegalArgumentException("Can't use 'with' method with null argument!");
        }
    }
}
