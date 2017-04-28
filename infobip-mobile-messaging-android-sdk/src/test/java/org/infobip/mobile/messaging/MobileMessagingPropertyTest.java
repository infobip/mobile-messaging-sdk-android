package org.infobip.mobile.messaging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author sslavin
 * @since 15/12/2016.
 */

public class MobileMessagingPropertyTest {

    @Test
    public void test_registrationShouldBeEnabledByDefault() throws Exception {
        assertEquals("Push registration should be enabled by default",
                MobileMessagingProperty.PUSH_REGISTRATION_ENABLED.getDefaultValue(), true);
    }
}