/*
 * PrivacySettingsTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.storage;

import org.infobip.mobile.messaging.CustomAttributeValue;
import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserAttributes;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

public class PrivacySettingsTest extends MobileMessagingTestCase {

    @Override
    public void setUp() throws Exception {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.SAVE_USER_DATA_ON_DISK, false);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.SAVE_CUSTOM_ATTRIBUTES_ON_DISK, false);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.SAVE_INSTALLATION_ON_DISK, false);
        super.setUp();
    }

    @Test
    @Ignore
    public void test_delete_user_data() {
        User givenUser = userData();

        mobileMessagingCore.saveUser(givenUser);

        User savedUser = mobileMessagingCore.getUser();
        assertNotNull("User data should exist before app initialization", savedUser);
    }

    @Test
    @Ignore
    public void test_delete_installation_data() {
        Installation givenInstallation = getInstallations().get(0);

        mobileMessagingCore.saveInstallation(givenInstallation);
        mobileMessagingCore.saveCustomAttributes(getCustomAttributes());

        Installation savedInstallation = mobileMessagingCore.getInstallation();
        String customAttributes = mobileMessagingCore.getCustomAttributes();

        assertNotNull("Installation data should exist before app initialization", savedInstallation);
        assertNotNull("Custom attributes data should exist before app initialization", customAttributes);
    }

    @Test
    @Ignore
    public void test_delete_application_code() {

        String retreivedApplicationCode = mobileMessagingCore.getApplicationCode();

        assertNotNull("Application code exists", retreivedApplicationCode);

//        mobileMessagingCore.removeApplicationCodeFromLocalStorage();

        String deletedApplicationCode = PreferenceHelper.findString(context, MobileMessagingProperty.APPLICATION_CODE);
        assertNull("Application code should be deleted", deletedApplicationCode);
    }

    @NonNull
    private User userData() {

        Map<String, CustomAttributeValue> customAttributes = getCustomAttributes();

        Set<String> phones = new HashSet<>(Collections.singletonList("+123456789"));
        Set<String> emails = new HashSet<>(Collections.singletonList("user@example.com"));
        Set<String> tags = new HashSet<>(Arrays.asList("VIP", "BetaTester"));

        return new User(
                "user123",   // externalUserId
                "John",                   // firstName
                "Doe",                    // lastName
                null,                     // middleName
                UserAttributes.Gender.Male, // gender
                "1990-01-01",             // birthday
                User.Type.CUSTOMER,       // type
                phones,                   // phones
                emails,                   // emails
                tags,                     // tags
                getInstallations(),       // installations
                customAttributes          // customAttributes
        );
    }

    @NonNull
    private static Map<String, CustomAttributeValue> getCustomAttributes() {
        Map<String, CustomAttributeValue> customAttributes = new HashMap<>();
        customAttributes.put("nickname", new CustomAttributeValue("JohnDoe"));
        customAttributes.put("age", new CustomAttributeValue("30"));
        customAttributes.put("isPremiumUser", new CustomAttributeValue("true"));
        return customAttributes;
    }

    @NonNull
    private static List<Installation> getInstallations() {
        List<Installation> installations = new ArrayList<>();
        Installation installation = new Installation();
        installation.setPrimaryDevice(true);
        installation.setPushRegistrationEnabled(true);
        installation.setNotificationsEnabled(true);
        installation.setSdkVersion("1.0.0");
        installation.setAppVersion("2.3.4");
        installation.setOs("Android");
        installation.setOsVersion("12.0");
        installation.setDeviceManufacturer("Google");
        installation.setDeviceModel("Pixel 6");
        installation.setDeviceSecure(true);
        installation.setLanguage("en");
        installation.setDeviceTimezoneOffset("+02:00");
        installation.setApplicationUserId("user123");
        installation.setDeviceName("John's Phone");

        installations.add(installation);
        return installations;
    }
}
