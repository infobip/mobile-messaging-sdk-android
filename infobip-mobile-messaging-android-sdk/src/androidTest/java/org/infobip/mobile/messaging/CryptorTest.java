package org.infobip.mobile.messaging;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Base64;

import org.infobip.mobile.messaging.util.Cryptor;

/**
 * @author sslavin
 * @since 29/08/16.
 */
public class CryptorTest extends InstrumentationTestCase {

    Context context = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getContext();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

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