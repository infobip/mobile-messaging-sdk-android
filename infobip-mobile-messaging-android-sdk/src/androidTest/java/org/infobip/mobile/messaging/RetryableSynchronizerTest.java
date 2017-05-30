package org.infobip.mobile.messaging;

import android.annotation.SuppressLint;

import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.data.SystemDataReporter;
import org.infobip.mobile.messaging.mobile.data.UserDataSynchronizer;
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

/**
 * @author pandric on 08/03/2017.
 */

public class RetryableSynchronizerTest extends MobileMessagingTestCase {

    private Executor executor;

    private SystemDataReporter systemDataReporter;
    private MessagesSynchronizer messagesSynchronizer;
    private RegistrationSynchronizer registrationSynchronizer;
    private UserDataSynchronizer userDataSynchronizer;

    @SuppressLint("CommitPrefEdits")
    @Override
    public void setUp() throws Exception {
        super.setUp();

        MobileMessagingStats stats = mobileMessagingCore.getStats();

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, true);
        PreferenceHelper.saveInt(context, MobileMessagingProperty.DEFAULT_EXP_BACKOFF_MULTIPLIER, 0);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);

        executor = Executors.newSingleThreadExecutor();
        systemDataReporter = new SystemDataReporter(context, stats, executor, broadcaster);
        messagesSynchronizer = new MessagesSynchronizer(context, stats, executor, broadcaster, notificationHandler);
        registrationSynchronizer = new RegistrationSynchronizer(context, stats, executor, broadcaster);
        userDataSynchronizer = new UserDataSynchronizer(context, mobileMessagingCore, executor, broadcaster);

        debugServer.respondWith(NanoHTTPD.Response.Status.INTERNAL_ERROR, "{\n" +
                "  \"code\": \"500\",\n" +
                "  \"message\": \"Internal server error\"\n" +
                "}");
    }

    @Test
    public void test_system_data_retry() {

        // When
        reportSystemData();

        // Then
        Mockito.verify(broadcaster, Mockito.after(8000).atLeast(3)).error(Mockito.any(MobileMessagingError.class));
    }

    private void reportSystemData() {
        boolean reportEnabled = PreferenceHelper.findBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO);

        SystemData data = new SystemData(SoftwareInformation.getSDKVersionWithPostfixForSystemData(context),
                reportEnabled ? SystemInformation.getAndroidSystemVersion() : "",
                reportEnabled ? DeviceInformation.getDeviceManufacturer() : "",
                reportEnabled ? DeviceInformation.getDeviceModel() : "",
                reportEnabled ? SoftwareInformation.getAppVersion(context) : "",
                mobileMessagingCore.isGeofencingActivated(),
                SoftwareInformation.areNotificationsEnabled(context));

        Integer hash = PreferenceHelper.findInt(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);
        if (hash != data.hashCode()) {
            PreferenceHelper.saveString(context, MobileMessagingProperty.UNREPORTED_SYSTEM_DATA, data.toString());
        }
        systemDataReporter.synchronize();
    }

    @Test
    public void test_sync_messages_retry() {

        // When
        messagesSynchronizer.synchronize();

        // Then
        Mockito.verify(broadcaster, Mockito.after(4000).atLeast(4)).error(Mockito.any(MobileMessagingError.class));
    }

    @Test
    public void test_registration_retry() {

        // Given
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED, false);

        // When
        registrationSynchronizer.synchronize();

        // Then
        Mockito.verify(broadcaster, Mockito.after(4000).atLeast(4)).error(Mockito.any(MobileMessagingError.class));
    }

    @Test
    public void test_user_data_retry() {

        // Given
        UserData userData = new UserData();
        userData.setFirstName("Retry");
        userData.setLastName("Everything");

        // When
        userDataSynchronizer.synchronize(null, userData);

        // Then
        Mockito.verify(broadcaster, Mockito.after(10000).atLeast(4)).error(Mockito.any(MobileMessagingError.class));
    }

    public void test_user_data_opt_out_retry() {

        // Given
        withoutStoringUserData();

        UserData userData = new UserData();
        userData.setFirstName("Retry");
        userData.setLastName("Everything");

        // When
        userDataSynchronizer.synchronize(null, userData);

        // Then
        Mockito.verify(broadcaster, Mockito.after(4000).times(1)).error(Mockito.any(MobileMessagingError.class));
    }

    private void withoutStoringUserData() {
        PreferenceHelper.saveBoolean(contextMock, MobileMessagingProperty.SAVE_USER_DATA_ON_DISK, false);
    }
}
