package org.infobip.mobile.messaging.util;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.Base64;

/**
 * @author sslavin
 * @since 29/08/16.
 */
public class EncryptUtilTest extends InstrumentationTestCase {

    private Context context = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getContext();
    }

    public void test_encryptDecrypt() throws Exception {

        String data = "thisIsMyTestData";
        String encrypted = EncryptUtil.encrypt(context, data);
        String decrypted = EncryptUtil.decrypt(context, encrypted);

        assertFalse(data.equals(encrypted));
        assertFalse(data.equals(new String(Base64.decode(encrypted, Base64.DEFAULT))));
        assertEquals(data, decrypted);
    }
}