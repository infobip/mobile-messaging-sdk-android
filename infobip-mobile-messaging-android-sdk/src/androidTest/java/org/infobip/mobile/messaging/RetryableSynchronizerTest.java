package org.infobip.mobile.messaging;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.infobip.mobile.messaging.mobile.data.SystemDataReporter;
import org.infobip.mobile.messaging.mobile.data.UserDataSynchronizer;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.mobile.messages.MessagesSynchronizer;
import org.infobip.mobile.messaging.mobile.registration.RegistrationSynchronizer;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.Helper;
import org.infobip.mobile.messaging.tools.InfobipAndroidTestCase;
import org.infobip.mobile.messaging.util.DeviceInformation;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.SystemInformation;
import org.mockito.Mockito;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author pandric on 08/03/2017.
 */

public class RetryableSynchronizerTest extends InfobipAndroidTestCase {

    private BroadcastReceiver errorReceiver;
    private Executor executor;

    private SystemDataReporter systemDataReporter;
    private GeoReporter geoReporter;
    private MessagesSynchronizer messagesSynchronizer;
    private RegistrationSynchronizer registrationSynchronizer;
    private UserDataSynchronizer userDataSynchronizer;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        MobileMessagingStats stats = mobileMessagingCore.getStats();

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

        debugServer.respondWith(NanoHTTPD.Response.Status.INTERNAL_ERROR, "{\n" +
                "  \"code\": \"500\",\n" +
                "  \"message\": \"Internal server error\"\n" +
                "}");
    }

    @Override
    protected void tearDown() throws Exception {
        context.unregisterReceiver(errorReceiver);

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

        // Given
        Helper.createReport(context, "signalingMessageId1", "campaignId1", "messageId1", true, Helper.createArea("areaId1"));
        Helper.createReport(context, "signalingMessageId2", "campaignId2", "messageId2", true, Helper.createArea("areaId2"));
        Helper.createReport(context, "signalingMessageId2", "campaignId2", "messageId3", true, Helper.createArea("areaId3"));

        // When
        geoReporter.synchronize();

        // Then
        Mockito.verify(errorReceiver, Mockito.after(4000).atLeast(4)).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    public void test_sync_messages_retry() {
        messagesSynchronizer.synchronize();
        Mockito.verify(errorReceiver, Mockito.after(4000).times(4)).onReceive(Mockito.any(Context.class), Mockito.any(Intent.class));
    }

    public void test_registration_retry() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED, false);

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
