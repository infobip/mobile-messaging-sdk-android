package org.infobip.mobile.messaging;

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
    private ExecutorService taskExecutor;

    private BroadcastReceiver geoEventsReceiver;
    private BroadcastReceiver pusgRegistrationEnabledReceiver;
    private BroadcastReceiver seenStatusReceiver;
    private BroadcastReceiver messagesSynchronizerReceiver;
    private BroadcastReceiver errorReceiver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = getInstrumentation().getContext();

        registrationSynchronizer = new RegistrationSynchronizer();
        seenStatusReporter = new SeenStatusReporter();
        geoReporter = new GeoReporter();
        messagesSynchronizer = new MessagesSynchronizer();

        taskExecutor = Executors.newSingleThreadExecutor();
        debugServer = new DebugServer();
        debugServer.start();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();

        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(getInstrumentation().getContext(), MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveLong(getInstrumentation().getContext(), MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);
        PreferenceHelper.saveBoolean(getInstrumentation().getContext(), MobileMessagingProperty.GEOFENCING_ACTIVATED, true);

        MobileApiResourceProvider.INSTANCE.resetMobileApi();

        captor = ArgumentCaptor.forClass(Intent.class);
        geoEventsReceiver = Mockito.mock(BroadcastReceiver.class);
        pusgRegistrationEnabledReceiver = Mockito.mock(BroadcastReceiver.class);
        seenStatusReceiver = Mockito.mock(BroadcastReceiver.class);
        messagesSynchronizerReceiver = Mockito.mock(BroadcastReceiver.class);
        errorReceiver = Mockito.mock(BroadcastReceiver.class);
        context.registerReceiver(geoEventsReceiver, new IntentFilter(Event.GEOFENCE_EVENTS_REPORTED.getKey()));
        context.registerReceiver(pusgRegistrationEnabledReceiver, new IntentFilter(Event.PUSH_REGISTRATION_ENABLED.getKey()));
        context.registerReceiver(seenStatusReceiver, new IntentFilter(Event.SEEN_REPORTS_SENT.getKey()));
        context.registerReceiver(messagesSynchronizerReceiver, new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
        context.registerReceiver(errorReceiver, new IntentFilter(Event.API_COMMUNICATION_ERROR.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        context.unregisterReceiver(geoEventsReceiver);
        context.unregisterReceiver(pusgRegistrationEnabledReceiver);
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


    public void test_push_registration_disabled() {
        String response = "{\n" +
                "  \"deviceApplicationInstanceId\": \"testDeviceApplicationInstanceId\",\n" +
                "  \"pushRegistrationEnabled\": false\n" +
                "}";

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, response);

        verifyRegistration(Mockito.after(1000).atLeastOnce());
        Intent intent = captor.getValue();
        boolean isPushRegistrationEnabled = intent.getBooleanExtra(BroadcastParameter.EXTRA_PUSH_REGISTRATION_ENABLED, true);
        assertFalse(isPushRegistrationEnabled);

        // reports should NOT be called if push is disabled
        VerificationMode never = Mockito.after(1000).never();
        verifyGeoReporting(never);
        verifySeenStatusReporter(never);
        verifyMessagesSynchronizer(never);
    }

    public void test_push_registration_enabled() {
        String response = "{\n" +
                "  \"deviceApplicationInstanceId\": \"testDeviceApplicationInstanceId\",\n" +
                "  \"pushRegistrationEnabled\": true\n" +
                "}";
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, response);

        verifyRegistration(Mockito.after(1000).atLeastOnce());
        Intent intent = captor.getValue();
        boolean isPushRegistrationEnabled = intent.getBooleanExtra(BroadcastParameter.EXTRA_PUSH_REGISTRATION_ENABLED, false);
        assertTrue(isPushRegistrationEnabled);

        // reports should BE called if push is enabled
        VerificationMode atLeastOnce = Mockito.after(1000).atLeastOnce();
        verifyGeoReporting(atLeastOnce);
        verifySeenStatusReporter(atLeastOnce);
        verifyMessagesSynchronizer(atLeastOnce);
    }

    public void test_push_registration_status() {
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, "{}");
        VerificationMode never = Mockito.after(1000).never();
        MobileMessaging.getInstance(context).enablePushRegistration();

        verifyRegistration(never);
        assertFalse(MobileMessaging.getInstance(context).isPushRegistrationEnabled());
    }


    private void verifyMessagesSynchronizer(VerificationMode verificationMode) {
        MobileMessagingCore.getInstance(context).addSyncMessagesIds("test-message-id");
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
        messagesSynchronizer.synchronize(context, MobileMessagingCore.getInstance(context).getStats(), taskExecutor);
        Mockito.verify(messagesSynchronizerReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifySeenStatusReporter(VerificationMode verificationMode) {
        String messageIds[] = {"1"};
        seenStatusReporter.report(context, messageIds, new MobileMessagingStats(getInstrumentation().getContext()), taskExecutor);
        Mockito.verify(seenStatusReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifyGeoReporting(VerificationMode verificationMode) {
        List<GeoReport> reports = new ArrayList<>();
        reports.add(new GeoReport("campaignId1", "messageId1", GeoEventType.entry, new Area("areaId1", "Area1", 1.0, 1.0, 3), 1001L));
        reports.add(new GeoReport("campaignId2", "messageId2", GeoEventType.exit, new Area("areaId2", "Area2", 2.0, 2.0, 4), 1002L));
        reports.add(new GeoReport("campaignId3", "messageId3", GeoEventType.dwell, new Area("areaId3", "Area3", 3.0, 3.0, 5), 1003L));

        MobileMessagingCore.getInstance(context).addUnreportedGeoEvents(reports);
        geoReporter.report(context, MobileMessagingCore.getInstance(context).getStats());
        Mockito.verify(geoEventsReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifyRegistration(VerificationMode verificationMode) {
        registrationSynchronizer.updatePushRegistrationStatus(context, "TestDeviceInstanceId", null, MobileMessagingCore.getInstance(context).getStats(), taskExecutor);
        Mockito.verify(pusgRegistrationEnabledReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }
}
