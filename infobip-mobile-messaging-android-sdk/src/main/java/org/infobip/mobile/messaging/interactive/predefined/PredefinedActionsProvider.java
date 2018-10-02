package org.infobip.mobile.messaging.interactive.predefined;

import android.content.Context;

import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.util.HashSet;
import java.util.Set;

/**
 * @author sslavin
 * @since 02/10/2018.
 */
public class PredefinedActionsProvider {

    private static final String TAG = PredefinedActionsProvider.class.getSimpleName();
    private static final String MM_BUTTON_ACCEPT_TEXT_RES_NAME = "mm_button_accept";
    private static volatile Boolean isResourceLibraryAvailable = null;

    private final Context context;

    public PredefinedActionsProvider(Context context) {
        this.context = context;
    }

    public NotificationAction[] getDefaultInAppActions() {
        if (isResourceLibraryUnavailable()) {
            MobileMessagingLogger.w(TAG, "Using limited number of default actions for in-app dialogs, \"Open\" button might be unavailable, include 'infobip-mobile-messaging-android-resources' dependency to enable all default buttons");
            return PredefinedNotificationAction.defaultInAppActionsWhenNoResources();
        }
        return PredefinedNotificationAction.defaultInAppActions();
    }

    public Set<NotificationCategory> getPredefinedCategories() {
        if (isResourceLibraryUnavailable()) {
            return new HashSet<>();
        }
        return PredefinedNotificationCategories.load();
    }

    public static boolean isOpenAction(String actionId) {
        return PredefinedNotificationAction.PredefinedActionIds.mm_open.name().equals(actionId);
    }

    public void verifyResourcesForCategory(String categoryId) {
        if (isResourceLibraryAvailable()) {
            return;
        }

        try {
            PredefinedNotificationCategories.PredefinedCategoryIds.valueOf(categoryId);
            MobileMessagingLogger.w(TAG, "Resources for [" + categoryId + "] notification category are not found in your project, make sure to include 'infobip-mobile-messaging-android-resources' dependency");
        } catch (Throwable ignored) {
        }
    }

    private boolean isResourceLibraryUnavailable() {
        return !isResourceLibraryAvailable();
    }

    private boolean isResourceLibraryAvailable() {
        if (isResourceLibraryAvailable != null) {
            return isResourceLibraryAvailable;
        }
        isResourceLibraryAvailable = context.getResources().getIdentifier(MM_BUTTON_ACCEPT_TEXT_RES_NAME, "string", context.getPackageName()) != 0;
        return isResourceLibraryAvailable;
    }
}
