package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.data.SystemDataReport;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
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

    private ArgumentCaptor<SystemData> captor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, true);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);

        captor = ArgumentCaptor.forClass(SystemData.class);
    }

    @Test
    public void test_reportSystemData() {

        mobileMessagingCore.reportSystemData();

        verify(broadcaster, after(1000).atLeastOnce()).systemDataReported(captor.capture());
        SystemData data = captor.getValue();
        // application version is null in test
        //assertFalse(data.getApplicationVersion().isEmpty());
        assertFalse(data.getDeviceManufacturer().isEmpty());
        assertFalse(data.getDeviceModel().isEmpty());
        assertFalse(data.getOsVersion().isEmpty());
        assertFalse(data.getSdkVersion().isEmpty());
        //assertFalse(data.isDeviceSecure());
    }

    @Test
    public void test_reportSystemData_withSettingDisabled() {

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, false);

        mobileMessagingCore.reportSystemData();

        verify(broadcaster, after(1000).atLeastOnce()).systemDataReported(captor.capture());
        SystemData data = captor.getValue();
        // application version is null in test
        //assertTrue(data.getApplicationVersion().isEmpty());
        assertTrue(data.getDeviceManufacturer().isEmpty());
        assertTrue(data.getDeviceModel().isEmpty());
        assertTrue(data.getOsVersion().isEmpty());
        assertFalse(data.getSdkVersion().isEmpty());
        //assertFalse(data.isDeviceSecure());
    }

    @Test
    public void test_reportSystemData_noDoubleReports() {

        mobileMessagingCore.reportSystemData();

        verify(broadcaster, after(1000).times(1)).systemDataReported(any(SystemData.class));

        mobileMessagingCore.reportSystemData();

        verify(broadcaster, after(1000).times(1)).systemDataReported(any(SystemData.class));
    }

    @Test
    public void test_reportSystemData_repeatAfterError() {

        doThrow(new RuntimeException()).when(mobileApiData).reportSystemData(any(SystemDataReport.class));

        mobileMessagingCore.reportSystemData();

        verify(broadcaster, after(1000).never()).systemDataReported(any(SystemData.class));
        verify(broadcaster, atLeastOnce()).error(any(MobileMessagingError.class));

        reset(mobileApiData);

        mobileMessagingCore.reportSystemData();

        verify(broadcaster, after(1000).atMost(1)).systemDataReported(any(SystemData.class));
    }

    @Test
    public void test_shouldReport_whenRegistrationIDAvailable() {

        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);

        mobileMessagingCore.reportSystemData();

        verify(broadcaster, after(1000).never()).systemDataReported(any(SystemData.class));

        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");

        mobileMessagingCore.reportSystemData();

        verify(broadcaster, after(1000).times(1)).systemDataReported(any(SystemData.class));
    }
}
