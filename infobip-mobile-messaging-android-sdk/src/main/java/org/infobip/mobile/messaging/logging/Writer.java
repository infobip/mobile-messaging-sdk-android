package org.infobip.mobile.messaging.logging;

import androidx.annotation.Nullable;

/**
 * @author sslavin
 * @since 11/07/2017.
 */

public interface Writer {
    void write(Level level, String tag, String message, @Nullable Throwable throwable);
}
