/*
 * InstallationReconciliationTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import android.annotation.SuppressLint;

import org.infobip.mobile.messaging.android.MobileMessagingBaseTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class InstallationReconciliationTest extends MobileMessagingBaseTestCase {

    @SuppressLint("ApplySharedPref")
    @Before
    public void setUp() throws Exception {
        super.setUp();
        PreferenceHelper.getPublicSharedPreferences(context).edit().clear().commit();
        PreferenceHelper.getPrivateMMSharedPreferences(context).edit().clear().commit();
        PreferenceHelper.saveUsePrivateSharedPrefs(context, true);
    }

    @Test
    public void test_validRegistration_noReset() {
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "RegId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.CLOUD_TOKEN, "Token");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, true);
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID, "UUID");
        PreferenceHelper.saveInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH, 12345);

        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED));
        assertEquals("UUID", PreferenceHelper.findString(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID));
        assertEquals(12345, PreferenceHelper.findInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH));
    }

    @Test
    public void test_lostRegistration_resetsDependentState() {
        // No INFOBIP_REGISTRATION_ID or CLOUD_TOKEN — simulates post-restore after self-healing
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, true);
        PreferenceHelper.saveString(context, MobileMessagingProperty.REPORTED_PUSH_SERVICE_TYPE, "FCM");
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID, "OldUUID");
        PreferenceHelper.saveString(context, MobileMessagingProperty.MOBILE_CARRIER_NAME, "Carrier");
        PreferenceHelper.saveInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH, 12345);

        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED));
        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.REPORTED_PUSH_SERVICE_TYPE));
        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID));
        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.MOBILE_CARRIER_NAME));
        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH));
    }

    @Test
    public void test_lostRegistration_preservesUserState() {
        // No registration, but user state should survive
        PreferenceHelper.saveString(context, MobileMessagingProperty.APP_USER_ID, "user123");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, true);
        PreferenceHelper.saveString(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES, "{\"key\":\"val\"}");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED, false);

        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertEquals("user123", PreferenceHelper.findString(context, MobileMessagingProperty.APP_USER_ID));
        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED));
        assertEquals("{\"key\":\"val\"}", PreferenceHelper.findString(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES));
        assertFalse(PreferenceHelper.findBoolean(context, MobileMessagingProperty.DISPLAY_NOTIFICATION_ENABLED));
    }

    @Test
    public void test_lostRegistration_reflagsAppUserId() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, true);
        PreferenceHelper.saveString(context, MobileMessagingProperty.APP_USER_ID, "user123");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED, false);

        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED));
    }

    @Test
    public void test_lostRegistration_reflagsPushRegistrationEnabled() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, true);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, true);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.UNREPORTED_PUSH_REGISTRATION_ENABLED, false);

        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.UNREPORTED_PUSH_REGISTRATION_ENABLED));
    }

    @Test
    public void test_lostRegistration_reflagsPrimaryDevice() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, true);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_PRIMARY, true);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED, false);

        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertTrue(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED));
    }

    @Test
    public void test_lostRegistration_reflagsPrimaryDeviceFalse() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, true);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.IS_PRIMARY, false);

        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertTrue(PreferenceHelper.contains(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED));
        assertFalse(PreferenceHelper.findBoolean(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED));
    }

    @Test
    public void test_lostRegistration_reflagsCustomAttributes() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, true);
        PreferenceHelper.saveString(context, MobileMessagingProperty.CUSTOM_ATTRIBUTES, "{\"key\":\"val\"}");

        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertEquals("{\"key\":\"val\"}", PreferenceHelper.findString(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES));
    }

    @Test
    public void test_lostRegistration_doesNotReflagAbsentState() {
        // No APP_USER_ID, no push registration, no primary, no custom attributes — nothing to reflag
        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.IS_APP_USER_ID_UNREPORTED));
        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.UNREPORTED_PUSH_REGISTRATION_ENABLED));
        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.IS_PRIMARY_UNREPORTED));
        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.UNREPORTED_CUSTOM_ATTRIBUTES));
    }

    @Test
    public void test_freshInstall_noOp() {
        // No prefs at all — reconciliation is a harmless no-op
        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED));
        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID));
    }

    @Test
    public void test_hasUiiButNoTokenOrRegId_triggersReconciliation() {
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID, "OldUUID");

        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertFalse(PreferenceHelper.contains(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID));
    }

    @Test
    public void test_hasCloudTokenButNoRegId_noReset() {
        // Token obtained from Firebase but not yet registered with backend
        PreferenceHelper.saveString(context, MobileMessagingProperty.CLOUD_TOKEN, "FcmToken");
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID, "UUID");

        MobileMessagingCore.reconcileLostInstallationIdentity(context);

        assertEquals("UUID", PreferenceHelper.findString(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID));
    }
}
