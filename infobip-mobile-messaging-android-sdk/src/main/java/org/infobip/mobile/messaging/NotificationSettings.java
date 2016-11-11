package org.infobip.mobile.messaging;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * The notifications configuration class. It configures the how notification will look like if the library is displaying it.
 *
 * @author mstipanov
 * @see MobileMessaging
 * @see MobileMessaging.Builder
 * @see NotificationSettings
 * @see Builder
 * @see Builder#withDefaultTitle(String)
 * @see Builder#withCallbackActivity(Class)
 * @see Builder#withDefaultIcon(int)
 * @see Builder#withIntentFlags(int)
 * @see Builder#withPendingIntentFlags(int)
 * @see Builder#withNotificationAutoCancel()
 * @see Builder#withoutNotificationAutoCancel()
 * @since 07.04.2016.
 */
public class NotificationSettings {

    private final Context context;

    NotificationSettings(Context context) {
        this.context = context;
    }

    public Class<?> getCallbackActivity() {
        return PreferenceHelper.findClass(context, MobileMessagingProperty.CALLBACK_ACTIVITY);
    }

    private void setCallbackActivity(Class<?> callbackActivity) {
        if (null == callbackActivity) {
            throw new IllegalArgumentException("callbackActivity is mandatory! You should use the activity that will display received messages.");
        }
        PreferenceHelper.saveClass(context, MobileMessagingProperty.CALLBACK_ACTIVITY, callbackActivity);
    }

