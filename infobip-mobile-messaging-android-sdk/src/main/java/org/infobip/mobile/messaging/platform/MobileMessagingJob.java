package org.infobip.mobile.messaging.platform;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

/**
 * @author tjuric
 * @since 28/09/17.
 */

public class MobileMessagingJob {

    protected static final String MM_JOB_SCHEDULER_START_KEY = MobileMessagingJob.class.getCanonicalName();
    public static final int MM_JOB_SCHEDULER_START_ID = 7000000;
    private static Integer jobSchedulerStartId;

    public static final int ON_NETWORK_AVAILABLE_JOB_ID = 1;
    public static final int CLOUD_INTENT_JOB_ID = 2;

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
}
