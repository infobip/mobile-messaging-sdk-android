package org.infobip.mobile.messaging.util;

import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import org.infobip.mobile.messaging.MobileMessagingLogger;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author sslavin
 * @since 29/08/16.
 */
public class Cryptor {

    private static String AES_ALGO = "AES/ECB/PKCS5Padding";
    private Key key = null;

    public Cryptor(@NonNull String keySecret) {
        byte keyBytes[] = keySecret.getBytes();
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            MobileMessagingLogger.d(Log.getStackTraceString(e));
            return;
        }
        keyBytes = sha.digest(keyBytes);
        keyBytes = Arrays.copyOf(keyBytes, 16);
        key = new SecretKeySpec(keyBytes, AES_ALGO);
    }

    public String encrypt(String data) {
        if (StringUtils.isBlank(data)) {
            return null;
        }

        byte encoded[] = encodeAES128(data.getBytes());
        return Base64.encodeToString(encoded, Base64.NO_WRAP);
    }

    public String decrypt(String encryptedBase64Data) {
        if (StringUtils.isBlank(encryptedBase64Data)) {
            return null;
        }

        byte encrypted[] = Base64.decode(encryptedBase64Data, Base64.DEFAULT);
        byte decrypted[] = decodeAES128(encrypted);
        return new String(decrypted);
    }

    private byte[] encodeAES128(byte data[]) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception e) {
            MobileMessagingLogger.d(Log.getStackTraceString(e));
            return null;
        }
    }

    private byte[] decodeAES128(byte data[]) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception e) {
            MobileMessagingLogger.d(Log.getStackTraceString(e));
            return null;
        }
    }
}
