/*
 * MobileMessagingJobServiceTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.platform;

import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.PackageManager;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.infobip.mobile.messaging.platform.MobileMessagingJob.MM_JOB_SCHEDULER_START_ID;
import static org.infobip.mobile.messaging.platform.MobileMessagingJob.ON_NETWORK_AVAILABLE_JOB_ID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
        PackageManager packageManagerMock = Mockito.mock(PackageManager.class);
        MobileMessagingJobService givenJobService = spy(new MobileMessagingJobService(mmcMock));

        doReturn("123fgh").when(mmcMock).getApplicationCode();
        doReturn(packageManagerMock).when(givenJobService).getPackageManager();
        doReturn(givenJobScheduler).when(givenJobService).getSystemService(eq(Context.JOB_SCHEDULER_SERVICE));
        doReturn(getClass().getPackage().getName()).when(givenJobService).getPackageName();
        doReturn(ON_NETWORK_AVAILABLE_JOB_ID + MM_JOB_SCHEDULER_START_ID).when(givenJobParameters).getJobId();

        // When
        givenJobService.onStartJob(givenJobParameters);

        // Then
        verify(mmcMock, times(1)).retrySyncOnNetworkAvailable();
    }

    @Test
    public void shouldNotInvokeResyncWhenJobStartsAndAppCodeDoesNotExist() throws Exception {
        // Given
        PackageManager packageManagerMock = Mockito.mock(PackageManager.class);
        MobileMessagingJobService givenJobService = spy(new MobileMessagingJobService(mmcMock));

        doReturn(null).when(mmcMock).getApplicationCode();
        doReturn(packageManagerMock).when(givenJobService).getPackageManager();
        doReturn(givenJobScheduler).when(givenJobService).getSystemService(eq(Context.JOB_SCHEDULER_SERVICE));
        doReturn(getClass().getPackage().getName()).when(givenJobService).getPackageName();
        doReturn(ON_NETWORK_AVAILABLE_JOB_ID + MM_JOB_SCHEDULER_START_ID).when(givenJobParameters).getJobId();

        // When
        givenJobService.onStartJob(givenJobParameters);

        // Then
        verify(mmcMock, never()).retrySyncOnNetworkAvailable();
    }
}