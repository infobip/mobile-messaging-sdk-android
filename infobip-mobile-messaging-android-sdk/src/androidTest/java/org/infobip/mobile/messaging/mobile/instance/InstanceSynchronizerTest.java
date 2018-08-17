package org.infobip.mobile.messaging.mobile.instance;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.api.instance.Instance;
import org.infobip.mobile.messaging.api.instance.MobileApiInstance;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.RetryPolicyProvider;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.concurrent.Executor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author sslavin
 * @since 21/06/2018.
 */
public class InstanceSynchronizerTest extends MobileMessagingTestCase {

    private InstanceSynchronizer instanceSynchronizer;

    private InstanceServerListener serverListener = mock(InstanceServerListener.class);
    private MobileApiInstance mobileApiInstance = mock(MobileApiInstance.class);
    private BatchReporter batchReporter = new BatchReporter(0L);
    private InstanceActionListener actionListener = mock(InstanceActionListener.class);
    private Executor executor = new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
            command.run();
        }
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MRetryPolicy retryPolicy = new RetryPolicyProvider(context).DEFAULT();
        instanceSynchronizer = new InstanceSynchronizer(serverListener, executor, mobileApiInstance, batchReporter, retryPolicy);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        reset(serverListener);
    }

    @Test
    public void shouldReadPrimaryFromServer_onSync() {

        when(mobileApiInstance.get()).thenReturn(new Instance(true));

        instanceSynchronizer.sync();

        verify(mobileApiInstance, after(300).times(1)).get();
        verify(serverListener, times(1)).onPrimaryFetchedFromServer(eq(true));
    }

    @Test
    public void shouldSendPrimaryToServer() {

        instanceSynchronizer.sendPrimary(true, actionListener);

        verify(actionListener, after(300).times(1)).onPrimarySetSuccess();
        verify(mobileApiInstance, times(1)).update(argThat(new ArgumentMatcher<Instance>() {
            @Override
            public boolean matches(Object argument) {
                return ((Instance) (argument)).getPrimary();
            }
        }));
    }

    @Test
    public void shouldReportErrorWhenSending() {

        doThrow(new RuntimeException()).when(mobileApiInstance).update(any(Instance.class));

        instanceSynchronizer.sendPrimary(true, actionListener);

        verify(actionListener, after(300).times(1)).onPrimarySetError(any(Throwable.class));
    }
}
