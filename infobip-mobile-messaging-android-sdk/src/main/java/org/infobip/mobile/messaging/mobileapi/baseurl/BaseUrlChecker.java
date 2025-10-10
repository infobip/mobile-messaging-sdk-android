package org.infobip.mobile.messaging.mobileapi.baseurl;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.baseurl.BaseUrlResponse;
import org.infobip.mobile.messaging.api.baseurl.MobileApiBaseUrl;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.common.MAsyncTask;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BaseUrlChecker {

    private final Context context;
    private final Executor executor;
    private final MobileApiBaseUrl mobileApiBaseUrl;

    private final AtomicBoolean isSyncInProgress = new AtomicBoolean(false);

    public BaseUrlChecker(Context context, Executor executor, MobileApiBaseUrl mobileApiBaseUrl) {
        this.context = context;
        this.executor = executor;
        this.mobileApiBaseUrl = mobileApiBaseUrl;
    }

    public void sync() {
        if (!isSyncInProgress.compareAndSet(false, true)) return;

        long lastCheckTimeMillis = PreferenceHelper.findLong(context, MobileMessagingProperty.BASEURL_CHECK_LAST_TIME);
        int minimumIntervalHours = PreferenceHelper.findInt(context, MobileMessagingProperty.BASEURL_CHECK_INTERVAL_HOURS);
        long lastBaseUrlCheckHours = TimeUnit.MILLISECONDS.toHours(Time.now() - lastCheckTimeMillis);
        if (lastBaseUrlCheckHours < minimumIntervalHours) {
            isSyncInProgress.set(false);
            return;
        }

        new MAsyncTask<Void, BaseUrlResponse>() {
            @Override
            public BaseUrlResponse run(Void[] voids) {
                MobileMessagingLogger.v("BASE URL >>>");
                BaseUrlResponse response = mobileApiBaseUrl.getBaseUrl();
                MobileMessagingLogger.v("BASE URL DONE <<<", response);
                return response;
            }

            @Override
            public void after(BaseUrlResponse baseUrlResponse) {
                if (baseUrlResponse != null) {
                    String baseUrl = baseUrlResponse.getBaseUrl();
                    if (StringUtils.isNotBlank(baseUrl)) {
                        MobileMessagingCore.setApiUri(context, baseUrl);
                    }
                }
                PreferenceHelper.saveLong(context, MobileMessagingProperty.BASEURL_CHECK_LAST_TIME, Time.now());
                isSyncInProgress.set(false);
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("Error while checking base URL!", error);
                isSyncInProgress.set(false);
            }
        }
                .execute(executor);
    }
}
