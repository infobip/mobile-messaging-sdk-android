package org.infobip.mobile.messaging.chat.properties;

import android.content.Context;

import androidx.annotation.Nullable;

import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sslavin
 * @since 01/11/2017.
 */

public class PropertyHelper extends PreferenceHelper {

    private final Context context;

    public PropertyHelper(Context context) {
        this.context = context;
    }

    public boolean findBoolean(MobileMessagingChatProperty property) {
        return findBoolean(context, property.getKey(), (Boolean) property.getDefaultValue());
    }

    public void saveBoolean(MobileMessagingChatProperty property, boolean value) {
        saveBoolean(context, property.getKey(), value);
    }

    public int findInt(MobileMessagingChatProperty property) {
        return findInt(context, property.getKey(), (int) property.getDefaultValue());
    }

    public void saveInt(MobileMessagingChatProperty property, int value) {
        saveInt(context, property.getKey(), value);
    }

    public void saveString(MobileMessagingChatProperty property, String value) {
        saveString(context, property.getKey(), value);
    }

    @Nullable
    public String findString(MobileMessagingChatProperty property){
        return findString(context, property.getKey(), (String) property.getDefaultValue());
    }

    public Class[] findClasses(MobileMessagingChatProperty property) {
        String[] classNames = findStringArray(context, property.getKey(), new String[0]);
        if (classNames == null) {
            return (Class[]) property.getDefaultValue();
        }

        List<Class> classes = new ArrayList<>(classNames.length);
        for (String className : classNames) {
            try {
                classes.add(Class.forName(className));
            } catch (Exception ignored) {
            }
        }
        return classes.toArray(new Class[0]);
    }

    public void saveClasses(MobileMessagingChatProperty property, Class... classes) {
        if (classes == null) {
            return;
        }

        List<String> classNames = new ArrayList<>(classes.length);
        for (Class cls : classes) {
            if (cls == null) {
                continue;
            }
            String value = cls.getName();
            classNames.add(value);
        }
        saveStringArray(context, property.getKey(), classNames.toArray(new String[0]));
    }

    public void remove(MobileMessagingChatProperty property) {
        remove(context, property.getKey());
    }

}
