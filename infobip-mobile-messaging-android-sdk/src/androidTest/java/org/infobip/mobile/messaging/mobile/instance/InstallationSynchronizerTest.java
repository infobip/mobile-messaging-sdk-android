package org.infobip.mobile.messaging.mobile.instance;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.Result;
import org.infobip.mobile.messaging.mobile.appinstance.InstallationSynchronizer;
import org.infobip.mobile.messaging.mobile.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
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
        when(mobileApiAppInstance.createInstance(anyBoolean(), any(AppInstance.class))).thenReturn(new AppInstance("pushRegId"));
        when(mobileApiAppInstance.getInstance(anyString())).thenReturn(new AppInstance("pushRegId"));
    }

    @Test
    public void shouldCreateInstallationOnServer() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, false);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);

        installationSynchronizer.sync(actionListener);

        verifySuccess();
        verify(broadcaster, after(300).times(1)).registrationCreated(anyString(), anyString());
        verify(mobileApiAppInstance, times(1)).createInstance(anyBoolean(), any(AppInstance.class));
    }

    @Test
    public void shouldReportErrorWhenCreatingOnServer() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, false);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);
        doThrow(new RuntimeException()).when(mobileApiAppInstance).createInstance(anyBoolean(), any(AppInstance.class));

        installationSynchronizer.sync(actionListener);

        verifyError();
        verify(broadcaster, after(300).times(1)).error(any(MobileMessagingError.class));
    }

    @Test
    public void shouldPatchInstallationOnServer() {
        installationSynchronizer.sync(actionListener);

        verifySuccess();
        verify(broadcaster, after(300).times(1)).installationUpdated(any(Installation.class));
        verify(mobileApiAppInstance, times(1)).patchInstance(anyString(), anyBoolean(), any(Map.class));
    }

    @Test
    public void shouldReportErrorWhenPatchingOnServer() {
        doThrow(new RuntimeException()).when(mobileApiAppInstance).patchInstance(anyString(), anyBoolean(), any(Map.class));

        installationSynchronizer.sync(actionListener);

        verifyError();
        verify(broadcaster, after(300).times(1)).error(any(MobileMessagingError.class));
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

    private void verifySuccess() {
        verify(actionListener, after(300).times(1)).onResult(captor.capture());
        Result result = captor.getValue();
        assertNotNull(result.getData());
        assertTrue(result.isSuccess());
        assertNull(result.getError());
    }

    private void verifyError() {
        verify(actionListener, after(300).times(1)).onResult(captor.capture());
        Result result = captor.getValue();
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertNotNull(result.getError());
    }
}
