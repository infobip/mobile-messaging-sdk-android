package org.infobip.mobile.messaging.util;

import android.util.Base64;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class CryptorImpl extends Cryptor {

    private static final String AES_ALGO = "AES/CBC/PKCS7PADDING";
    private Key key = null;
    private IvParameterSpec ivSpec = null;

    public CryptorImpl(@NonNull String keySecret) {
        byte[] keyBytes = keySecret.getBytes();
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            MobileMessagingLogger.e("Cryptor initialization failed.", e);
            return;
        }
        keyBytes = sha.digest(keyBytes);
        keyBytes = Arrays.copyOf(keyBytes, 16);
        key = new SecretKeySpec(keyBytes, AES_ALGO);

        ivSpec = new IvParameterSpec(Arrays.copyOf(keySecret.getBytes(), 16));
    }

    @Override
    @Nullable
    public String encrypt(String data) {
        if (StringUtils.isBlank(data)) {
            return null;
        }

        byte[] encoded = encodeAES128(data.getBytes());
        return Base64.encodeToString(encoded, Base64.NO_WRAP);
    }

    @Override
    @Nullable
    public String decrypt(String encryptedBase64Data) {
        if (StringUtils.isBlank(encryptedBase64Data)) {
            return null;
        }

        byte[] encrypted = Base64.decode(encryptedBase64Data, Base64.DEFAULT);
        byte[] decrypted = decodeAES128(encrypted);
        if (decrypted == null) {
            return null;
        }
        return new String(decrypted);
    }

    private byte[] encodeAES128(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            MobileMessagingLogger.e("Data encryption failed.", e);
            return null;
        }
    }

    private byte[] decodeAES128(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            MobileMessagingLogger.e("Data decryption failed.", e);
            return null;
        }
    }
}
