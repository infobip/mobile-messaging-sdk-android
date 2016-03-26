package org.infobip.mobile.messaging.util;

/**
 * @author mstipanov
 * @since 07.03.2016.
 */
public abstract class StringUtils {
    private StringUtils() {
    }

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public static boolean isBlank(String s) {
        return null == s || s.trim().isEmpty();
    }
}
