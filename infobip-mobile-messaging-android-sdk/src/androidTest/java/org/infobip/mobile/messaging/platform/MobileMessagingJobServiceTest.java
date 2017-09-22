package org.infobip.mobile.messaging.platform;

import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.Context;
import android.support.test.runner.AndroidJUnit4;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

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

    private final MobileMessagingCore mmcMock = mock(MobileMessagingCore.class);
    private final JobScheduler givenJobScheduler = mock(JobScheduler.class);
    private final JobParameters givenJobParameters = mock(JobParameters.class);

    @After
    public void after() {
        reset(givenJobScheduler, mmcMock);
    }

    @Test
    public void shouldInvokeResyncWhenJobStarts() throws Exception {
        // Given
        MobileMessagingJobService givenJobService = spy(new MobileMessagingJobService(mmcMock));
        doReturn(givenJobScheduler).when(givenJobService).getSystemService(eq(Context.JOB_SCHEDULER_SERVICE));
        doReturn(getClass().getPackage().getName()).when(givenJobService).getPackageName();
        doReturn(ON_NETWORK_AVAILABLE_ID).when(givenJobParameters).getJobId();

        // When
        givenJobService.onStartJob(givenJobParameters);

        // Then
        verify(mmcMock, times(1)).retrySync();
    }
}