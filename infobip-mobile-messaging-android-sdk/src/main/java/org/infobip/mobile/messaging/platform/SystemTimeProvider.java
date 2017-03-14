package org.infobip.mobile.messaging.platform;

/**
 * @author sslavin
 * @since 14/03/2017.
 */

public class SystemTimeProvider implements TimeProvider {
    @Override
    public long now() {
        return System.currentTimeMillis();
    }
}
