package org.infobip.mobile.messaging.util;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * @author sslavin
 * @since 29/08/16.
 */
public class PreferenceHelperTest extends MobileMessagingTestCase {

    @Test
    public void test_privateSharedPrefsNotDeletedOnClearingOfPublicAppPrefs() throws Exception {
        MobileMessagingProperty key1 = MobileMessagingProperty.APP_USER_ID;
        MobileMessagingProperty key2 = MobileMessagingProperty.USER_DATA;
        MobileMessagingProperty key3 = MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES;
        String value = "some value";

        // saving to public storage
        PreferenceHelper.saveString(context, key1, value);
        PreferenceHelper.saveString(context, key2, value);
        // saving to private storage
        PreferenceHelper.saveUsePrivateSharedPrefs(context, true);
        PreferenceHelper.saveString(context, key3, value);

        assertTrue(PreferenceHelper.publicPrefsContains(context, key1));
        assertEquals(value, PreferenceHelper.getPublicSharedPreferences(context).getString(key1.getKey(), null));
        assertTrue(PreferenceHelper.publicPrefsContains(context, key2));
        assertEquals(value, PreferenceHelper.getPublicSharedPreferences(context).getString(key2.getKey(), null));
        assertTrue(PreferenceHelper.contains(context, key3));
        assertEquals(value, PreferenceHelper.findString(context, key3));

        PreferenceHelper.getPublicSharedPreferences(context).edit().clear().apply();

        assertFalse(PreferenceHelper.getPublicSharedPreferences(context).contains(key1.getKey()));
        assertFalse(PreferenceHelper.getPublicSharedPreferences(context).contains(key2.getKey()));
        assertFalse(PreferenceHelper.getPublicSharedPreferences(context).contains(key3.getKey()));
        assertTrue(PreferenceHelper.getPrivateMMSharedPreferences(context).contains(key3.getKey()));
        assertEquals(value, PreferenceHelper.getPrivateMMSharedPreferences(context).getString(key3.getKey(), null));
    }

    @Test
    public void test_encryptedPreferencesTest() throws Exception {
        String key = "thisIsMyUnencryptedKey";
        String value = "thisIsMyUnencryptedData";

        PreferenceHelper.saveString(context, key, value, true);
        String foundValue = PreferenceHelper.findString(context, key, null, true);
        String foundUnencryptedValue = PreferenceHelper.findString(context, key, null);

        assertFalse(value.equals(foundUnencryptedValue));
        assertEquals(value, foundValue);
    }

    @Test
    public void test_shouldFindAndRemoveDeviceInstanceIdAsEncryptedProperty() throws Exception {
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "StubStringValue");

        assertEquals(true, PreferenceHelper.contains(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID));
        assertEquals("StubStringValue", PreferenceHelper.findString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID));

        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);

        assertEquals(false, PreferenceHelper.contains(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID));
        assertEquals(null, PreferenceHelper.findString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID));
    }
}
