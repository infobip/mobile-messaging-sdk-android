package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import org.infobip.mobile.messaging.api.shaded.google.gson.Gson;
import org.infobip.mobile.messaging.api.shaded.google.gson.GsonBuilder;

import java.util.Arrays;

public final class MobileMessagingLogger {

    public static final String TAG = "MobileMessaging";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static Context context;
    private static boolean isEnforced = false;

    public static void init(Context context) {
        MobileMessagingLogger.context = context;
    }

    public static void enforce() {
        isEnforced = true;
    }

    public static boolean loggingEnabled() {
        boolean isDebuggable = (context != null && 0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        return isDebuggable || isEnforced;
    }

    public static void v(String tag, String msg) {
        log(Log.VERBOSE, tag, msg, null);
    }

    public static void v(String msg) {
        v(TAG, msg);
    }

    public static void v(String msg, Object o) {
        log(Log.VERBOSE, TAG, msg, o);
    }

    public static void v(String msg, Object o, Object...os) {
        log(Log.VERBOSE, TAG, msg, Arrays.asList(o, os));
    }

    public static void v(String tag, String msg, Throwable tr) {
        log(Log.VERBOSE, tag, msg, tr);
    }

    public static void v(String msg, Throwable tr) {
        v(TAG, msg, tr);
    }

    public static void d(String tag, String msg) {
        log(Log.DEBUG, tag, msg, null);
    }

    public static void d(String msg) {
        d(TAG, msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        log(Log.DEBUG, tag, msg, tr);
    }

    public static void d(String msg, Throwable tr) {
        d(TAG, msg, tr);
    }

    public static void i(String tag, String msg) {
        log(Log.INFO, tag, msg, null);
    }

    public static void i(String msg) {
        i(TAG, msg);
    }

    public static void i(String tag, String msg, Throwable tr) {
        log(Log.INFO, tag, msg, tr);
    }

    public static void i(String msg, Throwable tr) {
        i(TAG, msg, tr);
    }

    public static void w(String tag, String msg) {
        log(Log.WARN, tag, msg, null);
    }

    public static void w(String msg) {
        w(TAG, msg);
    }

    public static void w(String tag, String msg, Throwable tr) {
        log(Log.WARN, tag, msg, tr);
    }

    public static void w(String msg, Throwable tr) {
        w(TAG, msg, tr);
    }

    public static void e(String tag, String msg) {
        log(Log.ERROR, tag, msg, null);
    }

    public static void e(String msg) {
        e(TAG, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        log(Log.ERROR, tag, msg, tr);
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

    private static void log(int logLevel, String tag, String msg, Object o) {
        if (!loggingEnabled()) {
            return;
        }

        log(logLevel, tag, msg + "\n" + objectToPrettyString(o), null);
    }

    private static void log(int logLevel, String tag, String msg, Throwable tr) {
        if (!loggingEnabled()) {
            return;
        }

        switch (logLevel) {
            case Log.VERBOSE:
                Log.v(tag, msg, tr);
                break;
            case Log.DEBUG:
                Log.d(tag, msg, tr);
                break;
            case Log.INFO:
                Log.i(tag, msg, tr);
                break;
            case Log.WARN:
                Log.w(tag, msg, tr);
                break;
            case Log.ERROR:
                Log.e(tag, msg, tr);
                break;
        }
    }
}
