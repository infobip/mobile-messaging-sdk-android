package org.infobip.mobile.messaging.chat.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.infobip.mobile.messaging.chat.MobileChatView;
import org.infobip.mobile.messaging.chat.properties.MobileChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.chat.view.ChatActivity;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author sslavin
 * @since 19/11/2017.
 */

public class MobileChatViewImpl implements MobileChatView {

    private static final Set<NotificationCategory> notificationCategories = new HashSet<>();
    private final Context context;
    private final PropertyHelper propertyHelper;

    @SuppressWarnings("unused")
    private MobileChatViewImpl() {
        context = null;
        propertyHelper = null;
    }

    MobileChatViewImpl(Context context) {
        this.context = context;
        this.propertyHelper = new PropertyHelper(context);
    }

    @Override
    public MobileChatView withActionCategories(NotificationCategory... categories) {
        notificationCategories.clear();
        notificationCategories.addAll(Arrays.asList(categories));
        return this;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void show() {
        propertyHelper.saveClasses(MobileChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES, getTapActivityClasses());
        context.startActivity(new Intent(context, ChatActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public Set<NotificationCategory> getNotificationCategories() {
        return notificationCategories;
    }

    // region private methods

    private Class[] getTapActivityClasses() {
        Class launchActivityClass = getLaunchActivityClass();
        if (launchActivityClass != null) {
            return new Class[]{launchActivityClass, ChatActivity.class};
        }
        return new Class[]{ChatActivity.class};
    }

    @Nullable
    private Class getLaunchActivityClass() {
        @SuppressWarnings("ConstantConditions")
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchIntent == null) {
            return null;
        }

        ComponentName componentName = launchIntent.getComponent();
        if (componentName == null) {
            return null;
        }

        String className = componentName.getClassName();
        if (StringUtils.isBlank(className)) {
            return null;
        }

        try {
            return Class.forName(className);
        } catch (Exception ignored) {
        }
        return null;
    }

    // endregion
}
