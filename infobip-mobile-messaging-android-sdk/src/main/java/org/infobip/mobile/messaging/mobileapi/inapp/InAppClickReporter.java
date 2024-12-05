package org.infobip.mobile.messaging.mobileapi.inapp;

import android.content.Context;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.support.util.ApiConstants;
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class InAppClickReporter {
    private final MobileMessagingCore mobileMessagingCore;
    private final Context context;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final Broadcaster broadcaster;
    private final BatchReporter batchReporter;
    private final MRetryPolicy retryPolicy;

    public InAppClickReporter(
            MobileMessagingCore mobileMessagingCore,
            Context context,
            MobileMessagingStats stats,
            Executor executor,
            Broadcaster broadcaster,
            BatchReporter batchReporter,
            MRetryPolicy retryPolicy) {
        this.mobileMessagingCore = mobileMessagingCore;
        this.context = context;
        this.stats = stats;
        this.executor = executor;
        this.broadcaster = broadcaster;
        this.batchReporter = batchReporter;
        this.retryPolicy = retryPolicy;
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

                MobileMessagingLogger.v("INAPPCLICK >>>");
                for (String clickAction : clickActions) {
                    makeHttpRequest(clickAction);
                }
                MobileMessagingLogger.v("INAPPCLICK DONE <<<");

                return clickActions;
            }

            @Override
            public void after(String[] clickUrls) {
                String[] clickActionUrlsFromReports = mobileMessagingCore.getInAppClickUrlsFromReports(clickUrls).toArray(new String[0]);
                broadcaster.inAppClickReported(clickActionUrlsFromReports);
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("Error reporting in-app click status!");
                stats.reportError(MobileMessagingStatsError.IN_APP_CLICK_ERROR);
                broadcaster.error(MobileMessagingError.createFrom(error));
            }
        }
                .retryWith(retryPolicy)
                .execute(executor));
    }

    private @NonNull Map<String, String> getHeaders(String[] payload) {
        Map<String, String> headers = new HashMap<>();
        if (mobileMessagingCore.getApplicationCode() != null) {
            headers.put(ApiConstants.AUTHORIZATION, "App " + mobileMessagingCore.getApplicationCode());
            headers.put(ApiConstants.PUSH_REGISTRATION_ID, mobileMessagingCore.getPushRegistrationId());
            headers.put("buttonidx", payload[1]);
            headers.put(ApiConstants.USER_AGENT, payload[2]);
        }
        return headers;
    }

    private void makeHttpRequest(String clickAction) {
        String[] payload = clickAction.split(StringUtils.COMMA_WITH_SPACE); // [0] - clickUrl, [1] - buttonIdx, [2] - userAgent, [3] - attempt
        if (Integer.parseInt(payload[3]) == retryPolicy.getMaxRetries()) {
            mobileMessagingCore.removeReportedInAppClickActions(clickAction);
            return;
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(payload[0]).openConnection();

            conn.setRequestMethod("GET");

            for (Map.Entry<String, String> header : getHeaders(payload).entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }

            int responseCode = conn.getResponseCode();
            MobileMessagingLogger.d("Response Code : " + responseCode);
            if (responseCode == 200) {
                mobileMessagingCore.removeReportedInAppClickActions(clickAction);
            } else {
                mobileMessagingCore.removeReportedInAppClickActions(clickAction);
                payload[3] = String.valueOf(Integer.parseInt(payload[3]) + 1);
                PreferenceHelper.appendToStringArray(context, MobileMessagingProperty.INFOBIP_UNREPORTED_IN_APP_CLICK_URLS, StringUtils.concat(payload, StringUtils.COMMA_WITH_SPACE));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


