package org.infobip.mobile.messaging.util;

import android.content.Context;

/**
 * @author mstipanov
 * @since 25.03.2016.
 */
public abstract class ResourceLoader {
    private ResourceLoader() {
    }

    public static int loadResourceByName(final Context context, final String resourceGroup, final String resourceName) {
        String className = context.getApplicationContext().getPackageName() + ".R$" + resourceGroup;
        try {
            Class<?> aClass = Class.forName(className);
            return (int) aClass.getDeclaredField(resourceName).get(aClass);
        } catch (Exception e) {
            throw new MissingAndroidResourceException("Can't load resource: " + resourceName, className, resourceName, e);
        }
    }
}
