package org.infobip.mobile.messaging.mobile.synchronizer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient.ErrorCode;
import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author pandric
 * @since 03/03/2017.
 */
public abstract class RetryableSynchronizer implements Synchronizer {

    private Handler handler = new Handler(Looper.getMainLooper());

    private int retryCnt = 0;

    protected final Context context;
    protected final MobileMessagingStats stats;
    protected final Executor executor;
    private final int maxRetryCount;
    private final int expBackoff;

    public RetryableSynchronizer(Context context, MobileMessagingStats stats) {
        this.context = context;
        this.stats = stats;
        this.executor = Executors.newSingleThreadExecutor();
        this.maxRetryCount = PreferenceHelper.findInt(context, MobileMessagingProperty.DEFAULT_MAX_RETRY_COUNT);
        this.expBackoff = PreferenceHelper.findInt(context, MobileMessagingProperty.DEFAULT_EXP_BACKOFF_MULTIPLIER);
    }

    public RetryableSynchronizer(Context context, MobileMessagingStats stats, Executor executor) {
        this.context = context;
        this.stats = stats;
        this.executor = executor;
        this.maxRetryCount = PreferenceHelper.findInt(context, MobileMessagingProperty.DEFAULT_MAX_RETRY_COUNT);
        this.expBackoff = PreferenceHelper.findInt(context, MobileMessagingProperty.DEFAULT_EXP_BACKOFF_MULTIPLIER);
    }

    @Override
    public void updatePushRegistrationStatus(Boolean enabled) {
    }

    @Override
    public void synchronize() {
    }

    @Override
    public void synchronize(MobileMessaging.ResultListener listener) {
    }

    protected int maxRetryCount() {
        return maxRetryCount;
    }

    protected void retry(UnsuccessfulResult result) {
        if (result == null) {
            return;
        }

        if (result.hasError() && (result.getError() instanceof ApiIOException)) {
            ApiIOException apiBackendException = (ApiIOException) result.getError();
            String errorCode = apiBackendException.getCode();

            if (errorCode == null) {
                return;
            }

            if (errorCode.equals(ErrorCode.UNKNOWN_API_BACKEND_ERROR.getValue())) {
                ++retryCnt;
                scheduleTask(retryCnt);
            }
        }
    }

    private void scheduleTask(final int numOfAttempts) {
        if (numOfAttempts > maxRetryCount()) {
            handler.removeCallbacksAndMessages(null);
            retryCnt = 0;
            return;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                switch (getTask()) {
                    case SYNC_USER_DATA:
                        synchronize(null);
                        break;
                    default:
                        synchronize();
                }
            }
        }, TimeUnit.SECONDS.toMillis(numOfAttempts * numOfAttempts * expBackoff));
    }
}
