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
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.mobile.messages.MessagesSynchronizer;
import org.infobip.mobile.messaging.mobile.registration.RegistrationSynchronizer;
import org.infobip.mobile.messaging.mobile.seen.SeenStatusReporter;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.BroadcastReceiverMockito;
import org.infobip.mobile.messaging.tools.DebugServer;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fi.iki.elonen.NanoHTTPD;


public class PushUnregisteredTest extends InstrumentationTestCase {

    private Context context;
    private DebugServer debugServer;

    private GeoReporter geoReporter;
    private SeenStatusReporter seenStatusReporter;
    private RegistrationSynchronizer registrationSynchronizer;
    private MessagesSynchronizer messagesSynchronizer;

    private ArgumentCaptor<Intent> captor;

    private BroadcastReceiver geoEventsReceiver;
    private BroadcastReceiver pushRegistrationEnabledReceiver;
    private BroadcastReceiver seenStatusReceiver;
    private BroadcastReceiver messagesSynchronizerReceiver;
    private BroadcastReceiver errorReceiver;
    private MobileMessagingCore mobileMessagingCore;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getContext();
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        mobileMessagingCore = MobileMessagingCore.getInstance(context);
        MobileMessagingStats stats = mobileMessagingCore.getStats();

        MobileApiResourceProvider.INSTANCE.resetMobileApi();
        debugServer = new DebugServer();
        debugServer.start();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();

        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.GCM_REGISTRATION_ID, "TestRegistrationId");
        PreferenceHelper.saveLong(getInstrumentation().getContext(), MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);
        PreferenceHelper.saveBoolean(getInstrumentation().getContext(), MobileMessagingProperty.GEOFENCING_ACTIVATED, true);

        registrationSynchronizer = new RegistrationSynchronizer(context, stats, taskExecutor, mobileMessagingCore);
        seenStatusReporter = new SeenStatusReporter(context, stats, taskExecutor);
        geoReporter = new GeoReporter(context, stats);
        messagesSynchronizer = new MessagesSynchronizer(context, stats, taskExecutor);

        captor = ArgumentCaptor.forClass(Intent.class);
        geoEventsReceiver = BroadcastReceiverMockito.mock();
        pushRegistrationEnabledReceiver = BroadcastReceiverMockito.mock();
        seenStatusReceiver = BroadcastReceiverMockito.mock();
        messagesSynchronizerReceiver = BroadcastReceiverMockito.mock();
        errorReceiver = BroadcastReceiverMockito.mock();

        context.registerReceiver(geoEventsReceiver, new IntentFilter(Event.GEOFENCE_EVENTS_REPORTED.getKey()));
        context.registerReceiver(pushRegistrationEnabledReceiver, new IntentFilter(Event.PUSH_REGISTRATION_ENABLED.getKey()));
        context.registerReceiver(seenStatusReceiver, new IntentFilter(Event.SEEN_REPORTS_SENT.getKey()));
        context.registerReceiver(messagesSynchronizerReceiver, new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
        context.registerReceiver(errorReceiver, new IntentFilter(Event.API_COMMUNICATION_ERROR.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        context.unregisterReceiver(geoEventsReceiver);
        context.unregisterReceiver(pushRegistrationEnabledReceiver);
        context.unregisterReceiver(seenStatusReceiver);
        context.unregisterReceiver(messagesSynchronizerReceiver);
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


    public void test_push_registration_disabled() throws Exception {
        String response =
                "{\n" +
                        "  \"deviceApplicationInstanceId\": \"testDeviceApplicationInstanceId\",\n" +
                        "  \"pushRegistrationEnabled\": false\n" +
                        "}";

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, response);

        verifyRegistrationStatusUpdate(Mockito.after(4000).atLeastOnce(), false);
        Intent intent = captor.getValue();
        boolean isPushRegistrationEnabled = intent.getBooleanExtra(BroadcastParameter.EXTRA_PUSH_REGISTRATION_ENABLED, true);
        assertFalse(isPushRegistrationEnabled);
        verifySeenStatusReporter(Mockito.after(4000).atLeastOnce());


        // reports should NOT be called if push is disabled
        verifyGeoReporting(Mockito.after(4000).never());
        verifyMessagesSynchronizer(Mockito.after(4000).never());
    }

    public void test_push_registration_enabled() throws Exception {
        String response = "{\n" +
                "  \"deviceApplicationInstanceId\": \"testDeviceApplicationInstanceId\",\n" +
                "  \"pushRegistrationEnabled\": true" +
                "}";
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, response);

        verifyRegistrationStatusUpdate(Mockito.after(4000).atLeastOnce(), true);
        Intent intent = captor.getValue();
        boolean isPushRegistrationEnabled = intent.getBooleanExtra(BroadcastParameter.EXTRA_PUSH_REGISTRATION_ENABLED, false);
        assertTrue(isPushRegistrationEnabled);
        verifySeenStatusReporter(Mockito.after(4000).atLeastOnce());

        // reports should BE called if push is enabled
        verifyGeoReporting(Mockito.after(15000).atLeastOnce());
        verifyMessagesSynchronizer(Mockito.after(4000).atLeastOnce());
    }

    public void test_push_registration_default_status() throws Exception {
        String response = "{\n" +
                "  \"deviceApplicationInstanceId\": \"testDeviceApplicationInstanceId\",\n" +
                "  \"pushRegistrationEnabled\": true" +
                "}";
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, response);
        MobileMessaging.getInstance(context).disablePushRegistration(); // this method shall trigger pushRegistrationEnabledReceiver only once

        registrationSynchronizer.synchronize();
        BroadcastReceiverMockito.verify(pushRegistrationEnabledReceiver, Mockito.after(4000).times(1)).onReceive(Mockito.any(Context.class), captor.capture());
        assertTrue(MobileMessaging.getInstance(context).isPushRegistrationEnabled());
    }


    private void verifyMessagesSynchronizer(VerificationMode verificationMode) throws InterruptedException {
        mobileMessagingCore.addSyncMessagesIds("test-message-id");
        String response = "{\n" +
                "  \"payloads\": [\n" +
                "    {\n" +
                "      \"gcm.notification.messageId\": \"test-message-id\",\n" +
                "      \"gcm.notification.title\": \"this is title\",\n" +
                "      \"gcm.notification.body\": \"body\",\n" +
                "      \"gcm.notification.sound\": \"true\",\n" +
                "      \"gcm.notification.vibrate\": \"true\",\n" +
                "      \"gcm.notification.silent\": \"true\",\n" +
                "      \"gcm.notification.category\": \"UNKNOWN\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, response);
        messagesSynchronizer.synchronize();
        BroadcastReceiverMockito.verify(messagesSynchronizerReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifySeenStatusReporter(VerificationMode verificationMode) throws InterruptedException {
        String messageIds[] = {"1"};
        mobileMessagingCore.addUnreportedSeenMessageIds(messageIds);
        seenStatusReporter.synchronize();
        BroadcastReceiverMockito.verify(seenStatusReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifyGeoReporting(VerificationMode verificationMode) throws InterruptedException {
        List<GeoReport> reports = new ArrayList<>();
        reports.add(new GeoReport("campaignId1", "messageId1", GeoEventType.entry, new Area("areaId1", "Area1", 1.0, 1.0, 3), 1001L));
        reports.add(new GeoReport("campaignId2", "messageId2", GeoEventType.exit, new Area("areaId2", "Area2", 2.0, 2.0, 4), 1002L));
        reports.add(new GeoReport("campaignId3", "messageId3", GeoEventType.dwell, new Area("areaId3", "Area3", 3.0, 3.0, 5), 1003L));

        mobileMessagingCore.addUnreportedGeoEvents(reports);
        geoReporter.synchronize();
        BroadcastReceiverMockito.verify(geoEventsReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifyRegistrationStatusUpdate(VerificationMode verificationMode, boolean enable) throws InterruptedException {
        registrationSynchronizer.updatePushRegistrationStatus(enable);
        BroadcastReceiverMockito.verify(pushRegistrationEnabledReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }
}
