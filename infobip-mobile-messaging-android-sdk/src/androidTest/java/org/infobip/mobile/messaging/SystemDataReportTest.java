/*
 * SystemDataReportTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;

import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemDataReportTest extends MobileMessagingTestCase {

    private ArgumentCaptor<Installation> captor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, true);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);

        captor = ArgumentCaptor.forClass(Installation.class);
    }

    @Test
    public void test_reportSystemData() {

        mobileMessagingCore.syncInstallation();

        verify(broadcaster, after(1000).atLeastOnce()).installationUpdated(captor.capture());
        Installation data = captor.getValue();
        // application version is null in test
//        assertFalse(data.getAppVersion().isEmpty());
        assertFalse(data.getDeviceManufacturer().isEmpty());
        assertFalse(data.getDeviceModel().isEmpty());
        assertFalse(data.getOsVersion().isEmpty());
        assertFalse(data.getSdkVersion().isEmpty());
        assertNotEquals("null", data.getSdkVersion());
//        assertFalse(data.getDeviceSecure());
    }

    @Test
    public void test_reportSystemData_withSettingDisabled() {

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, false);

        mobileMessagingCore.syncInstallation();

        verify(broadcaster, after(1000).atLeastOnce()).installationUpdated(captor.capture());
        Installation data = captor.getValue();
        // application version is null in test
        //assertTrue(data.getApplicationVersion().isEmpty());
        assertTrue(data.getDeviceManufacturer().isEmpty());
        assertTrue(data.getDeviceModel().isEmpty());
        assertTrue(data.getOsVersion().isEmpty());
        assertFalse(data.getSdkVersion().isEmpty());
        assertFalse(data.getDeviceSecure());
    }

    @Test
    public void test_reportSystemData_noDoubleReports() {

        mobileMessagingCore.syncInstallation();

        verify(broadcaster, after(1000).times(1)).installationUpdated(any(Installation.class));

        mobileMessagingCore.syncInstallation();

        verify(broadcaster, after(1000).times(1)).installationUpdated(any(Installation.class));
    }

    @Test
    public void test_reportSystemData_repeatAfterError() {

        doThrow(new RuntimeException()).when(mobileApiAppInstance).patchInstance(anyString(), any(Map.class));

        mobileMessagingCore.syncInstallation();

        verify(broadcaster, after(1000).never()).installationUpdated(any(Installation.class));
        verify(broadcaster, atLeastOnce()).error(any(MobileMessagingError.class));

        reset(mobileApiAppInstance);

        mobileMessagingCore.syncInstallation();

        verify(broadcaster, after(1000).atMost(1)).installationUpdated(any(Installation.class));
    }

    @Test
    public void test_shouldReport_whenRegistrationIDAvailable() {

        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);
        PreferenceHelper.remove(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED);
        AppInstance appInstance = new AppInstance("pushRegId");
        BDDMockito.given(mobileApiAppInstance.createInstance(any(AppInstance.class))).willReturn(appInstance);

        mobileMessagingCore.syncInstallation();

        verify(broadcaster, after(1000).never()).installationUpdated(any(Installation.class));
        verify(broadcaster, after(1000).times(1)).registrationCreated(anyString(), anyString());

        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");

        mobileMessagingCore.syncInstallation();

        verify(broadcaster, after(1000).never()).installationUpdated(any(Installation.class));
    }
}
