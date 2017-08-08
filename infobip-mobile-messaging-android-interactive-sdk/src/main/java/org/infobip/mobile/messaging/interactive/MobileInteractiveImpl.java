package org.infobip.mobile.messaging.interactive;

import android.content.Context;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author tjuric
 * @since 04/08/17.
 */
public class MobileInteractiveImpl extends MobileInteractive {


    private static MobileInteractiveImpl instance;
    private final Context context;
    private Set<NotificationCategory> customNotificationCategories;
    private Set<NotificationCategory> predefinedNotificationCategories;


    private MobileInteractiveImpl(Context context) {
        this.context = context;
    }

    public static MobileInteractiveImpl getInstance(Context context) {
        if (instance != null) {
            return instance;
        }

        instance = new MobileInteractiveImpl(context);
        return instance;
    }

    @Override
    public void setNotificationCategories(NotificationCategory... notificationCategories) {
        validateWithParam(notificationCategories);
        this.predefinedNotificationCategories = getPredefinedNotificationCategories();
        setCustomNotificationCategories(notificationCategories);
    }

    public Set<NotificationCategory> getNotificationCategories() {
        if (!isDisplayNotificationEnabled()) {
            return null;
        }

        Set<NotificationCategory> notificationCategories = getPredefinedNotificationCategories();
        Set<NotificationCategory> customNotificationCategories = getCustomNotificationCategories();
        notificationCategories.addAll(customNotificationCategories);

        return notificationCategories;
    }

    Set<NotificationCategory> getPredefinedNotificationCategories() {
        if (null != predefinedNotificationCategories) {
            return predefinedNotificationCategories;
        }

        return PredefinedNotificationCategories.load();
    }

    void setCustomNotificationCategories(NotificationCategory[] notificationCategories) {
        if (notificationCategories == null) {
            return;
        }

        if (notificationCategories.length == 0) {
            this.customNotificationCategories = null;
        } else {
            this.customNotificationCategories = new HashSet<>(Arrays.asList(notificationCategories));
        }

        final Set<String> customNotificationCategoriesStringSet = new HashSet<>();
        for (NotificationCategory customNotificationCategory : notificationCategories) {
            customNotificationCategoriesStringSet.add(customNotificationCategory.toString());
        }
        PreferenceHelper.saveStringSet(context, MobileMessagingProperty.INTERACTIVE_CATEGORIES, customNotificationCategoriesStringSet);
    }

    @NonNull
    Set<NotificationCategory> getCustomNotificationCategories() {
        if (null != customNotificationCategories) {
            return customNotificationCategories;
        }

        Set<String> notificationCategoriesStringSet = PreferenceHelper.findStringSet(context, MobileMessagingProperty.INTERACTIVE_CATEGORIES);
        Set<NotificationCategory> notificationCategoriesTemp = new HashSet<>();
        if (notificationCategoriesStringSet != MobileMessagingProperty.INTERACTIVE_CATEGORIES.getDefaultValue()) {
            for (String category : notificationCategoriesStringSet) {
                NotificationCategory notificationCategory = new JsonSerializer().deserialize(category, NotificationCategory.class);
                notificationCategoriesTemp.add(notificationCategory);
            }
        }
        this.customNotificationCategories = notificationCategoriesTemp;

        return customNotificationCategories;
    }

    private boolean isDisplayNotificationEnabled() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED);
    }

    private void validateWithParam(Object o) {
        if (null != o) {
            return;
        }
        throw new IllegalArgumentException("Can't use 'set' method with null argument!");
    }
}
