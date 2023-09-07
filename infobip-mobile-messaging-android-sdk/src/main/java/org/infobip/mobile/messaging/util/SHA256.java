package org.infobip.mobile.messaging.util;

import androidx.annotation.NonNull;

import java.security.MessageDigest;

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
