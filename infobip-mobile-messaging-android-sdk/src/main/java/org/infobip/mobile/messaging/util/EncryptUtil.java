package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author sslavin
 * @since 29/08/16.
 */
public class EncryptUtil {

    private static String AES_ALGO = "AES/ECB/PKCS5Padding";
    private static Key key = null;

    private static Key getKey(String algorithm, Context context) {
        if (key != null && key.getAlgorithm().equals(algorithm)) {
            return key;
        }

        byte keyBytes[] = DeviceInformation.getDeviceID(context).getBytes();
        key = new SecretKeySpec(keyBytes, algorithm);
        return key;
    }

    public static String encrypt(Context context, String data) {
        if (data == null) {
            return null;
        }

        byte encoded[] = encodeAES128(context, data.getBytes());
        return Base64.encodeToString(encoded, Base64.DEFAULT);
    }

    public static String decrypt(Context context, String encryptedBase64Data) {
        if (encryptedBase64Data == null) {
            return null;
        }

        byte encrypted[] = Base64.decode(encryptedBase64Data, Base64.DEFAULT);
        byte decrypted[] = decodeAES128(context, encrypted);
        return new String(decrypted);
    }

    private static byte[] encodeAES128(Context context, byte data[]) {
        Key key = getKey(AES_ALGO, context);
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(data);
        } catch (Exception e) {
            MobileMessagingLogger.d(Log.getStackTraceString(e));
            return null;
        }
    }

    private static byte[] decodeAES128(Context context, byte data[]) {
        Key key = getKey(AES_ALGO, context);
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
