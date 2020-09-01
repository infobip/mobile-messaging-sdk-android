package org.infobip.mobile.messaging.mobileapi.appinstance;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.InstallationMapper;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.api.support.ApiErrorCode;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.mobileapi.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class InstallationSynchronizerTest extends MobileMessagingTestCase {

    private InstallationSynchronizer installationSynchronizer;

    private MobileApiAppInstance mobileApiAppInstance = mock(MobileApiAppInstance.class);
    private MobileMessaging.ResultListener<Installation> actionListener = mock(MobileMessaging.ResultListener.class);
    private ArgumentCaptor<Result> captor;
    private Broadcaster broadcaster = Mockito.mock(Broadcaster.class);
    private Executor executor = new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
        }
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();
        captor = ArgumentCaptor.forClass(Result.class);
        MobileMessagingStats stats = mobileMessagingCore.getStats();
        RetryPolicyProvider retryPolicy = new RetryPolicyProvider(context);
        installationSynchronizer = new InstallationSynchronizer(context, mobileMessagingCore, stats, executor, broadcaster, retryPolicy, mobileApiAppInstance);
        when(mobileApiAppInstance.createInstance(any(AppInstance.class))).thenReturn(new AppInstance("pushRegId"));
        when(mobileApiAppInstance.getInstance(anyString())).thenReturn(new AppInstance("pushRegId"));
    }

    @Test
    public void shouldCreateInstallationOnServer() throws JSONException {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, false);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);

        installationSynchronizer.sync(actionListener);

        JSONObject bla = new JSONObject();
        bla.put("isPushRegistrationEnabled", false);

        InstallationMapper.fromJson(bla.toString());

        verifySuccess();
        verify(broadcaster, after(1000).times(1)).registrationCreated(anyString(), anyString());
        verify(mobileApiAppInstance, times(1)).createInstance(any(AppInstance.class));
    }

    @Test
    public void shouldNotCreateInstallationTwice() throws Exception {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, false);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);

        // we need same single thread executor here as we have it in release configuration
        // otherwise it will run on local thread and will have as many threads as we use to call `sync`
        final InstallationSynchronizer installationSynchronizer = new InstallationSynchronizer(
                context,
                mobileMessagingCore,
                mobileMessagingCore.getStats(),
                Executors.newSingleThreadExecutor(),
                broadcaster,
                new RetryPolicyProvider(context),
                mobileApiAppInstance);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                installationSynchronizer.sync(actionListener);
            }
        };

        Set<Thread> threads = CollectionUtils.setOf(new Thread(runnable), new Thread(runnable));
        for (Thread thread : threads) thread.start();
        for (Thread thread : threads) thread.join();

        verifySuccess(threads.size());
        verify(broadcaster, after(1000).times(1)).registrationCreated(anyString(), anyString());
        verify(mobileApiAppInstance, times(1)).createInstance(any(AppInstance.class));
    }

    @Test
    public void shouldReportErrorWhenCreatingOnServer() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, false);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);
        doThrow(new RuntimeException()).when(mobileApiAppInstance).createInstance(any(AppInstance.class));

        installationSynchronizer.sync(actionListener);

        verifyError();
        verify(broadcaster, after(1000).times(1)).error(any(MobileMessagingError.class));
    }

    @Test
    public void shouldPatchInstallationOnServer() {
        installationSynchronizer.sync(actionListener);

        verifySuccess();
        verify(broadcaster, after(1000).times(1)).installationUpdated(any(Installation.class));
        verify(mobileApiAppInstance, times(1)).patchInstance(anyString(), any(Map.class));
    }

    @Test
    public void shouldReportErrorWhenPatchingOnServer() {
        doThrow(new RuntimeException()).when(mobileApiAppInstance).patchInstance(anyString(), any(Map.class));

        installationSynchronizer.sync(actionListener);

        verifyError();
        verify(broadcaster, after(1000).times(1)).error(any(MobileMessagingError.class));
    }

    @Test
    public void shouldReportNoRegistrationErrorWhenPatchingOnServer() {
        //given
        doThrow(new ApiIOException(ApiErrorCode.NO_REGISTRATION, "No registration"))
                .when(mobileApiAppInstance).patchInstance(anyString(), any(Map.class));

        //when
        installationSynchronizer.sync(actionListener);

        //then
        verifyError();
        verify(broadcaster, after(1000).times(1)).error(any(MobileMessagingError.class));
    }

    @Test
    public void shouldGetInstallationFromServer() {
        installationSynchronizer.fetchInstance(actionListener);

        verifySuccess();
        verify(mobileApiAppInstance, times(1)).getInstance(anyString());
    }

    @Test
    public void shouldReportErrorGettingFromServer() {
        doThrow(new RuntimeException()).when(mobileApiAppInstance).getInstance(anyString());

        installationSynchronizer.fetchInstance(actionListener);

        verifyError();
    }

    @Test
    public void shouldReportNoRegistrationErrorWhenFetchingFromServer() {
        //given
        doThrow(new ApiIOException(ApiErrorCode.NO_REGISTRATION, "No registration"))
                .when(mobileApiAppInstance).getInstance(anyString());

        //when
        installationSynchronizer.fetchInstance(actionListener);

        //then
        verifyError();
    }

    private void verifySuccess() {
        verifySuccess(1);
    }

    private void verifySuccess(int numOfListenerInvocations) {
        verify(actionListener, after(1000).times(numOfListenerInvocations)).onResult(captor.capture());
        Result result = captor.getValue();
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertNull(result.getError());
    }

    private void verifyError() {
        verify(actionListener, after(1000).times(1)).onResult(captor.capture());
        Result result = captor.getValue();
        assertFalse(result.isSuccess());
        assertNotNull(result.getData());
        assertNotNull(result.getError());
    }
}
