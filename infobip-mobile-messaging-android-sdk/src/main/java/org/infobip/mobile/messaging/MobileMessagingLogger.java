package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

public final class MobileMessagingLogger {

    public static final String TAG = "MobileMessaging";
    private static Context context;
    private static boolean forceEnable;

    public static void init(Context context) {
        MobileMessagingLogger.context = context;
    }

    public static void forceEnable() {
        forceEnable = true;
    }

    public static void v(String tag, String msg) {
        log(Log.VERBOSE, tag, msg, null);
    }

    public static void v(String msg) {
        v(TAG, msg);
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

    private static void log(int logLevel, String tag, String msg, Throwable tr) {
        boolean isDebuggable = (context != null && 0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        if (!isDebuggable && !forceEnable) {
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
