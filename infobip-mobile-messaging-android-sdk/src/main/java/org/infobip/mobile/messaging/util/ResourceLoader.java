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
        try {
            return context.getResources().getIdentifier(resourceName, resourceGroup, context.getApplicationContext().getPackageName());
        } catch (Exception e) {
            String className = context.getApplicationContext().getPackageName() + ".R$" + resourceGroup;
            throw new MissingAndroidResourceException("Can't load resource: " + resourceName, className, resourceName, e);
        }
    }
}
