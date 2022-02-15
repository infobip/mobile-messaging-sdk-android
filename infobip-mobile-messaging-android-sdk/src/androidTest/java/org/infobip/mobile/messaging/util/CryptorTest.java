package org.infobip.mobile.messaging.util;

import android.util.Base64;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author sslavin
 * @since 29/08/16.
 */
public class CryptorTest extends MobileMessagingTestCase {

    @Test
    public void test_encryptDecrypt() throws Exception {
        String data = "thisIsMyTestData";
        String key = "thisIsMySuperSecretKey";
        String iv = "thisIsInitializationVector";
        String encrypted = new CryptorImpl(key).encrypt(data);
        String decrypted = new CryptorImpl(key).decrypt(encrypted);

        assertFalse(data.equals(encrypted));
        assertFalse(data.equals(new String(Base64.decode(encrypted, Base64.DEFAULT))));
        assertEquals(data, decrypted);
    }

    @Test
    public void test_deprecated_encryptDecrypt() throws Exception {
        String data = "thisIsMyTestData";
        String key = "thisIsMySuperSecretKey";
        String encrypted = new ECBCryptorImpl(key).encrypt(data);
        String decrypted = new ECBCryptorImpl(key).decrypt(encrypted);

        assertFalse(data.equals(encrypted));
        assertFalse(data.equals(new String(Base64.decode(encrypted, Base64.DEFAULT))));
        assertEquals(data, decrypted);
    }

    @Test
    public void test_ThatCryptedPropertiesMigrated() throws Exception {

        MobileMessagingProperty key1 = MobileMessagingProperty.APPLICATION_CODE;
        MobileMessagingProperty key3 = MobileMessagingProperty.INFOBIP_REGISTRATION_ID;
        MobileMessagingProperty key4 = MobileMessagingProperty.CLOUD_TOKEN;

        String value = "some value";

        // saving using old cryptor
        Cryptor oldCryptor =  new ECBCryptorImpl(DeviceInformation.getDeviceID(context));
        PreferenceHelper.cryptor = oldCryptor;
        PreferenceHelper.saveString(context, key1, value);
        PreferenceHelper.saveString(context, key3, value);
        PreferenceHelper.saveString(context, key4, value);
        assertEquals(PreferenceHelper.findString(context, key1), value);
        assertEquals(PreferenceHelper.findString(context, key3), value);
        assertEquals(PreferenceHelper.findString(context, key4), value);

        //set new cryptor
        PreferenceHelper.cryptor = new CryptorImpl(DeviceInformation.getDeviceID(context));

        assertTrue(PreferenceHelper.shouldMigrateFromCryptor(oldCryptor, context));

        PreferenceHelper.migrateCryptorIfNeeded(context, oldCryptor);

        assertFalse(PreferenceHelper.shouldMigrateFromCryptor(oldCryptor, context));

        assertEquals(PreferenceHelper.findString(context, key1), value);
        assertEquals(PreferenceHelper.findString(context, key3), value);
        assertEquals(PreferenceHelper.findString(context, key4), value);
    }
}