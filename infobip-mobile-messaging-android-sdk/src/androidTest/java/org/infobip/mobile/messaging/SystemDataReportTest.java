package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemDataReportTest extends MobileMessagingTestCase {

    private ArgumentCaptor<SystemData> captor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, true);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);

        captor = ArgumentCaptor.forClass(SystemData.class);
    }

    public void test_reportSystemData() {

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileMessagingCore.reportSystemData();

        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).systemDataReported(captor.capture());
        SystemData data = captor.getValue();
        // application version is null in test
        //assertFalse(data.getApplicationVersion().isEmpty());
        assertFalse(data.getDeviceManufacturer().isEmpty());
        assertFalse(data.getDeviceModel().isEmpty());
        assertFalse(data.getOsVersion().isEmpty());
        assertFalse(data.getSdkVersion().isEmpty());
    }

    public void test_reportSystemData_withSettingDisabled() {

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, false);

        mobileMessagingCore.reportSystemData();

        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).systemDataReported(captor.capture());
        SystemData data = captor.getValue();
        // application version is null in test
        //assertTrue(data.getApplicationVersion().isEmpty());
        assertTrue(data.getDeviceManufacturer().isEmpty());
        assertTrue(data.getDeviceModel().isEmpty());
        assertTrue(data.getOsVersion().isEmpty());
        assertFalse(data.getSdkVersion().isEmpty());
    }

    public void test_reportSystemData_noDoubleReports() {

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileMessagingCore.reportSystemData();

        Mockito.verify(broadcaster, Mockito.after(1000).atMost(1)).systemDataReported(Mockito.any(SystemData.class));

        mobileMessagingCore.reportSystemData();

        Mockito.verify(broadcaster, Mockito.after(1000).atMost(1)).systemDataReported(Mockito.any(SystemData.class));
    }

    public void test_reportSystemData_repeatAfterError() {

        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, null);

        mobileMessagingCore.reportSystemData();

        Mockito.verify(broadcaster, Mockito.after(1000).never()).systemDataReported(Mockito.any(SystemData.class));
        Mockito.verify(broadcaster, Mockito.after(1000).atLeastOnce()).error(Mockito.any(MobileMessagingError.class));

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileMessagingCore.reportSystemData();

        Mockito.verify(broadcaster, Mockito.after(1000).atMost(1)).systemDataReported(Mockito.any(SystemData.class));
    }

    public void test_shouldReport_whenRegistrationIDAvailable() {

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);

        mobileMessagingCore.reportSystemData();

        Mockito.verify(broadcaster, Mockito.after(1000).never()).systemDataReported(Mockito.any(SystemData.class));
        Mockito.verify(broadcaster, Mockito.after(1000).never()).error(Mockito.any(MobileMessagingError.class));

        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");

        mobileMessagingCore.reportSystemData();

        Mockito.verify(broadcaster, Mockito.after(1000).times(1)).systemDataReported(Mockito.any(SystemData.class));
    }
}
