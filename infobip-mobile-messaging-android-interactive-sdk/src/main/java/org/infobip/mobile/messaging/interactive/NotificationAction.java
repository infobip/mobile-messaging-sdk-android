package org.infobip.mobile.messaging.interactive;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationActionBundleMapper;

/**
 * Notification action class
 */
public class NotificationAction {
    private String id;
    private int titleResourceId;
    private int icon;
    private boolean bringsAppToForeground;
    private boolean sendsMoMessage;

    private NotificationAction(String id, int titleResourceId, int icon, boolean bringsAppToForeground, boolean sendsMoMessage) {
        this.id = id;
        this.titleResourceId = titleResourceId;
        this.icon = icon;
        this.bringsAppToForeground = bringsAppToForeground;
        this.sendsMoMessage = sendsMoMessage;
    }

    public static NotificationAction createFrom(Bundle bundle) {
        return NotificationActionBundleMapper.notificationActionFromBundle(bundle);
    }

    public String getId() {
        return id;
    }

    public int getTitleResourceId() {
        return titleResourceId;
    }

    public int getIcon() {
        return icon;
    }

    public boolean bringsAppToForeground() {
        return bringsAppToForeground;
    }

    public boolean sendsMoMessage() {
        return sendsMoMessage;
    }

    public static final class Builder {
        private boolean predefined;
        private String id;
        private int titleResourceId;
        private int icon;
        private boolean bringsAppToForeground;
        private boolean sendsMoMessage;

        public Builder() {
            this.predefined = false;
        }

        protected Builder(boolean predefined) {
            this.predefined = predefined;
        }

        /**
         * Id of an action.
         *
         * @param id Distinguishes actions used in categories. "mm_" prefix is reserved for Mobile Messaging IDs and cannot be used.
         */
        public Builder withId(@NonNull String id) {
            validateWithParam(id);
            if (!predefined && id.startsWith(NotificationCategory.MM_INTERACTIVE_ID_PREFIX)) {
                throw new IllegalArgumentException(String.format("'%s' prefix is reserved for Mobile Messaging library", NotificationCategory.MM_INTERACTIVE_ID_PREFIX));
            }

            this.id = id;
            return this;
        }

        /**
         * Title of the action button.
         *
         * @param titleResourceId Resource ID of the text displayed on notification action button. Example: R.string.yes
         */
        public Builder withTitleResourceId(int titleResourceId) {
            this.titleResourceId = titleResourceId;
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
         * Brings app to foreground on action click. {@link NotificationCategory} and action ID will be forwarded in intent to
         * activity that's set up with {@link NotificationSettings.Builder#withCallbackActivity(Class)}
         *
         * @param bringAppToForeground Sets app to foreground with 'true' value
         */
        public Builder withBringingAppToForeground(boolean bringAppToForeground) {
            this.bringsAppToForeground = bringAppToForeground;
            return this;
        }

        /**
         * Sends a speсific mobile originated message to the server when this notification action is triggered.
         */
        public Builder withMoMessage() {
            this.sendsMoMessage = true;
            return this;
        }

        public NotificationAction build() {
            return new NotificationAction(id, titleResourceId, icon, bringsAppToForeground, sendsMoMessage);
        }

        private void validateWithParam(Object o) {
            if (null != o) {
                return;
            }
            throw new IllegalArgumentException("Can't use 'with' method with null argument!");
        }
    }
}
