/*
 * ReflectionUtils.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.api.support.util;

import java.io.IOException;
import java.net.URL;

/**
 * @author mstipanov
 * @since 07.03.2016.
 */
public abstract class ReflectionUtils {
    private ReflectionUtils() {
    }

    public static void loadPackageInfo(String packageName) throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        URL pgkInfo = classLoader.getResource(packageName.replace('.', '/') + "/package-info.class");
        if (null != pgkInfo) {
            Class.forName(packageName + '.' + "package-info");
        }
    }
}
