package org.infobip.mobile.messaging.geo;

import android.annotation.SuppressLint;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.mobile.geo.GeoReporter;
import org.infobip.mobile.messaging.mobile.messages.MessagesSynchronizer;
import org.infobip.mobile.messaging.mobile.registration.RegistrationSynchronizer;
import org.infobip.mobile.messaging.mobile.seen.SeenStatusReporter;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fi.iki.elonen.NanoHTTPD;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;


public class PushUnregisteredTest extends MobileMessagingTestCase {

    private GeoReporter geoReporter;
    private SeenStatusReporter seenStatusReporter;
    private RegistrationSynchronizer registrationSynchronizer;
    private MessagesSynchronizer messagesSynchronizer;

    private ArgumentCaptor<Boolean> captor;

    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    @Override
    public void setUp() throws Exception {
        super.setUp();
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        MobileMessagingStats stats = mobileMessagingCore.getStats();

        PreferenceHelper.saveLong(context, MobileMessagingProperty.BATCH_REPORTING_DELAY, 100L);
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GEOFENCING_ACTIVATED, true);

        registrationSynchronizer = new RegistrationSynchronizer(context, stats, taskExecutor, broadcaster);
        seenStatusReporter = new SeenStatusReporter(context, stats, taskExecutor, broadcaster);
        geoReporter = new GeoReporter(context, broadcaster, stats);
        messagesSynchronizer = new MessagesSynchronizer(context, stats, taskExecutor, broadcaster);

        captor = ArgumentCaptor.forClass(Boolean.class);
    }

    @Test
    public void test_push_registration_disabled() throws Exception {
        String response =
                "{\n" +
                        "  \"deviceApplicationInstanceId\": \"testDeviceApplicationInstanceId\",\n" +
                        "  \"pushRegistrationEnabled\": false\n" +
                        "}";

        debugServer.respondWith(NanoHTTPD.Response.Status.OK, response);

        verifyRegistrationStatusUpdate(Mockito.after(1000).atLeastOnce(), false);
        boolean isPushRegistrationEnabled = captor.getValue();
        assertFalse(isPushRegistrationEnabled);

        verifySeenStatusReporter(Mockito.after(1000).atLeastOnce());

        // reports should NOT be called if push is disabled
        verifyGeoReporting(Mockito.after(1000).never());
        verifyMessagesSynchronizer(Mockito.after(1000).never());
    }

    @Test
    public void test_push_registration_enabled() throws Exception {
        String response = "{\n" +
                "  \"deviceApplicationInstanceId\": \"testDeviceApplicationInstanceId\",\n" +
                "  \"pushRegistrationEnabled\": true" +
                "}";
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, response);

        verifyRegistrationStatusUpdate(Mockito.after(1000).atLeastOnce(), true);
        boolean isPushRegistrationEnabled = captor.getValue();
        assertTrue(isPushRegistrationEnabled);
        verifySeenStatusReporter(Mockito.after(1000).atLeastOnce());

        // reports should BE called if push is enabled
        verifyGeoReporting(Mockito.after(1000).atLeastOnce());
        verifyMessagesSynchronizer(Mockito.after(1000).atLeastOnce());
    }

    @Test
    public void test_push_registration_default_status() throws Exception {
        String response = "{\n" +
                        "  \"deviceApplicationInstanceId\": \"testDeviceApplicationInstanceId\",\n" +
                        "  \"pushRegistrationEnabled\": true" +
                        "}";
        debugServer.respondWith(NanoHTTPD.Response.Status.OK, response);
        mobileMessagingCore.disablePushRegistration(); // this method shall trigger pushRegistrationEnabledReceiver only once

        registrationSynchronizer.synchronize();
        Mockito.verify(broadcaster, Mockito.after(1000).times(1)).registrationEnabled(Mockito.anyString(), Mockito.anyString(), Mockito.eq(true));
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

        Mockito.verify(broadcaster, verificationMode).messageReceived(Mockito.any(Message.class));
    }

    private void verifySeenStatusReporter(VerificationMode verificationMode) throws InterruptedException {
        String messageIds[] = {"1"};
        mobileMessagingCore.setMessagesSeen(messageIds);
        seenStatusReporter.synchronize();

        Mockito.verify(broadcaster, verificationMode).seenStatusReported(Mockito.any(String[].class));
    }

    private void verifyGeoReporting(VerificationMode verificationMode) throws InterruptedException {

        // Given
        GeoReport report1 = createReport(context, "signalingMessageId1", "campaignId1", "messageId1", true, createArea("areaId1"));
        GeoReport report2 = createReport(context, "signalingMessageId2", "campaignId2", "messageId2", true, createArea("areaId2"));
        GeoReport report3 = createReport(context, "signalingMessageId3", "campaignId3", "messageId3", true, createArea("areaId3"));
        createMessage(context, "signalingMessageId1", "campaignId1", true, report1.getArea(), report2.getArea());
        createMessage(context, "signalingMessageId2", "campaignId2", true, report3.getArea());

        // When
        geoReporter.synchronize();

        // Then
        //noinspection unchecked
        Mockito.verify(broadcaster, verificationMode).geoReported(Mockito.any(List.class));
    }

    private void verifyRegistrationStatusUpdate(VerificationMode verificationMode, boolean enable) throws InterruptedException {
        registrationSynchronizer.updatePushRegistrationStatus(enable);
        Mockito.verify(broadcaster, verificationMode).registrationEnabled(Mockito.anyString(), Mockito.anyString(), captor.capture());
    }
}
