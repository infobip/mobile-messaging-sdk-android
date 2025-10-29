/*
 * MobileMessagingJobService.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.platform;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import static org.infobip.mobile.messaging.platform.MobileMessagingJob.ON_NETWORK_AVAILABLE_JOB_ID;
import static org.infobip.mobile.messaging.platform.MobileMessagingJob.getScheduleId;

/**
 * Service that handles scheduled jobs
 *
 * @author sslavin
 * @since 14/09/2017.
 */

@SuppressLint("SpecifyJobSchedulerIdRange")
@TargetApi(Build.VERSION_CODES.N)
public class MobileMessagingJobService extends JobService {

    private static final String TAG = MobileMessagingJobService.class.getSimpleName();

    private MobileMessagingCore mobileMessagingCore;

    @SuppressWarnings("unused")
    public MobileMessagingJobService() {
    }

    @VisibleForTesting
    public MobileMessagingJobService(MobileMessagingCore mobileMessagingCore) {
        this.mobileMessagingCore = mobileMessagingCore;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        int connectivityScheduleId = getScheduleId(this, ON_NETWORK_AVAILABLE_JOB_ID);
        if (params.getJobId() == connectivityScheduleId) {
            if (TextUtils.isEmpty(mobileMessagingCore().getApplicationCode())) {
                return false;
            }
            MobileMessagingLogger.d(TAG, "Network available");
            mobileMessagingCore().retrySyncOnNetworkAvailable();
            return false;
        }

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    /**
     * We need to register a job to detect network unavailability.
     * Then we can register another one to trigger resync when network is available.
     *
     * @param context android context object
     */
    public static void registerJobForConnectivityUpdates(Context context) {
        registerForNetworkAvailability(context);
    }

    //region Private methods
    @NonNull
    @SuppressLint("SpecifyJobSchedulerIdRange")
    private MobileMessagingCore mobileMessagingCore() {
        if (mobileMessagingCore == null) {
            mobileMessagingCore = MobileMessagingCore.getInstance(getApplicationContext());
        }
        return mobileMessagingCore;
    }

    private static void registerForNetworkAvailability(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }

        int scheduleId = getScheduleId(context, ON_NETWORK_AVAILABLE_JOB_ID);

        jobScheduler.cancel(scheduleId);

        int r = jobScheduler.schedule(new JobInfo.Builder(scheduleId, new ComponentName(context, MobileMessagingJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build());
        if (r == JobScheduler.RESULT_SUCCESS) {
            MobileMessagingLogger.d(TAG, "Registered job for connectivity updates");
        } else {
            MobileMessagingLogger.e(TAG, "Failed to register job for connectivity updates");
        }
    }
    //endregion
}
