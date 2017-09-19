package org.infobip.mobile.messaging.platform;

import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.Context;
import android.net.Uri;
import android.os.PersistableBundle;
import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.InvocationTargetException;

import static org.infobip.mobile.messaging.platform.MobileMessagingJobService.ON_NETWORK_AVAILABLE_ID;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author sslavin
 * @since 18/09/2017.
 */

@RunWith(AndroidJUnit4.class)
public class MobileMessagingJobServiceTest {

    private MobileMessagingCore mmcMock = mock(MobileMessagingCore.class);
    private JobScheduler jobScheduler = mock(JobScheduler.class);

    @After
    public void after() {
        reset(jobScheduler, mmcMock);
    }

    @Test
    public void shouldInvokeResyncWhenJobStarts() throws Exception {
        // Given
        MobileMessagingJobService givenJobService = spy(new MobileMessagingJobService(mmcMock));
        doReturn(jobScheduler).when(givenJobService).getSystemService(eq(Context.JOB_SCHEDULER_SERVICE));
        doReturn(getClass().getPackage().getName()).when(givenJobService).getPackageName();

        // When
        givenJobService.onStartJob(givenJob(ON_NETWORK_AVAILABLE_ID));

        // Then
        verify(mmcMock, times(1)).retrySync();
    }

    //region private methods
    /**
     * Creates job parameters in a hacky way through reflection.
     * <p/>
     * Because constructor for {@link JobParameters} is not publically available.
     *
     * @return job parameters
     */
    private JobParameters givenJob(int jobId) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        try {
            return (JobParameters) JobParameters.class.getConstructors()[0]
                    .newInstance(null, jobId, new PersistableBundle(), false, new Uri[0], new String[0]);
        } catch (IllegalArgumentException ignored) {
            return (JobParameters) JobParameters.class.getConstructors()[0]
                    .newInstance(null, jobId, new PersistableBundle(), false);
        }
    }
    //endregion
}
