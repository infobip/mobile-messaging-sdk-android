package org.infobip.mobile.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.infobip.mobile.messaging.tools.DebugServer;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author sslavin
 * @since 25/08/16.
 */
public class SystemDataReportTest extends InstrumentationTestCase {

    DebugServer debugServer;
    BroadcastReceiver receiver;
    BroadcastReceiver errorReceiver;
    ArgumentCaptor<Intent> captor;
    MobileMessagingCore mobileMessagingCore;
    Context context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = getInstrumentation().getContext();

        debugServer = new DebugServer();
        debugServer.start();

        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, true);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);

        MobileApiResourceProvider.INSTANCE.resetMobileApi();

        mobileMessagingCore = MobileMessagingCore.getInstance(context);

        captor = ArgumentCaptor.forClass(Intent.class);
        receiver = Mockito.mock(BroadcastReceiver.class);
        errorReceiver = Mockito.mock(BroadcastReceiver.class);
        context.registerReceiver(receiver, new IntentFilter(Event.SYSTEM_DATA_REPORTED.getKey()));
        context.registerReceiver(errorReceiver, new IntentFilter(Event.API_COMMUNICATION_ERROR.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        context.unregisterReceiver(receiver);

        if (null != debugServer) {
            try {
                debugServer.stop();
            } catch (Exception e) {
                //ignore
            }
        }

        super.tearDown();
    }

    public void test_reportSystemData() {

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileMessagingCore.readSystemData();
        mobileMessagingCore.sync();

        Mockito.verify(receiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
        assertTrue(captor.getValue().hasExtra(BroadcastParameter.EXTRA_SYSTEM_DATA));

        SystemData data = new JsonSerializer().deserialize(captor.getValue().getStringExtra(BroadcastParameter.EXTRA_SYSTEM_DATA), SystemData.class);
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

        mobileMessagingCore.readSystemData();
        mobileMessagingCore.sync();

        Mockito.verify(receiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), captor.capture());
        assertTrue(captor.getValue().hasExtra(BroadcastParameter.EXTRA_SYSTEM_DATA));

        SystemData data = new JsonSerializer().deserialize(captor.getValue().getStringExtra(BroadcastParameter.EXTRA_SYSTEM_DATA), SystemData.class);
        // application version is null in test
        //assertTrue(data.getApplicationVersion().isEmpty());
        assertTrue(data.getDeviceManufacturer().isEmpty());
        assertTrue(data.getDeviceModel().isEmpty());
        assertTrue(data.getOsVersion().isEmpty());
        assertFalse(data.getSdkVersion().isEmpty());
    }

    public void test_reportSystemData_noDoubleReports() {

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileMessagingCore.readSystemData();
        mobileMessagingCore.sync();

        Mockito.verify(receiver, Mockito.after(1000).atMost(1)).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));

        mobileMessagingCore.readSystemData();
        mobileMessagingCore.sync();

        Mockito.verify(receiver, Mockito.after(1000).atMost(1)).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    public void test_reportSystemData_repeatAfterError() {

        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, null);

        mobileMessagingCore.readSystemData();
        mobileMessagingCore.sync();

        Mockito.verify(receiver, Mockito.after(1000).never()).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
        Mockito.verify(errorReceiver, Mockito.after(1000).atLeastOnce()).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, null);

        mobileMessagingCore.sync();

        Mockito.verify(receiver, Mockito.after(1000).atMost(1)).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }
}
