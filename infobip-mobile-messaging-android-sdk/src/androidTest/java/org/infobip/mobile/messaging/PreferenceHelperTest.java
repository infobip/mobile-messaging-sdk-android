package org.infobip.mobile.messaging;

import android.content.Context;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.util.PreferenceHelper;

/**
 * @author sslavin
 * @since 29/08/16.
 */
public class PreferenceHelperTest extends InstrumentationTestCase {

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

    public void test_encryptedPreferencesTest() throws Exception {
        String key = "thisIsMyUnencryptedKey";
        String value = "thisIsMyUnencryptedData";

        PreferenceHelper.saveString(context, key, value, true);
        String foundValue = PreferenceHelper.findString(context, key, null, true);
        String foundUnencryptedValue = PreferenceHelper.findString(context, key, null);

        assertFalse(value.equals(foundUnencryptedValue));
        assertEquals(value, foundValue);
    }

    public void test_shouldFindAndRemoveDeviceInstanceIdAsEncryptedProperty() throws Exception {
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "StubStringValue");

        assertEquals(true, PreferenceHelper.contains(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID));
        assertEquals("StubStringValue", PreferenceHelper.findString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID));

        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);

        assertEquals(false, PreferenceHelper.contains(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID));
        assertEquals(null, PreferenceHelper.findString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID));
    }
}
