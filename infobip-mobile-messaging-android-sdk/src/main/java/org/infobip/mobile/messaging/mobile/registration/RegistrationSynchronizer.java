package org.infobip.mobile.messaging.mobile.registration;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.registration.MobileApiRegistration;
import org.infobip.mobile.messaging.api.registration.RegistrationResponse;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.MAsyncTask;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

/**
 * @author mstipanov
 * @since 07.04.2016.
 */
public class RegistrationSynchronizer {

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final Broadcaster broadcaster;
    private final MRetryPolicy retryPolicy;
    private final MobileApiRegistration mobileApiRegistration;

    public RegistrationSynchronizer(
            Context context,
            MobileMessagingCore mobileMessagingCore,
            MobileMessagingStats stats,
            Executor executor,
            Broadcaster broadcaster,
            MRetryPolicy retryPolicy,
            MobileApiRegistration mobileApiRegistration) {

        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.stats = stats;
        this.executor = executor;
        this.broadcaster = broadcaster;
        this.retryPolicy = retryPolicy;
        this.mobileApiRegistration = mobileApiRegistration;
    }

    public void updateStatus(final Boolean enabled) {
        new MAsyncTask<Boolean, Registration>() {
            @Override
            public Registration run(Boolean[] params) {
                String cloudToken = mobileMessagingCore.getCloudToken();
                Boolean pushRegistrationEnabled = params.length > 0 ? params[0] : null;
                MobileMessagingLogger.v("REGISTRATION >>>", cloudToken, pushRegistrationEnabled);
                RegistrationResponse registrationResponse = mobileApiRegistration.upsert(cloudToken, pushRegistrationEnabled);
                MobileMessagingLogger.v("REGISTRATION <<<", registrationResponse);
                return new Registration(cloudToken, registrationResponse.getDeviceApplicationInstanceId(), registrationResponse.getPushRegistrationEnabled());
            }

            @Override
            public void after(Registration registration) {
                setPushRegistrationEnabled(registration.enabled);
                setPushRegistrationId(registration.registrationId);
                setRegistrationIdReported(true);

                broadcaster.registrationEnabled(registration.cloudToken, registration.registrationId, registration.enabled);
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("MobileMessaging API returned error (push registration status update)!");
                mobileMessagingCore.setLastHttpException(error);
                stats.reportError(MobileMessagingStatsError.PUSH_REGISTRATION_STATUS_UPDATE_ERROR);
                broadcaster.error(MobileMessagingError.createFrom(error));
            }
        }
        .execute(executor, enabled);
    }

    public void sync() {
        if (isRegistrationIdReported()) {
            return;
        }

        reportCloudToken(mobileMessagingCore.getCloudToken());
    }

    private void reportCloudToken(final String cloudToken) {
        if (StringUtils.isBlank(cloudToken)) {
            return;
        }

        new MRetryableTask<String, Registration>() {
            @Override
            public Registration run(String[] params) {
                String cloudToken = params.length > 0 ? params[0] : null;
                MobileMessagingLogger.v("REGISTRATION >>>", cloudToken);
                RegistrationResponse registrationResponse = mobileApiRegistration.upsert(cloudToken, null);
                MobileMessagingLogger.v("REGISTRATION <<<", registrationResponse);
                return new Registration(cloudToken, registrationResponse.getDeviceApplicationInstanceId(), registrationResponse.getPushRegistrationEnabled());
            }

            @Override
            public void after(Registration registration) {
                setPushRegistrationEnabled(registration.enabled);
                setPushRegistrationId(registration.registrationId);
                setRegistrationIdReported(true);

                MobileMessagingCore.getInstance(context).reportSystemData();

                broadcaster.registrationCreated(registration.cloudToken, registration.registrationId);
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("MobileMessaging API returned error (registration)!");
                setRegistrationIdReported(false);

                mobileMessagingCore.setLastHttpException(error);
                stats.reportError(MobileMessagingStatsError.PUSH_REGISTRATION_STATUS_UPDATE_ERROR);
                broadcaster.error(MobileMessagingError.createFrom(error));
            }
        }
        .retryWith(retryPolicy)
        .execute(executor, cloudToken);
    }

    private void setPushRegistrationEnabled(Boolean pushRegistrationEnabled) {
        if (pushRegistrationEnabled == null) {
            return;
        }

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.PUSH_REGISTRATION_ENABLED, pushRegistrationEnabled);
    }

    private void setPushRegistrationId(String registrationId) {
        if (registrationId == null) {
            return;
        }

        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, registrationId);
    }

    public void setRegistrationIdReported(boolean reported) {
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED, reported);
    }

    public boolean isRegistrationIdReported() {
        return PreferenceHelper.findBoolean(context, MobileMessagingProperty.GCM_REGISTRATION_ID_REPORTED);
    }
}
