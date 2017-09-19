package org.infobip.mobile.messaging.platform;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
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
    protected static final int ON_NETWORK_AVAILABLE_ID = 1;

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
        switch (params.getJobId()) {
            case ON_NETWORK_AVAILABLE_ID:
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

        jobScheduler.cancel(ON_NETWORK_AVAILABLE_ID);

        int r = jobScheduler.schedule(new JobInfo.Builder(ON_NETWORK_AVAILABLE_ID, new ComponentName(context, MobileMessagingJobService.class))
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
