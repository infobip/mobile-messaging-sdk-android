package org.infobip.mobile.messaging.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.test.InstrumentationTestCase;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
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
        geoEventsReceiver = BroadcastReceiverMockito.mock();
        pusgRegistrationEnabledReceiver = BroadcastReceiverMockito.mock();
        seenStatusReceiver = BroadcastReceiverMockito.mock();
        messagesSynchronizerReceiver = BroadcastReceiverMockito.mock();
        errorReceiver = BroadcastReceiverMockito.mock();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(geoEventsReceiver, new IntentFilter(Event.GEOFENCE_EVENTS_REPORTED.getKey()));
        localBroadcastManager.registerReceiver(pusgRegistrationEnabledReceiver, new IntentFilter(Event.PUSH_REGISTRATION_ENABLED.getKey()));
        localBroadcastManager.registerReceiver(seenStatusReceiver, new IntentFilter(Event.SEEN_REPORTS_SENT.getKey()));
        localBroadcastManager.registerReceiver(messagesSynchronizerReceiver, new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
        localBroadcastManager.registerReceiver(errorReceiver, new IntentFilter(Event.API_COMMUNICATION_ERROR.getKey()));
    }

    @Override
    protected void tearDown() throws Exception {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.unregisterReceiver(geoEventsReceiver);
        localBroadcastManager.unregisterReceiver(pusgRegistrationEnabledReceiver);
        localBroadcastManager.unregisterReceiver(seenStatusReceiver);
        localBroadcastManager.unregisterReceiver(messagesSynchronizerReceiver);
        localBroadcastManager.unregisterReceiver(errorReceiver);

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
        String response = "{\n" +
                "  \"deviceApplicationInstanceId\": \"testDeviceApplicationInstanceId\",\n" +
                "  \"pushRegistrationEnabled\": false\n" +
                "}";

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, response);

        verifyRegistration(Mockito.after(1000).atLeastOnce());
        Intent intent = captor.getValue();
        boolean isPushRegistrationEnabled = intent.getBooleanExtra(BroadcastParameter.EXTRA_PUSH_REGISTRATION_ENABLED, true);
        assertFalse(isPushRegistrationEnabled);
        verifySeenStatusReporter(Mockito.after(1000).atLeastOnce());

        // reports should NOT be called if push is disabled
        VerificationMode never = Mockito.after(1000).never();
        verifyGeoReporting(never);
        verifyMessagesSynchronizer(never);
    }

    public void test_push_registration_enabled() throws Exception {
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

    public void test_push_registration_default_status() throws Exception {
        debugServer.respondWith(NanoHTTPD.Response.Status.BAD_REQUEST, "{}");
        VerificationMode never = Mockito.after(1000).never();
        MobileMessaging.getInstance(context).disablePushRegistration();

        verifyRegistration(never);
        assertTrue(MobileMessaging.getInstance(context).isPushRegistrationEnabled());
    }


    private void verifyMessagesSynchronizer(VerificationMode verificationMode) throws InterruptedException {
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
        BroadcastReceiverMockito.verify(messagesSynchronizerReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifySeenStatusReporter(VerificationMode verificationMode) throws InterruptedException {
        String messageIds[] = {"1"};
        seenStatusReporter.report(context, messageIds, new MobileMessagingStats(getInstrumentation().getContext()), taskExecutor);
        BroadcastReceiverMockito.verify(seenStatusReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifyGeoReporting(VerificationMode verificationMode) throws InterruptedException {
        List<GeoReport> reports = new ArrayList<>();
        reports.add(new GeoReport("campaignId1", "messageId1", GeoEventType.entry, new Area("areaId1", "Area1", 1.0, 1.0, 3), 1001L));
        reports.add(new GeoReport("campaignId2", "messageId2", GeoEventType.exit, new Area("areaId2", "Area2", 2.0, 2.0, 4), 1002L));
        reports.add(new GeoReport("campaignId3", "messageId3", GeoEventType.dwell, new Area("areaId3", "Area3", 3.0, 3.0, 5), 1003L));

        Message messages[] = new Message[2];
        messages[0] = new Message();
        messages[0].setMessageId("signalingMessageId1");
        messages[0].setGeo(new Geo(null, null, null, null, null, "campaignId1", new ArrayList<Area>(){{
            add(reports[0].getArea());
            add(reports[1].getArea());
        }}, null));
        messages[1] = new Message();
        messages[1].setMessageId("signalingMessageId2");
        messages[1].setGeo(new Geo(null, null, null, null, null, "campaignId2", new ArrayList<Area>(){{
            add(reports[2].getArea());
        }}, null));

        MobileMessagingCore.getInstance(context).getMessageStoreForGeo().deleteAll(context);
        MobileMessagingCore.getInstance(context).getMessageStoreForGeo().save(context, messages);
        MobileMessagingCore.getInstance(context).addUnreportedGeoEvents(reports);
        geoReporter.report(context, MobileMessagingCore.getInstance(context).getStats());
        BroadcastReceiverMockito.verify(geoEventsReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifyRegistration(VerificationMode verificationMode) throws InterruptedException {
        registrationSynchronizer.updatePushRegistrationStatus(context, "TestDeviceInstanceId", null, MobileMessagingCore.getInstance(context).getStats(), taskExecutor);
        BroadcastReceiverMockito.verify(pusgRegistrationEnabledReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }
}
