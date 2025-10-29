/*
 * ExceptionUtils.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.util;

import android.util.Log;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author mstipanov
 * @since 04.04.2016.
 */
public abstract class ExceptionUtils {
    private ExceptionUtils() {
    }

    public static String stacktrace(Throwable e) {
        if (null == e) {
            return null;
        }

        PrintWriter writer = null;
        try {
            StringWriter sw = new StringWriter();
            writer = new PrintWriter(sw);
            MobileMessagingLogger.d(Log.getStackTraceString(e));
            return sw.toString();
        } finally {
            if (null != writer) {
                try {
                    writer.close();
                } catch (Exception e1) {
                    //ignore
                }
            }
        }
    }
}
