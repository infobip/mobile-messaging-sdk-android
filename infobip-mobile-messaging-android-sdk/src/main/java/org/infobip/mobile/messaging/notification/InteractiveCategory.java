package org.infobip.mobile.messaging.notification;


import android.os.Bundle;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.api.shaded.google.gson.Gson;
import org.infobip.mobile.messaging.api.shaded.google.gson.GsonBuilder;
import org.infobip.mobile.messaging.dal.bundle.InteractiveCategoryBundleMapper;

public class InteractiveCategory {

    public static final String MM_INTERACTIVE_ID_PREFIX = "mm_";

    private String categoryId;
    private NotificationAction[] notificationActions;

    /**
     * ID used for distinguishing action categories. Maximum of 3 actions can be shown in notification view.
     *
     * @param categoryId          Category ID. "mm_" prefix is reserved for Mobile Messaging IDs and cannot be used as a prefix.
     * @param notificationActions Actions
     */
    public InteractiveCategory(@NonNull String categoryId, @NonNull NotificationAction... notificationActions) {
        validateCategoryId(categoryId);
        this.categoryId = categoryId;
        this.notificationActions = notificationActions;
    }

    private InteractiveCategory(String categoryString) {
        Gson gson = new Gson();
        InteractiveCategory data = gson.fromJson(categoryString, InteractiveCategory.class);
        this.categoryId = data.categoryId;
        this.notificationActions = data.notificationActions;
    }

    public static InteractiveCategory createFrom(Bundle bundle) {
        return InteractiveCategoryBundleMapper.interactiveCategoryFromBundle(bundle);
    }

    private void validateCategoryId(String categoryId) {
        if (null == categoryId) {
            throw new IllegalArgumentException("Can't use 'with' method with null argument!");
        }

        if (categoryId.startsWith(MM_INTERACTIVE_ID_PREFIX)) {
            throw new IllegalArgumentException(String.format("'%s' prefix is reserved for Mobile Messaging library", MM_INTERACTIVE_ID_PREFIX));
        }
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        validateCategoryId(categoryId);
        this.categoryId = categoryId;
    }

    public NotificationAction[] getNotificationActions() {
        return notificationActions;
    }

    public void setNotificationActions(NotificationAction[] notificationActions) {
        this.notificationActions = notificationActions;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
