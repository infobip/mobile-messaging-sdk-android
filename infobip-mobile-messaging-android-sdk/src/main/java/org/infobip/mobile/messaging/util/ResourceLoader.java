/*
 * ResourceLoader.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
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

    public static String loadStringResourceByName(final Context context, final String resourceName) {
        int resource = 0;
        try {
            resource = loadResourceByName(context, "string", resourceName);
        } catch (MissingAndroidResourceException ignore) {

        }
        if (resource > 0) {
            String className = context.getResources().getString(resource);
            if (StringUtils.isNotBlank(className)) {
                return className;
            }
        }
        return null;
    }
}
