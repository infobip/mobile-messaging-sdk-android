package org.infobip.mobile.messaging.notification;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author sslavin
 * @since 15/09/16.
 */
public class NotificationHandler {

    /**
     * Create and show a simple notification for the corresponding message.
     *
     * @param message message to display notification for.
     */
    public static void displayNotification(Context context, Message message, int notificationId) {
        NotificationCompat.Builder builder = notificationCompatBuilder(context, message);
        if (builder == null) return;

        //issue: http://stackoverflow.com/questions/13602190/java-lang-securityexception-requires-vibrate-permission-on-jelly-bean-4-2
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = builder.build();
            notificationManager.notify(notificationId, notification);
        } catch (SecurityException se) {
            se.printStackTrace();
            Log.e(MobileMessaging.TAG, "Unable to vibrate. Please, add the following permission to the AndroidManifest.xml: " + Manifest.permission.VIBRATE);
        }
    }

    private static NotificationCompat.Builder notificationCompatBuilder(Context context, Message message) {
        NotificationSettings notificationSettings = notificationSettings(context, message);
        if (notificationSettings == null) return null;

        Intent intent = new Intent(context, notificationSettings.getCallbackActivity());
        intent.putExtra(MobileMessagingProperty.EXTRA_MESSAGE.getKey(), message.getBundle());
        intent.addFlags(notificationSettings.getIntentFlags());
        @SuppressWarnings("ResourceType") PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, notificationSettings.getPendingIntentFlags());

        String title = StringUtils.isNotBlank(message.getTitle()) ? message.getTitle() : notificationSettings.getDefaultTitle();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message.getBody())
                .setAutoCancel(notificationSettings.isNotificationAutoCancel())
                .setContentIntent(pendingIntent)
                .setWhen(message.getReceivedTimestamp());

        setNotificationSoundAndVibrate(context, notificationBuilder, message);
        setNotificationIcon(context, notificationBuilder, message);

        return notificationBuilder;
    }

    private static NotificationSettings notificationSettings(Context context, Message message) {
        NotificationSettings notificationSettings = MobileMessagingCore.getInstance(context).getNotificationSettings();
        if (null == notificationSettings) {
            return null;
        }

        if (!notificationSettings.isDisplayNotificationEnabled() ||
                null == notificationSettings.getCallbackActivity()) {
            return null;
        }

        if (StringUtils.isBlank(message.getBody())) {
            return null;
        }

        if (ActivityLifecycleMonitor.isForeground() && notificationSettings.isForegroundNotificationDisabled()) {
            return null;
        }

        return notificationSettings;
    }

    private static void setNotificationIcon(Context context, NotificationCompat.Builder notificationBuilder, Message message) {
        NotificationSettings notificationSettings = notificationSettings(context, message);
        if (notificationSettings == null) return;

        int icon;
        if (StringUtils.isNotBlank(message.getIcon())) {
            icon = ResourceLoader.loadResourceByName(context, "drawable", message.getIcon());
        } else {
            icon = notificationSettings.getDefaultIcon();
        }
        notificationBuilder.setSmallIcon(icon);
    }

    private static void setNotificationSoundAndVibrate(Context context, NotificationCompat.Builder notificationBuilder, Message message) {
        int notificationDefaults = Notification.DEFAULT_ALL;
        if (!message.isVibrate()) {
            notificationDefaults &= ~Notification.DEFAULT_VIBRATE;
        } else if (message.isVibrate() && ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_DENIED) {
            notificationDefaults &= ~Notification.DEFAULT_VIBRATE;
            Log.e(MobileMessaging.TAG, "Unable to vibrate. Please, add the following permission to the AndroidManifest.xml: " + Manifest.permission.VIBRATE);
        }
        if (!message.isDefaultSound()) {
            notificationDefaults &= ~Notification.DEFAULT_SOUND;
        }
        notificationBuilder.setDefaults(notificationDefaults);

        String sound = message.getSound();
        if (message.isDefaultSound() || StringUtils.isBlank(sound)) {
            return;
        }

        Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/raw/" + sound);
        if (soundUri == null) {
            Log.e(MobileMessaging.TAG, "Cannot create uri for sound:" + sound + " messageId:" + message.getMessageId());
            return;
        }

        notificationBuilder.setSound(soundUri);
    }
}
