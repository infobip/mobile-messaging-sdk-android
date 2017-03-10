package org.infobip.mobile.messaging.geo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.mobile.messages.MessagesSynchronizer;
import org.infobip.mobile.messaging.mobile.registration.RegistrationSynchronizer;
import org.infobip.mobile.messaging.mobile.seen.SeenStatusReporter;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.Brockito;
import org.infobip.mobile.messaging.tools.Helper;
import org.infobip.mobile.messaging.tools.InfobipAndroidTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fi.iki.elonen.NanoHTTPD;


public class PushUnregisteredTest extends InfobipAndroidTestCase {

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

    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        MobileMessagingStats stats = mobileMessagingCore.getStats();

        PreferenceHelper.saveLong(getInstrumentation().getContext(), MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);
        PreferenceHelper.saveBoolean(getInstrumentation().getContext(), MobileMessagingProperty.GEOFENCING_ACTIVATED, true);

        registrationSynchronizer = new RegistrationSynchronizer(context, stats, taskExecutor, mobileMessagingCore);
        seenStatusReporter = new SeenStatusReporter(context, stats, taskExecutor);
        geoReporter = new GeoReporter(context, stats);
        messagesSynchronizer = new MessagesSynchronizer(context, stats, taskExecutor);

        captor = ArgumentCaptor.forClass(Intent.class);
        geoEventsReceiver = Brockito.mock();
        pushRegistrationEnabledReceiver = Brockito.mock();
        seenStatusReceiver = Brockito.mock();
        messagesSynchronizerReceiver = Brockito.mock();
        errorReceiver = Brockito.mock();

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
        verifyGeoReporting(Mockito.after(4000).atLeastOnce());
        verifyMessagesSynchronizer(Mockito.after(90000).atLeastOnce());
    }

    public void test_push_registration_default_status() throws Exception {
        String response = "{\n" +
                        "  \"deviceApplicationInstanceId\": \"testDeviceApplicationInstanceId\",\n" +
                        "  \"pushRegistrationEnabled\": true" +
                        "}";
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, response);
        MobileMessaging.getInstance(context).disablePushRegistration(); // this method shall trigger pushRegistrationEnabledReceiver only once

        registrationSynchronizer.synchronize();
        Brockito.verify(pushRegistrationEnabledReceiver, Mockito.after(4000).times(1)).onReceive(Mockito.any(Context.class), captor.capture());
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
        Brockito.verify(messagesSynchronizerReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifySeenStatusReporter(VerificationMode verificationMode) throws InterruptedException {
        String messageIds[] = {"1"};
        mobileMessagingCore.setMessagesSeen(messageIds);
        seenStatusReporter.synchronize();
        Brockito.verify(seenStatusReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifyGeoReporting(VerificationMode verificationMode) throws InterruptedException {

        // Given
        GeoReport report1 = Helper.createReport(context, "signalingMessageId1", "campaignId1", "messageId1", true, Helper.createArea("areaId1"));
        GeoReport report2 = Helper.createReport(context, "signalingMessageId2", "campaignId2", "messageId2", true, Helper.createArea("areaId2"));
        GeoReport report3 = Helper.createReport(context, "signalingMessageId3", "campaignId3", "messageId3", true, Helper.createArea("areaId3"));
        Helper.createMessage(context, "signalingMessageId1", "campaignId1", true, report1.getArea(), report2.getArea());
        Helper.createMessage(context, "signalingMessageId2", "campaignId2", true, report3.getArea());

        // When
        geoReporter.synchronize();

        // Then
        Brockito.verify(geoEventsReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }

    private void verifyRegistrationStatusUpdate(VerificationMode verificationMode, boolean enable) throws InterruptedException {
        registrationSynchronizer.updatePushRegistrationStatus(enable);
        Brockito.verify(pushRegistrationEnabledReceiver, verificationMode).onReceive(Mockito.any(Context.class), captor.capture());
    }
}