    public boolean isDisplayNotificationEnabled() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED);
    }

    public String getDefaultTitle() {
        return PreferenceHelper.findString(context, MobileMessagingProperty.DEFAULT_TITLE);
    }

    private void setDefaultTitle(String defaultTitle) {
        PreferenceHelper.saveString(context, MobileMessagingProperty.DEFAULT_TITLE, defaultTitle);
    }

    public int getDefaultIcon() {
        return PreferenceHelper.findInt(context, MobileMessagingProperty.DEFAULT_ICON);
    }

    private void setDefaultIcon(int defaultIcon) {
        //We can't use the suggested getDrawable(@DrawableRes int id, @Nullable Theme theme) method,
        //because it is introduced in API v21 and we need to support API v14
        //noinspection deprecation
        if (null == context.getResources().getDrawable(defaultIcon)) {
            throw new IllegalArgumentException("defaultIcon doesn't exist: " + defaultIcon);
        }

        PreferenceHelper.saveInt(context, MobileMessagingProperty.DEFAULT_ICON, defaultIcon);
    }

    public int getIntentFlags() {
        return PreferenceHelper.findInt(context, MobileMessagingProperty.INTENT_FLAGS);
    }

    private void setIntentFlags(int intentFlags) {
        PreferenceHelper.saveInt(context, MobileMessagingProperty.INTENT_FLAGS, intentFlags);
    }

    public int getPendingIntentFlags() {
        return PreferenceHelper.findInt(context, MobileMessagingProperty.PENDING_INTENT_FLAGS);
    }

    private void setPendingIntentFlags(int pendingIntentFlags) {
        PreferenceHelper.saveInt(context, MobileMessagingProperty.PENDING_INTENT_FLAGS, pendingIntentFlags);
    }

    public boolean isNotificationAutoCancel() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.NOTIFICATION_AUTO_CANCEL);
    }

    private void setNotificationAutoCancel(boolean notificationAutoCancel) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.NOTIFICATION_AUTO_CANCEL, notificationAutoCancel);
    }

    public boolean isForegroundNotificationEnabled() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.FOREGROUND_NOTIFICATION_ENABLED);
    }

    private void setForegroundNotificationEnabled(boolean foregroundNotificationEnabled) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.FOREGROUND_NOTIFICATION_ENABLED, foregroundNotificationEnabled);
    }

    public boolean isForegroundNotificationDisabled() {
        return !isForegroundNotificationEnabled();
    }

    /**
     * The {@link NotificationSettings} builder class.
     *
     * @author mstipanov
     * @see MobileMessaging
     * @see MobileMessaging.Builder
     * @see NotificationSettings
     * @see Builder#withDefaultTitle(String)
     * @see Builder#withCallbackActivity(Class)
     * @see Builder#withDefaultIcon(int)
     * @see Builder#withIntentFlags(int)
     * @see Builder#withPendingIntentFlags(int)
     * @see Builder#withNotificationAutoCancel()
     * @see Builder#withoutNotificationAutoCancel()
     * @since 07.04.2016.
     */
    public static final class Builder {
        private final Context context;
        private int defaultIcon = (int) MobileMessagingProperty.DEFAULT_ICON.getDefaultValue();
        private String defaultTitle = (String) MobileMessagingProperty.DEFAULT_TITLE.getDefaultValue();
        private Class<?> callbackActivity = (Class<?>) MobileMessagingProperty.CALLBACK_ACTIVITY.getDefaultValue();
        private int intentFlags = (int) MobileMessagingProperty.INTENT_FLAGS.getDefaultValue();
        private int pendingIntentFlags = (int) MobileMessagingProperty.PENDING_INTENT_FLAGS.getDefaultValue();
        private boolean notificationAutoCancel = (boolean) MobileMessagingProperty.NOTIFICATION_AUTO_CANCEL.getDefaultValue();
        private boolean foregroundNotificationEnabled = (boolean) MobileMessagingProperty.FOREGROUND_NOTIFICATION_ENABLED.getDefaultValue();

        public Builder(Context context) {
            if (null == context) {
                throw new IllegalArgumentException("context is mandatory!");
            }
            this.context = context;

            loadCallbackActivity(context);
            loadDefaultTitle(context);
            loadDefaultIcon(context);
        }

        private void loadCallbackActivity(Context context) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            String className = launchIntent.getComponent().getClassName();
            if (StringUtils.isBlank(className)) {
                return;
            }
            try {
                this.callbackActivity = Class.forName(className);
            } catch (ClassNotFoundException e) {
                //ignore
            }
        }

        private void loadDefaultTitle(Context context) {
            int resource = ResourceLoader.loadResourceByName(context, "string", "app_name");
            if (resource > 0) {
                String defaultTitle = context.getResources().getString(resource);
                if (StringUtils.isNotBlank(defaultTitle)) {
                    this.defaultTitle = defaultTitle;
                }
            }
        }

        private void loadDefaultIcon(Context context) {
            int resource = SoftwareInformation.getAppIconResourceId(context);
            if (resource > 0) {
                this.defaultIcon = resource;
            }
        }

        private void validateWithParam(Object o) {
            if (null != o) {
                return;
            }
            throw new IllegalArgumentException("Can't use 'with' method with null argument!");
        }

        /**
         * Defines component to be used as a callback activity. It is triggered when user clicks on the notification.
         * <pre>
         * {@code
         * new Builder(this)
         *                .withCallbackActivity(MyActivity.class)
         *                .build();
         * }
         * </pre>
         * <p/>
         * By default it will use the default activity for the application package.
         *
         * @param callbackActivity The component class that is to be used for the intent when notification is clicked.
         * @return {@link Builder}
         */
        public Builder withCallbackActivity(Class<?> callbackActivity) {
            validateWithParam(callbackActivity);
            this.callbackActivity = callbackActivity;
            return this;
        }

        /**
         * When you want to use some notification title instead of the app name <i>R.string.app_name</i>
         * <p/>
         * By default it will use <i>R.string.app_name</i>
         *
         * @param defaultTitle will be displayed in the notification title, if the notification doesn't override it.
         * @return {@link Builder}
         */
        public Builder withDefaultTitle(String defaultTitle) {
            validateWithParam(defaultTitle);
            this.defaultTitle = defaultTitle;
            return this;
        }

        /**
         * When you want to use some notification icon instead of the app icon <i>R.mipmap.ic_launcher</i>
         * <p/>
         * By default it will use <i>R.mipmap.ic_launcher</i>
         *
         * @param defaultIcon will be displayed in the notification area, if the notification doesn't override it.
         * @return {@link Builder}
         */
        public Builder withDefaultIcon(int defaultIcon) {
            this.defaultIcon = defaultIcon;
            return this;
        }

        /**
         * When you want to set notification intent flags. It is delegated to {@link Intent#addFlags(int)}
         * <p/>
         * By default it will use <i>Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP</i>
         *
         * @param intentFlags to be delegated to {@link Intent#addFlags(int)}.
         * @return {@link Builder}
         * @see Intent#addFlags(int)
         */
        public Builder withIntentFlags(int intentFlags) {
            this.intentFlags = intentFlags;
            return this;
        }

        /**
         * When you want to set notification pending intent flags. It is delegated to
         * {@link PendingIntent#getActivity(android.content.Context, int, android.content.Intent, int)}
         * <p/>
         * By default it will use {@link PendingIntent#FLAG_CANCEL_CURRENT}
         *
         * @param pendingIntentFlags to be delegated to {@link PendingIntent#getActivity(android.content.Context, int, android.content.Intent, int)}.
         * @return {@link Builder}
         * @see PendingIntent
         * @see PendingIntent#FLAG_CANCEL_CURRENT
         * @see PendingIntent#getActivity(android.content.Context, int, android.content.Intent, int)
         */
        public Builder withPendingIntentFlags(int pendingIntentFlags) {
            this.pendingIntentFlags = pendingIntentFlags;
            return this;
        }

        /**
         * When you want to set notification auto-cancel to <i>true</i>. It is delegated to {@link NotificationCompat.Builder#setAutoCancel(boolean)}
         * <p/>
         * By default it will be set to <i>true</i>
         *
         * @return {@link Builder}
         * @see NotificationCompat.Builder
         * @see NotificationCompat.Builder#setAutoCancel(boolean)
         */
        public Builder withNotificationAutoCancel() {
            this.notificationAutoCancel = true;
            return this;
        }

        /**
         * When you want to set notification auto-cancel to <i>false</i>. It is delegated to {@link NotificationCompat.Builder#setAutoCancel(boolean)}
         * <p/>
         * By default it will be set to <i>true</i>
         *
         * @return {@link Builder}
         * @see NotificationCompat.Builder
         * @see NotificationCompat.Builder#setAutoCancel(boolean)
         */
        public Builder withoutNotificationAutoCancel() {
            this.notificationAutoCancel = false;
            return this;
        }

        /**
         * When you want to disable notifications when your app is in foreground.
         * <p/>
         * By default foreground notifications are enabled
         *
         * @return {@link Builder}
         */
        public Builder withoutForegroundNotification() {
            this.foregroundNotificationEnabled = false;
            return this;
        }

        /**
         * Builds the <i>NotificationSettings</i> configuration.
         *
         * @return {@link NotificationSettings}
         */
        public NotificationSettings build() {
            NotificationSettings notificationSettings = new NotificationSettings(context.getApplicationContext());

            notificationSettings.setDefaultTitle(defaultTitle);
            notificationSettings.setDefaultIcon(defaultIcon);
            notificationSettings.setCallbackActivity(callbackActivity);
            notificationSettings.setIntentFlags(intentFlags);
            notificationSettings.setPendingIntentFlags(pendingIntentFlags);
            notificationSettings.setNotificationAutoCancel(notificationAutoCancel);
            notificationSettings.setForegroundNotificationEnabled(foregroundNotificationEnabled);

            return notificationSettings;
        }
    }
}
