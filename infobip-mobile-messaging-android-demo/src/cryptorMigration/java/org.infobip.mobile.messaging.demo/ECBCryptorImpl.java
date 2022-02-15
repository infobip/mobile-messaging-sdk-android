package org.infobip.mobile.messaging.demo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import org.infobip.mobile.messaging.util.Cryptor;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Used only to migrate previously saved data.
 * ECB encryption mode should not be used, because it's not secure.
 */
@Deprecated
public class ECBCryptorImpl extends Cryptor {

    private static final String AES_ALGO = "AES/ECB/PKCS5Padding";
    private Key key = null;
    private final String TAG = "ECBCryptorImpl_TAG";

    public ECBCryptorImpl(@NonNull String keySecret) {
        byte[] keyBytes = keySecret.getBytes();
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, Log.getStackTraceString(e));
            return;
        }
        keyBytes = sha.digest(keyBytes);
        keyBytes = Arrays.copyOf(keyBytes, 16);
        key = new SecretKeySpec(keyBytes, AES_ALGO);
    }

    @Override
    @Nullable
    public String encrypt(String data) {
        if (isBlank(data)) {
            return null;
        }

        byte[] encoded = encodeAES128(data.getBytes());
        return Base64.encodeToString(encoded, Base64.NO_WRAP);
    }

    @Override
    @Nullable
    public String decrypt(String encryptedBase64Data) {
        if (isBlank(encryptedBase64Data)) {
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
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    private byte[] decodeAES128(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
            return null;
        }
    }

    private boolean isBlank(String s) {
        return null == s || s.trim().isEmpty();
    }
}

