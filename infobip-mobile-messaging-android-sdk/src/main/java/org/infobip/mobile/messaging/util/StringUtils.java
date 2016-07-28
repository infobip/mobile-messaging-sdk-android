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

    public static boolean isEqual(String s1, String s2) {
        return s1 == null && s2 == null || s1 != null && s1.equals(s2);

    }

}
