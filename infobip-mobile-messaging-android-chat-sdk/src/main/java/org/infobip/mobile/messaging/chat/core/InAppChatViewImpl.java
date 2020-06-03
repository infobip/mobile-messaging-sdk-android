package org.infobip.mobile.messaging.chat.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.infobip.mobile.messaging.chat.InAppChatView;
import org.infobip.mobile.messaging.chat.properties.InAppChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.chat.view.InAppChatActivity;
import org.infobip.mobile.messaging.util.StringUtils;


public class InAppChatViewImpl implements InAppChatView {

    private final Context context;
    private final PropertyHelper propertyHelper;

    @SuppressWarnings("unused")
    private InAppChatViewImpl() {
        context = null;
        propertyHelper = null;
    }

    public InAppChatViewImpl(Context context) {
        this.context = context;
        this.propertyHelper = new PropertyHelper(context);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void show() {
        propertyHelper.saveClasses(InAppChatProperty.ON_MESSAGE_TAP_ACTIVITY_CLASSES, getTapActivityClasses());
        context.startActivity(new Intent(context, InAppChatActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    // region private methods

    private Class[] getTapActivityClasses() {
        Class launchActivityClass = getLaunchActivityClass();
        if (launchActivityClass != null) {
            return new Class[]{launchActivityClass, InAppChatActivity.class};
        }
        return new Class[]{InAppChatActivity.class};
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
