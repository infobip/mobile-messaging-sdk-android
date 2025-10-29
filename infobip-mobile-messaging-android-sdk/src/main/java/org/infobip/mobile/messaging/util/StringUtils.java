/*
 * StringUtils.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.util;

import java.util.Locale;

import androidx.annotation.NonNull;

/**
 * @author mstipanov
 * @since 07.03.2016.
 */
public abstract class StringUtils {

    public static final String COMMA_WITH_SPACE = ", ";

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

    public static String concat(String s1, String s2, String separator) {
        return String.format(Locale.getDefault(), "%s%s%s", s1, separator, s2);
    }

    public static String concat(String[] stringArray, String separator) {
        return StringUtils.join(separator, stringArray);
    }

    public static String join(CharSequence delimiter, CharSequence... values) {
        return join(delimiter, "", "", values);
    }

    // Can be performed by StringJoiner logic from jdk 9
    public static String join(@NonNull CharSequence delimiter,
                              @NonNull CharSequence prefix,
                              @NonNull CharSequence suffix,
                              @NonNull CharSequence... values) {
        StringBuilder joiner = new StringBuilder().append(prefix);

        if (values.length > 0) {
            joiner.append(values[0]);
            int valuesLen = values.length;
            for (int i = 1; i < valuesLen; i++) {
                joiner.append(delimiter).append(values[i]);
            }
        }
        joiner.append(suffix);

        return joiner.toString();
    }
}
