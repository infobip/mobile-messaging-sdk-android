package org.infobip.mobile.messaging;

import android.annotation.SuppressLint;

import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.DefaultRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.data.SystemDataReporter;
import org.infobip.mobile.messaging.mobile.data.UserDataReporter;
import org.infobip.mobile.messaging.mobile.messages.MessagesSynchronizer;
import org.infobip.mobile.messaging.mobile.registration.RegistrationSynchronizer;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.DeviceInformation;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.SystemInformation;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import fi.iki.elonen.NanoHTTPD;

import static org.junit.Assert.assertEquals;

/**
 * @author pandric on 08/03/2017.
 */

public class RetryableSynchronizersTest extends MobileMessagingTestCase {

    private Executor executor;

    private SystemDataReporter systemDataReporter;
    private MessagesSynchronizer messagesSynchronizer;
    private RegistrationSynchronizer registrationSynchronizer;
    private UserDataReporter userDataReporter;
    private MRetryPolicy retryPolicy;

    @SuppressLint("CommitPrefEdits")
    @Override
    public void setUp() throws Exception {
        super.setUp();

        MobileMessagingStats stats = mobileMessagingCore.getStats();

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, true);
        PreferenceHelper.saveInt(context, MobileMessagingProperty.DEFAULT_EXP_BACKOFF_MULTIPLIER, 0);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);

        retryPolicy = DefaultRetryPolicy.create(context);
        executor = Executors.newSingleThreadExecutor();
        systemDataReporter = new SystemDataReporter(context, mobileMessagingCore, stats, retryPolicy, executor, broadcaster);
        messagesSynchronizer = new MessagesSynchronizer(context, mobileMessagingCore, stats, executor, broadcaster, retryPolicy, notificationHandler);
        registrationSynchronizer = new RegistrationSynchronizer(context, mobileMessagingCore, stats, executor, broadcaster, retryPolicy);
        userDataReporter = new UserDataReporter(context, mobileMessagingCore, executor, broadcaster, retryPolicy, stats);

        debugServer.respondWith(NanoHTTPD.Response.Status.INTERNAL_ERROR, "{\n" +
                "  \"code\": \"500\",\n" +
                "  \"message\": \"Internal server error\"\n" +
                "}");
    }

    @Test
    public void test_system_data_retry() {

        // Given
        prepareSystemData();

        // When
        systemDataReporter.synchronize();

        // Then
        Mockito.verify(broadcaster, Mockito.after(3000).times(1)).error(Mockito.any(MobileMessagingError.class));
        assertEquals(1 + retryPolicy.getMaxRetries(), debugServer.getBodiesForUri("/mobile/2/data/system").size());
    }

    private void prepareSystemData() {
        boolean reportEnabled = PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO);

        SystemData data = new SystemData(SoftwareInformation.getSDKVersionWithPostfixForSystemData(context),
                reportEnabled ? SystemInformation.getAndroidSystemVersion() : "",
                reportEnabled ? DeviceInformation.getDeviceManufacturer() : "",
                reportEnabled ? DeviceInformation.getDeviceModel() : "",
                reportEnabled ? SoftwareInformation.getAppVersion(context) : "",
                mobileMessagingCore.isGeofencingActivated(),
                SoftwareInformation.areNotificationsEnabled(context),
                DeviceInformation.isDeviceSecure(context));

        Integer hash = PreferenceHelper.findInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);
        if (hash != data.hashCode()) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA, data.toString());
        }
    }

    @Test
    public void test_sync_messages_retry() {

        // When
        messagesSynchronizer.sync();

        // Then
        Mockito.verify(broadcaster, Mockito.after(3000).atLeast(1)).error(Mockito.any(MobileMessagingError.class));
        assertEquals(1 + retryPolicy.getMaxRetries(), debugServer.getBodiesForUri("/mobile/5/messages").size());
    }

    @Test
    public void test_registration_retry() {

        // Given
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED, false);

        // When
        registrationSynchronizer.sync();

        // Then
        Mockito.verify(broadcaster, Mockito.after(3000).times(1)).error(Mockito.any(MobileMessagingError.class));
        assertEquals(1 + retryPolicy.getMaxRetries(), debugServer.getBodiesForUri("/mobile/4/registration").size());
    }

    @Test
    public void test_user_data_retry() {

        // Given
        UserData userData = new UserData();
        userData.setFirstName("Retry");
        userData.setLastName("Everything");

        // When
        userDataReporter.sync(null, userData);

        // Then
        Mockito.verify(broadcaster, Mockito.after(3000).atLeast(1)).error(Mockito.any(MobileMessagingError.class));
        assertEquals(1 + retryPolicy.getMaxRetries(), debugServer.getBodiesForUri("/mobile/4/data/user").size());
    }

    @Test
    public void test_user_data_opt_out_retry() {

        // Given
        withoutStoringUserData();

        UserData userData = new UserData();
        userData.setFirstName("Retry");
        userData.setLastName("Everything");

        // When
        userDataReporter.sync(null, userData);

        // Then
        Mockito.verify(broadcaster, Mockito.after(4000).times(1)).error(Mockito.any(MobileMessagingError.class));
    }

    private void withoutStoringUserData() {
        PreferenceHelper.saveBoolean(contextMock, MobileMessagingProperty.SAVE_USER_DATA_ON_DISK, false);
    }
}
