package org.infobip.mobile.messaging.platform;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

/**
 * Service that handles scheduled jobs
 *
 * @author sslavin
 * @since 14/09/2017.
 */

@TargetApi(Build.VERSION_CODES.N)
public class MobileMessagingJobService extends JobService {

    private static final String TAG = MobileMessagingJobService.class.getSimpleName();
    protected static final String MM_JOB_SCHEDULER_START_KEY = MobileMessagingJobService.class.getCanonicalName();
    protected static final int MM_JOB_SCHEDULER_START_ID = 7000000;
    protected static final int ON_NETWORK_AVAILABLE_ID = 1;
    private static Integer jobSchedulerStartId;

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
        int connectivityScheduleId = getScheduleId(this, ON_NETWORK_AVAILABLE_ID);
        if (params.getJobId() == connectivityScheduleId) {
            MobileMessagingLogger.d(TAG, "Network available");
            mobileMessagingCore().retrySync();
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

    /**
     * Gets schedule ID that's used as job ID for scheduling jobs with {@link JobScheduler}.
     * <p>
     * Schedule ID is a sum of {@link #MM_JOB_SCHEDULER_START_ID} and particular job ID. {@link #MM_JOB_SCHEDULER_START_ID} might be
     * overridden with the value under {@link #MM_JOB_SCHEDULER_START_KEY} key in meta data in AndroidManifest.
     *
     * @param context Android context object
     * @param jobId   Predefined job ID
     * @return Schedule ID
     */
    public static int getScheduleId(Context context, int jobId) {
        if (jobSchedulerStartId == null) {
            ApplicationInfo ai = null;
            try {
                ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                MobileMessagingLogger.e("Failed to get application info.");
            }

            if (ai != null && ai.metaData != null) {
                jobSchedulerStartId = ai.metaData.getInt(MM_JOB_SCHEDULER_START_KEY, MM_JOB_SCHEDULER_START_ID);
            } else {
                jobSchedulerStartId = MM_JOB_SCHEDULER_START_ID;
            }
        }

        return jobSchedulerStartId + jobId;
    }

    //region Private methods
    @NonNull
    private MobileMessagingCore mobileMessagingCore() {
        if (mobileMessagingCore == null) {
            mobileMessagingCore = MobileMessagingCore.getInstance(this);
        }
        return mobileMessagingCore;
    }

    private static void registerForNetworkAvailability(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) {
            return;
        }

        int scheduleId = getScheduleId(context, ON_NETWORK_AVAILABLE_ID);

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
