package org.infobip.mobile.messaging.interactive;


import android.os.Bundle;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.interactive.dal.bundle.NotificationCategoryBundleMapper;

public class NotificationCategory {

    static final String MM_INTERACTIVE_ID_PREFIX = "mm_";

    private static final JsonSerializer jsonSerializer = new JsonSerializer(true);

    private String categoryId;
    private NotificationAction[] notificationActions;

    /**
     * ID used for distinguishing action categories. Maximum of 3 actions can be shown in notification view.
     *
     * @param categoryId          Category ID. "mm_" prefix is reserved for Mobile Messaging IDs and cannot be used as a prefix.
     * @param notificationActions Actions
     */
    public NotificationCategory(@NonNull String categoryId, @NonNull NotificationAction... notificationActions) {
        validateCategoryId(categoryId);
        this.categoryId = categoryId;
        this.notificationActions = notificationActions;
    }

    public NotificationCategory(boolean predefined, @NonNull String categoryId, @NonNull NotificationAction... notificationActions) {
        if (!predefined) {
            validateCategoryId(categoryId);
        }
        this.categoryId = categoryId;
        this.notificationActions = notificationActions;
    }

    private NotificationCategory(String categoryString) {
        NotificationCategory data = jsonSerializer.deserialize(categoryString, NotificationCategory.class);
        this.categoryId = data.categoryId;
        this.notificationActions = data.notificationActions;
    }

    public static NotificationCategory createFrom(Bundle bundle) {
        return NotificationCategoryBundleMapper.notificationCategoryFromBundle(bundle);
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
        return jsonSerializer.serialize(this);
    }
}
