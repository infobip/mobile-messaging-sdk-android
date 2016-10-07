package org.infobip.mobile.messaging.util;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.infobip.mobile.messaging.MobileMessaging.TAG;

/**
 * @author mstipanov
 * @since 04.04.2016.
 */
public abstract class ExceptionUtils {
    private ExceptionUtils() {
    }

    public static String stacktrace(Exception e) {
        if (null == e) {
            return null;
        }

        PrintWriter writer = null;
        try {
            StringWriter sw = new StringWriter();
            writer = new PrintWriter(sw);
            Log.d(TAG, Log.getStackTraceString(e));
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
