/*
 * SHA256.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.util;

import java.security.MessageDigest;

import androidx.annotation.NonNull;

public class SHA256 {
    public static @NonNull String calc(String str) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest(str.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : result) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
