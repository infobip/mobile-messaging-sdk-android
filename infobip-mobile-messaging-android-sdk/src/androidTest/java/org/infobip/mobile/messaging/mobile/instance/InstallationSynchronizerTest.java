package org.infobip.mobile.messaging.mobile.instance;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.AppInstanceWithPushRegId;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.appinstance.Installation;
import org.infobip.mobile.messaging.mobile.appinstance.InstallationActionListener;
import org.infobip.mobile.messaging.mobile.appinstance.InstallationSynchronizer;
import org.infobip.mobile.messaging.mobile.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author sslavin
 * @since 21/06/2018.
 */
public class InstallationSynchronizerTest extends MobileMessagingTestCase {

    private InstallationSynchronizer installationSynchronizer;

    private MobileApiAppInstance mobileApiAppInstance = mock(MobileApiAppInstance.class);
    private InstallationActionListener actionListener = mock(InstallationActionListener.class);
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
        MobileMessagingStats stats = mobileMessagingCore.getStats();
        RetryPolicyProvider retryPolicy = new RetryPolicyProvider(context);
        installationSynchronizer = new InstallationSynchronizer(context, mobileMessagingCore, stats, executor, broadcaster, retryPolicy, mobileApiAppInstance);
        when(mobileApiAppInstance.createInstance(anyBoolean(), any(AppInstance.class))).thenReturn(new AppInstanceWithPushRegId("pushRegId"));
        when(mobileApiAppInstance.getInstance(anyString())).thenReturn(new AppInstanceWithPushRegId("pushRegId"));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void shouldCreateInstallationOnServer() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, false);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);

        installationSynchronizer.sync(actionListener);

        verify(actionListener, after(300).times(1)).onSuccess(any(Installation.class));
        verify(mobileApiAppInstance, times(1)).createInstance(anyBoolean(), any(AppInstance.class));
    }

    @Test
    public void shouldReportErrorWhenCreatingOnServer() {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, false);
        PreferenceHelper.remove(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID);
        doThrow(new RuntimeException()).when(mobileApiAppInstance).createInstance(anyBoolean(), any(AppInstance.class));

        installationSynchronizer.sync(actionListener);

        verify(actionListener, after(300).times(1)).onError(any(MobileMessagingError.class));
    }

    @Test
    public void shouldPatchInstallationOnServer() {
        installationSynchronizer.sync(actionListener);

        verify(actionListener, after(300).times(1)).onSuccess(any(Installation.class));
        verify(mobileApiAppInstance, times(1)).patchInstance(anyString(), anyBoolean(), any(AppInstance.class));
    }

    @Test
    public void shouldReportErrorWhenPatchingOnServer() {
        doThrow(new RuntimeException()).when(mobileApiAppInstance).patchInstance(anyString(), anyBoolean(), any(AppInstance.class));

        installationSynchronizer.sync(actionListener);

        verify(actionListener, after(300).times(1)).onError(any(MobileMessagingError.class));
    }

    @Test
    public void shouldGetInstallationFromServer() {
        installationSynchronizer.fetchInstance(actionListener);

        verify(actionListener, after(300).times(1)).onSuccess(any(Installation.class));
        verify(mobileApiAppInstance, times(1)).getInstance(anyString());
    }

    @Test
    public void shouldReportErrorGettingFromServer() {
        doThrow(new RuntimeException()).when(mobileApiAppInstance).getInstance(anyString());

        installationSynchronizer.fetchInstance(actionListener);

        verify(actionListener, after(300).times(1)).onError(any(MobileMessagingError.class));
    }
}
