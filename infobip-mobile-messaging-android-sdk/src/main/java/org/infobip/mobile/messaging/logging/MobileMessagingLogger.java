/*
 * MobileMessagingLogger.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.logging;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;

public final class MobileMessagingLogger {

    public static final String TAG = "MobileMessaging";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private static Writer writer = new LogcatWriter();
    private static boolean isEnforced = false;


    public static void init(Context context) {
        MobileMessagingLogger.context = context;
    }

    public static void enforce() {
        isEnforced = true;
    }

    public static void setWriter(Writer logWriter) {
        if (logWriter == null) {
            throw new IllegalArgumentException("Log writer should not be null");
        }
        writer = logWriter;
    }

    public static boolean loggingEnabled() {
        boolean isDebuggable = (context != null && 0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        return isDebuggable || isEnforced;
    }

    public static void v(String tag, String msg) {
        log(Level.VERBOSE, tag, msg, null);
    }

    public static void v(String msg) {
        v(TAG, msg);
    }

    public static void v(String msg, Object o) {
        log(Level.VERBOSE, TAG, msg, o);
    }

    public static void v(String msg, Object o, Object... os) {
        log(Level.VERBOSE, TAG, msg, Arrays.asList(o, os));
    }

    public static void v(String tag, String msg, Throwable tr) {
        log(Level.VERBOSE, tag, msg, tr);
    }

    public static void v(String msg, Throwable tr) {
        v(TAG, msg, tr);
    }

    public static void d(String tag, String msg) {
        log(Level.DEBUG, tag, msg, null);
    }

    public static void d(String msg) {
        d(TAG, msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        log(Level.DEBUG, tag, msg, tr);
    }

    public static void d(String msg, Throwable tr) {
        d(TAG, msg, tr);
    }

    public static void i(String tag, String msg) {
        log(Level.INFO, tag, msg, null);
    }

    public static void i(String msg) {
        i(TAG, msg);
    }

    public static void i(String tag, String msg, Throwable tr) {
        log(Level.INFO, tag, msg, tr);
    }

    public static void i(String msg, Throwable tr) {
        i(TAG, msg, tr);
    }

    public static void w(String tag, String msg) {
        log(Level.WARN, tag, msg, null);
    }

    public static void w(String msg) {
        w(TAG, msg);
    }

    public static void w(String tag, String msg, Throwable tr) {
        log(Level.WARN, tag, msg, tr);
    }

    public static void w(String msg, Throwable tr) {
        w(TAG, msg, tr);
    }

    public static void e(String tag, String msg) {
        log(Level.ERROR, tag, msg, null);
    }

    public static void e(String msg) {
        e(TAG, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        log(Level.ERROR, tag, msg, tr);
    }

    public static void e(String msg, Throwable tr) {
        e(TAG, msg, tr);
    }

    public static String objectToPrettyString(Object o) {
        if (!loggingEnabled()) {
            return "";
        }

        return gson.toJson(o);
    }

    private static void log(Level level, String tag, String msg, Object o) {
        if (loggingEnabled() || level == Level.ERROR) {
            log(level, tag, msg + "\n" + objectToPrettyString(o), null);
        }
    }

    private static void log(Level level, String tag, String msg, Throwable tr) {
        if (loggingEnabled() || level == Level.ERROR) {
            writer.write(level, tag, msg, tr);
        }
    }
}
