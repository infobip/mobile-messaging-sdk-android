package org.infobip.mobile.messaging.api.support.util;

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

    public static String join(String separator, String... uris) {
        if (null == uris || uris.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean endingWithSeparator = true;
        for (String uri : uris) {
            if (null == uri) {
                uri = "";
            }
            if (!endingWithSeparator && !uri.startsWith(separator)) {
                sb.append(separator);
                endingWithSeparator = true;
            }

            if (endingWithSeparator && uri.startsWith(separator) && sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }

            sb.append(uri);
            endingWithSeparator = uri.endsWith(separator);
        }
        return sb.toString();
    }

    public static boolean equals(String one, String two) {
        if (isBlank(one) && isBlank(two)) {
            return true;
        }

        return ("" + one).equals(two);
    }
}
