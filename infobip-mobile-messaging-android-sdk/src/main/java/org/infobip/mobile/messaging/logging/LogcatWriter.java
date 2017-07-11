package org.infobip.mobile.messaging.logging;

import android.util.Log;

/**
 * @author sslavin
 * @since 11/07/2017.
 */

public class LogcatWriter implements Writer {

    @Override
    public void write(Level level, String tag, String message, Throwable throwable) {
        switch (level) {
            case VERBOSE:
                Log.v(tag, message, throwable);
                break;
            case DEBUG:
                Log.d(tag, message, throwable);
                break;
            case INFO:
                Log.i(tag, message, throwable);
                break;
            case WARN:
                Log.w(tag, message, throwable);
                break;
            case ERROR:
                Log.e(tag, message, throwable);
                break;
        }
    }
}
