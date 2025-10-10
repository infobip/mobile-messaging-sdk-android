package org.infobip.mobile.messaging.notification;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;

import org.infobip.mobile.messaging.ConfigurationException;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.NotificationTapReceiverActivity;
import org.infobip.mobile.messaging.OpenLivechatAction;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.app.ContentIntentWrapper;
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.DomainHelper;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BitmapCompat;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_MESSAGE;

/**
 * @author sslavin
 * @since 15/09/16.
 */
public class BaseNotificationHandler {

    private static final int DEFAULT_NOTIFICATION_ID = 0;

    private final Context context;

    private ContentIntentWrapper contentIntentWrapper;

    private DomainHelper domainHelper;

    public BaseNotificationHandler(Context context) {
        this.context = context;
    }

    public void cancelAllNotifications() {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            MobileMessagingLogger.w("Unable to get notification manager and cancel notifications");
            return;
        }

        notificationManager.cancelAll();
    }

    /**
     * Displays native android notification with builder settings for the provided notificationId.
     *
     * @param builder        Android NotificationCompat.Builder with settings for notification building.
     * @param message        Message object used for notification building.
     * @param notificationId Id of notification to be displayed
     * @see #createNotificationCompatBuilder(Message)
     * @see #getNotificationId(Message)
     */
    public boolean displayNotification(NotificationCompat.Builder builder, Message message, int notificationId) {
        if (builder == null) return false;

        //issue: http://stackoverflow.com/questions/13602190/java-lang-securityexception-requires-vibrate-permission-on-jelly-bean-4-2
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) {
                MobileMessagingLogger.w("Unable to get notification manager and display notification");
                return false;
            }
            Notification notification = builder.build();
            MobileMessagingLogger.v("NOTIFY FOR MESSAGE", message);
            notificationManager.notify(notificationId, notification);
            return true;

        } catch (SecurityException se) {
            ConfigurationException exception = new ConfigurationException(ConfigurationException.Reason.MISSING_REQUIRED_PERMISSION, Manifest.permission.VIBRATE);
            MobileMessagingLogger.e("Unable to vibrate: " + exception.getMessage(), se);
            return false;
        }
    }

    /**
     * Gets notification builder for Message.
     *
     * @param message message to display notification for.
     * @return builder
     */
    public NotificationCompat.Builder createNotificationCompatBuilder(Message message) {
        NotificationSettings notificationSettings = notificationSettings(message);
        if (notificationSettings == null) return null;

        String title = StringUtils.isNotBlank(message.getTitle()) ? message.getTitle() : notificationSettings.getDefaultTitle();
        String body = message.getBody();
        if (message.isChatMessage()) {
            String chatDefaultTitle = notificationSettings.getChatDefaultTitle();
            String chatDefaultBody = notificationSettings.getChatDefaultBody();
            if (StringUtils.isNotBlank(chatDefaultTitle)) {
                title = chatDefaultTitle;
            }
            if (StringUtils.isNotBlank(chatDefaultBody)) {
                body = chatDefaultBody;
            }
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, getChannelIdForNotification(notificationSettings, message))
                .setContentTitle(title)
                .setContentText(body)
                .setColor(notificationSettings.getColor())
                .setAutoCancel(notificationSettings.isNotificationAutoCancel())
                .setContentIntent(createTapPendingIntent(notificationSettings, message))
                .setWhen(message.getReceivedTimestamp());

        setNotificationStyle(notificationBuilder, message, title, body);
        setNotificationSoundAndVibrate(notificationBuilder, message);
        setNotificationIcon(notificationBuilder, message);
        setNotificationPriority(notificationBuilder, notificationSettings, message);

        return notificationBuilder;
    }

    private void setNotificationStyle(NotificationCompat.Builder notificationBuilder, Message message, String title, String body) {
        String contentUrl = message.getContentUrl();
        Bitmap notificationPicture = fetchNotificationPicture(contentUrl);

        if (notificationPicture == null) {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle(title));
            return;
        }


        notificationBuilder
                .setLargeIcon(notificationPicture)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(notificationPicture)
                        .bigLargeIcon(null)
                        .setBigContentTitle(title)
                        .setSummaryText(body));
    }

    @Nullable
    private static Bitmap downloadBitmap(@NonNull String contentUrl) {
        try {
            URL url = new URL(contentUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            return validateBitmap(bitmap);

        } catch (Exception e) {
            MobileMessagingLogger.e("Could not fetch image", e);
            return null;
        }
    }

    private static @Nullable
    Bitmap validateBitmap(@Nullable Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        if (bitmap.getWidth() == 0
                || bitmap.getHeight() == 0
                || isBitmapEmpty(bitmap)) {

            MobileMessagingLogger.w("Got empty or malformed Bitmap, ignoring it");
            return null;
        }

        return bitmap;
    }

    /**
     * Same code as in {@link BitmapCompat#getAllocationByteCount(Bitmap)} to avoid compat dependencies
     */
    private static boolean isBitmapEmpty(@NonNull Bitmap bitmap) {
        return bitmap.getAllocationByteCount() == 0;
    }

    @Nullable
    @VisibleForTesting
    Bitmap fetchNotificationPicture(String contentUrl) {
        if (contentUrl == null) return null;

        int maxRetries = PreferenceHelper.findInt(context, MobileMessagingProperty.DEFAULT_MAX_RETRY_COUNT);
        for (int i = 0; i < maxRetries; i++) {
            Bitmap bitmap = downloadBitmap(contentUrl);
            if (bitmap != null) {
                return bitmap;
            }
        }

        return null;
    }

    @SuppressWarnings("WrongConstant")
    @NonNull
    private PendingIntent createTapPendingIntent(@NonNull NotificationSettings notificationSettings, Message message) {

        Intent callbackIntent;
        if (StringUtils.isNotBlank(message.getWebViewUrl())) {
            callbackIntent = handleUrl(message.getWebViewUrl(), url -> activityStarterWrapper(context).createWebViewContentIntent(url), message, notificationSettings);
        } else if (StringUtils.isNotBlank(message.getBrowserUrl())) {
            callbackIntent = handleUrl(message.getBrowserUrl(), url -> activityStarterWrapper(context).createBrowserIntent(url), message, notificationSettings);
        } else if (isInAppChatMessage(message)) {
            callbackIntent = null; // InAppChat module handles the chat notification tap
        } else {
            callbackIntent = activityStarterWrapper(context).createContentIntent(message, notificationSettings);
        }

        ArrayList<Intent> intentsList = new ArrayList<>();
        if (callbackIntent != null) intentsList.add(callbackIntent);

        Intent invisibleActivity = new Intent(context, NotificationTapReceiverActivity.class);
        invisibleActivity.setAction(message.getMessageId());
        invisibleActivity.putExtra(EXTRA_MESSAGE, MessageBundleMapper.messageToBundle(message));
        intentsList.add(invisibleActivity);

        Intent[] intents = new Intent[intentsList.size()];

        int pendingIntentFlags = notificationSettings.getPendingIntentFlags();

        //if PendingIntent.FLAG_IMMUTABLE wasn't set, setting it because for Android 12 it's mandatory
        pendingIntentFlags = setPendingIntentMutabilityFlagIfNeeded(pendingIntentFlags);

        return PendingIntent.getActivities(
                context,
                0,
                intentsList.toArray(intents),
                pendingIntentFlags
        );

    }

    private Intent handleUrl(String url, Function<Message, Intent> intentCreator, Message message, NotificationSettings notificationSettings) {
        if (domainHelper(context).isTrustedDomain(url)) {
            return intentCreator.apply(message);
        } else {
            MobileMessagingLogger.w("URL domain is not trusted and will not be opened: " + url);
            return activityStarterWrapper(context).createContentIntent(message, notificationSettings);
        }
    }

    private boolean isInAppChatMessage(Message message) {
        return MobileMessagingCore.getInstance(context).findMessageHandlerModule(MobileMessagingCore.IN_APP_CHAT_MESSAGE_HANDLER_MODULE_NAME) != null
                && (OpenLivechatAction.parseFrom(message) != null || message.isChatMessage());
    }

    int setPendingIntentMutabilityFlagIfNeeded(int currentFlags) {
        int resultFlags = currentFlags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (currentFlags & PendingIntent.FLAG_IMMUTABLE) != PendingIntent.FLAG_IMMUTABLE &&
                (currentFlags & PendingIntent.FLAG_MUTABLE) != PendingIntent.FLAG_MUTABLE) {
            resultFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return resultFlags;
    }

    private NotificationSettings notificationSettings(Message message) {
        NotificationSettings notificationSettings = MobileMessagingCore.getInstance(context).getNotificationSettings();
        if (null == notificationSettings) {
            return null;
        }

        if (!notificationSettings.isDisplayNotificationEnabled() ||
                null == notificationSettings.getCallbackActivity()) {
            return null;
        }

        if (StringUtils.isBlank(message.getBody()) && StringUtils.isBlank(notificationSettings.getChatDefaultBody())) {
            return null;
        }

        if (ActivityLifecycleMonitor.isForeground() && notificationSettings.isForegroundNotificationDisabled() && !message.isChatMessage()) {
            return null;
        }

        return notificationSettings;
    }

    private void setNotificationIcon(NotificationCompat.Builder notificationBuilder, Message message) {
        NotificationSettings notificationSettings = notificationSettings(message);
        if (notificationSettings == null) return;

        int icon;
        if (StringUtils.isNotBlank(message.getIcon())) {
            icon = ResourceLoader.loadResourceByName(context, "drawable", message.getIcon());
        } else {
            icon = notificationSettings.getDefaultIcon();
        }
        notificationBuilder.setSmallIcon(icon);
    }

    private void setNotificationSoundAndVibrate(NotificationCompat.Builder notificationBuilder, Message message) {
        int notificationDefaults = Notification.DEFAULT_ALL;
        if (!message.isVibrate()) {
            notificationDefaults &= ~Notification.DEFAULT_VIBRATE;
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_DENIED) {
            notificationDefaults &= ~Notification.DEFAULT_VIBRATE;
            MobileMessagingLogger.e("Unable to vibrate", new ConfigurationException(ConfigurationException.Reason.MISSING_REQUIRED_PERMISSION, Manifest.permission.VIBRATE));
        }
        if (!message.isDefaultSound()) {
            notificationDefaults &= ~Notification.DEFAULT_SOUND;
        }
        notificationBuilder.setDefaults(notificationDefaults);
        if (message.getSound() == null && !message.isVibrate()) {
            notificationBuilder.setSilent(true);
            return;
        }
        String sound = message.getSound();
        if (message.isDefaultSound() || StringUtils.isBlank(sound)) {
            return;
        }

        Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/raw/" + sound);
        if (soundUri == null) {
            MobileMessagingLogger.w("Cannot create uri for sound: " + sound + " messageId: " + message.getMessageId());
            return;
        }
        MobileMessagingLogger.w("Trying to play custom sound: " + sound + " messageId: " + message.getMessageId());
        notificationBuilder.setSound(soundUri);
    }

    /**
     * This method will set priority to HIGH if heads up notification is required.
     * <br>This setting is necessary for Android 5.0 - 7.1 versions
     *
     * @param notificationBuilder  - notification builder
     * @param notificationSettings - notification settings to use when choosing priority
     */
    private void setNotificationPriority(NotificationCompat.Builder notificationBuilder, @NonNull NotificationSettings notificationSettings, Message message) {
        if (shouldDisplayHeadsUpNotification(notificationSettings, message)) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }
    }

    /**
     * This method will return channel with HIGH importance if heads up notification is required.
     * <br>This is necessary for Android 8.0 and above.
     *
     * @param notificationSettings - notification settings to use when choosing the channel
     */
    @NonNull
    private String getChannelIdForNotification(@NonNull NotificationSettings notificationSettings, Message message) {
        boolean hasCustomSound = message.getSound() != null && !message.getSound().equals("default");
        boolean shouldDisplayHeadsUp = shouldDisplayHeadsUpNotification(notificationSettings, message);
        boolean isVibrate = message.isVibrate();
        boolean soundEnabled = message.getSound() != null;

        if (hasCustomSound) {
            String baseChannelId = PreferenceHelper.findString(context, MobileMessagingProperty.NOTIFICATION_CHANNEL_ID);
            if (baseChannelId != null) {
                if (shouldDisplayHeadsUp) {
                    return baseChannelId + (isVibrate ? "_high_priority, vibration" : "_high_priority");
                } else {
                    return baseChannelId + (isVibrate ? "_vibration" : "");
                }
            }
        }

        return MobileMessagingCore.getNotificationChannelId(soundEnabled, isVibrate, shouldDisplayHeadsUp);
    }

    /**
     * Gets notification ID for the given message
     *
     * @param message Message object used for setting notification ID
     * @return notification ID
     */
    public int getNotificationId(Message message) {
        NotificationSettings settings = notificationSettings(message);
        if (settings == null) {
            return DEFAULT_NOTIFICATION_ID;
        }

        boolean areMultipleNotificationsEnabled = settings.areMultipleNotificationsEnabled();
        return areMultipleNotificationsEnabled ? message.getMessageId().hashCode() : DEFAULT_NOTIFICATION_ID;
    }

    private boolean shouldDisplayHeadsUpNotification(NotificationSettings notificationSettings, Message message) {
        if (!notificationSettings.areHeadsUpNotificationsEnabled()) {
            return false;
        }

        // 1) always display in background
        // 2) display in foreground when configured in message
        // 3) with banner in foreground
        return ActivityLifecycleMonitor.isBackground() || message.getInAppStyle() == Message.InAppStyle.BANNER || notificationSettings.areBannerForegroundNotificationsEnabled();
    }

    private ContentIntentWrapper activityStarterWrapper(Context context) {
        if (contentIntentWrapper == null) {
            contentIntentWrapper = new ContentIntentWrapper(context);
        }
        return contentIntentWrapper;
    }

    private DomainHelper domainHelper(Context context) {
        if (domainHelper == null) {
            domainHelper = new DomainHelper(context);
        }
        return domainHelper;
    }
}
