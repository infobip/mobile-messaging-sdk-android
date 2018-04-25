package org.infobip.mobile.messaging.app;

import android.support.annotation.NonNull;

/**
 * @author sslavin
 * @since 18/04/2018.
 */
public interface ForegroundStateMonitor {
    @NonNull ForegroundState isInForeground();
}
