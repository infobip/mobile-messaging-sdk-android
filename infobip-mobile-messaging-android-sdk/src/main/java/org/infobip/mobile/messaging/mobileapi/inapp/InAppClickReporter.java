package org.infobip.mobile.messaging.mobileapi.inapp;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.clickreporter.MobileApiClickReporter;
import org.infobip.mobile.messaging.api.support.ApiException;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.BatchReporter;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

public class InAppClickReporter {
    private final MobileMessagingCore mobileMessagingCore;
    private final Context context;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final Broadcaster broadcaster;
    private final BatchReporter batchReporter;
    private final MRetryPolicy retryPolicy;
    private final MobileApiClickReporter mobileApiClickReporter;

    public InAppClickReporter(
            MobileMessagingCore mobileMessagingCore,
            Context context,
            MobileMessagingStats stats,
            Executor executor,
            Broadcaster broadcaster,
            BatchReporter batchReporter,
            MRetryPolicy retryPolicy,
            MobileApiClickReporter mobileApiClickReporter) {
        this.mobileMessagingCore = mobileMessagingCore;
        this.context = context;
        this.stats = stats;
        this.executor = executor;
        this.broadcaster = broadcaster;
        this.batchReporter = batchReporter;
        this.retryPolicy = retryPolicy;
        this.mobileApiClickReporter = mobileApiClickReporter;
    }

    public void sync() {
        String[] unreportedInAppClickIds = mobileMessagingCore.getUnreportedInAppClickActions();
        if (unreportedInAppClickIds.length == 0) {
            return;
        }


        batchReporter.put(() -> new MRetryableTask<Void, String[]>() {
            @Override
            public String[] run(Void[] voids) {
                if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
                    MobileMessagingLogger.w("Push reg ID wasn't fetched upon the click!");
                }

                String[] clickActions = mobileMessagingCore.getUnreportedInAppClickActions();
                if (clickActions.length == 0) {
                    return clickActions;
                }

                MobileMessagingLogger.v("INAPP CLICK REPORT >>>");
                for (String clickAction : clickActions) {
                    makeHttpRequest(clickAction);
                }
                MobileMessagingLogger.v("INAPP CLICK REPORT DONE <<<");

                return clickActions;
            }

            @Override
            public void after(String[] clickUrls) {
                String[] clickActionUrlsFromReports = mobileMessagingCore.getInAppClickUrlsFromReports(clickUrls).toArray(new String[0]);
                broadcaster.inAppClickReported(clickActionUrlsFromReports);
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("INAPP CLICK REPORT ERROR <<<", error);
                stats.reportError(MobileMessagingStatsError.IN_APP_CLICK_ERROR);
                broadcaster.error(MobileMessagingError.createFrom(error));
            }
        }
                .retryWith(retryPolicy)
                .execute(executor));
    }

    private void makeHttpRequest(String clickAction) {
        String[] payload = clickAction.split(StringUtils.COMMA_WITH_SPACE); // [0] - clickUrl, [1] - buttonIdx, [2] - userAgent, [3] - attempt
        if (Integer.parseInt(payload[3]) == retryPolicy.getMaxRetries()) {
            mobileMessagingCore.removeReportedInAppClickActions(clickAction);
            return;
        }
        try {
            String clickUrl = payload[0];
            String authorization = "App " + mobileMessagingCore.getApplicationCode();
            String pushRegistrationId = mobileMessagingCore.getPushRegistrationId();
            String buttonIdx = payload[1];
            String userAgent = payload[2];

            mobileApiClickReporter.get(clickUrl, authorization, pushRegistrationId, buttonIdx, userAgent);

            MobileMessagingLogger.d("InApp click reported successfully to: " + clickUrl);
            mobileMessagingCore.removeReportedInAppClickActions(clickAction);
        } catch (ApiException e) {
            MobileMessagingLogger.e("Failed to report InApp click: " + e.getMessage());
            mobileMessagingCore.removeReportedInAppClickActions(clickAction);
            payload[3] = String.valueOf(Integer.parseInt(payload[3]) + 1);
            PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_IN_APP_CLICK_URLS, StringUtils.concat(payload, StringUtils.COMMA_WITH_SPACE));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


