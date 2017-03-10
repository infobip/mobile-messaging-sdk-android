package org.infobip.mobile.messaging;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.geo.Area;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.data.SystemDataReporter;
import org.infobip.mobile.messaging.mobile.data.UserDataSynchronizer;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.mobile.messages.MessagesSynchronizer;
import org.infobip.mobile.messaging.mobile.registration.RegistrationSynchronizer;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.DebugServer;
import org.infobip.mobile.messaging.util.DeviceInformation;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.SystemInformation;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author pandric on 08/03/2017.
 */

public class RetryableSynchronizerTest extends InstrumentationTestCase {

    private DebugServer debugServer;
    private BroadcastReceiver errorReceiver;
    private MobileMessagingCore mobileMessagingCore;
    private Context context;
    private Executor executor;

    private SystemDataReporter systemDataReporter;
    private GeoReporter geoReporter;
    private MessagesSynchronizer messagesSynchronizer;
    private RegistrationSynchronizer registrationSynchronizer;
    private UserDataSynchronizer userDataSynchronizer;

    private String errorResponse = "{\n" +
            "  \"code\": \"500\",\n" +
            "  \"message\": \"Internal server error\"\n" +
            "}";


    @SuppressLint("CommitPrefEdits")
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        MobileApiResourceProvider.INSTANCE.resetMobileApi();
        context = getInstrumentation().getContext();
        mobileMessagingCore = MobileMessagingCore.getInstance(context);
        MobileMessagingStats stats = mobileMessagingCore.getStats();

        debugServer = new DebugServer();
        debugServer.start();
        debugServer.respondWith(NanoHTTPD.Response.Status.INTERNAL_ERROR, errorResponse);

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();

        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.GCM_REGISTRATION_ID, "TestRegistrationId");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, true);
        PreferenceHelper.saveInt(context, MobileMessagingProperty.DEFAULT_EXP_BACKOFF_MULTIPLIER, 0);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);

        errorReceiver = Mockito.mock(BroadcastReceiver.class);
        context.registerReceiver(errorReceiver, new IntentFilter(Event.API_COMMUNICATION_ERROR.getKey()));

        executor = Executors.newSingleThreadExecutor();
        geoReporter = new GeoReporter(context, stats);
        systemDataReporter = new SystemDataReporter(context, stats, executor);
        messagesSynchronizer = new MessagesSynchronizer(context, stats, executor);
        registrationSynchronizer = new RegistrationSynchronizer(context, stats, executor, mobileMessagingCore);
        userDataSynchronizer = new UserDataSynchronizer(context, stats, executor);
    }

    @Override
    protected void tearDown() throws Exception {
        context.unregisterReceiver(errorReceiver);

        if (null != debugServer) {
            try {
                debugServer.stop();
            } catch (Exception e) {
                //ignore
            }
        }
        super.tearDown();
    }

    public void test_system_data_retry() {
        reportSystemData();
        Mockito.verify(errorReceiver, Mockito.after(4000).atLeast(3)).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    private void reportSystemData() {
        boolean reportEnabled = PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO);

        SystemData data = new SystemData(SoftwareInformation.getLibraryVersion(),
                reportEnabled ? SystemInformation.getAndroidSystemVersion() : "",
                reportEnabled ? DeviceInformation.getDeviceManufacturer() : "",
                reportEnabled ? DeviceInformation.getDeviceModel() : "",
                reportEnabled ? SoftwareInformation.getAppVersion(context) : "",
                MobileMessagingCore.isGeofencingActivated(context),
                SoftwareInformation.areNotificationsEnabled(context));

        Integer hash = PreferenceHelper.findInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);
        if (hash != data.hashCode()) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA, data.toString());
        }
        systemDataReporter.synchronize();
    }

    public void test_geo_report_retry() {
        List<GeoReport> reports = new ArrayList<>();
        reports.add(new GeoReport("campaignId1", "messageId1", GeoEventType.entry, new Area("areaId1", "Area1", 1.0, 1.0, 3), 1001L));
        reports.add(new GeoReport("campaignId2", "messageId2", GeoEventType.exit, new Area("areaId2", "Area2", 2.0, 2.0, 4), 1002L));
        reports.add(new GeoReport("campaignId3", "messageId3", GeoEventType.dwell, new Area("areaId3", "Area3", 3.0, 3.0, 5), 1003L));
        mobileMessagingCore.addUnreportedGeoEvents(reports);

        geoReporter.synchronize();
        Mockito.verify(errorReceiver, Mockito.after(4000).atLeast(4)).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    public void test_sync_messages_retry() {
        messagesSynchronizer.synchronize();
        Mockito.verify(errorReceiver, Mockito.after(4000).times(4)).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    public void test_registration_retry() {
        registrationSynchronizer.synchronize();
        Mockito.verify(errorReceiver, Mockito.after(4000).times(4)).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    public void test_user_data_retry() {
        UserData userData = new UserData();
        userData.setFirstName("Retry");
        userData.setLastName("Everything");
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_USER_DATA, userData.toString());

        userDataSynchronizer.synchronize(null);
        Mockito.verify(errorReceiver, Mockito.after(4000).times(4)).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }
}
