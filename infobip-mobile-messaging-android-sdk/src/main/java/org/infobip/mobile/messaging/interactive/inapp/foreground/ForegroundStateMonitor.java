package org.infobip.mobile.messaging.interactive.inapp.foreground;

import androidx.annotation.NonNull;

/**
 * @author sslavin
 * @since 18/04/2018.
 */
public interface ForegroundStateMonitor {
    @NonNull ForegroundState isInForeground();
}
