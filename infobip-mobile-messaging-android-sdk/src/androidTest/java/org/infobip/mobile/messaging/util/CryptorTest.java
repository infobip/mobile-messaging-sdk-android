package org.infobip.mobile.messaging.util;

import android.test.InstrumentationTestCase;
import android.util.Base64;

/**
 * @author sslavin
 * @since 29/08/16.
 */
public class CryptorTest extends InstrumentationTestCase {

    public void test_encryptDecrypt() throws Exception {

        String data = "thisIsMyTestData";
        String key = "thisIsMySuperSecretKey";
        String encrypted = new Cryptor(key).encrypt(data);
        String decrypted = new Cryptor(key).decrypt(encrypted);

        assertFalse(data.equals(encrypted));
        assertFalse(data.equals(new String(Base64.decode(encrypted, Base64.DEFAULT))));
        assertEquals(data, decrypted);
    }
}