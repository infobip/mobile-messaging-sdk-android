package org.infobip.mobile.messaging.mobileapi.events;

import org.infobip.mobile.messaging.CustomEvent;
import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.InstallationMapper;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.api.appinstance.UserCustomEventBody;
import org.infobip.mobile.messaging.api.appinstance.UserSessionEventBody;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.BatchReporter;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.mobileapi.common.MAsyncTask;
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

public class UserEventsSynchronizer {

    private final MobileMessagingCore mobileMessagingCore;
    private final Broadcaster broadcaster;
    private final MobileApiAppInstance mobileApiAppInstance;
    private final Executor executor;
    private final MRetryPolicy policy;
    private final BatchReporter batchReporter;

    public UserEventsSynchronizer(
            MobileMessagingCore mobileMessagingCore,
            Broadcaster broadcaster,
            MobileApiAppInstance mobileApiAppInstance,
            MRetryPolicy policy,
            Executor executor,
            BatchReporter batchReporter) {

        this.mobileMessagingCore = mobileMessagingCore;
        this.broadcaster = broadcaster;
        this.mobileApiAppInstance = mobileApiAppInstance;
        this.policy = policy;
        this.executor = executor;
        this.batchReporter = batchReporter;
    }

    public void reportSessions() {
        if (mobileMessagingCore.isDepersonalizeInProgress()) {
            MobileMessagingLogger.w("Depersonalization is in progress, will report user session later");
            return;
        }

        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration not available yet, will report user session later");
            return;
        }

        final long sessionStartsMillis = mobileMessagingCore.getActiveSessionStartTime();
        long lastReportedSessionStartTime = mobileMessagingCore.getLastReportedActiveSessionStartTime();
        final String[] storedSessionBounds = mobileMessagingCore.getStoredSessionBounds();
        AppInstance systemData = getSystemDataForBackend();
        final UserSessionEventBody userSessionEventBody = UserEventsRequestMapper.createUserSessionEventRequest(sessionStartsMillis, storedSessionBounds, systemData);

        // if request cannot be created (missing params) or
        // if we already reported the active session and session bounds are also reported (absent) we don't send a request
        if (userSessionEventBody == null ||
                sessionStartsMillis == lastReportedSessionStartTime && userSessionEventBody.getSessionBounds().size() == 0) {
            return;
        }

        new MRetryableTask<Void, Void>() {

            @Override
            public Void run(Void[] voids) {
                MobileMessagingLogger.v("USER SESSION REPORT >>>", userSessionEventBody);
                mobileApiAppInstance.sendUserSessionReport(mobileMessagingCore.getPushRegistrationId(), userSessionEventBody);
                return null;
            }

            @Override
            public void after(Void aVoid) {
                MobileMessagingLogger.v("USER SESSION REPORT DONE <<<");
                mobileMessagingCore.setUserSessionsReported(storedSessionBounds, sessionStartsMillis);
                broadcaster.userSessionsReported();
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("USER SESSION REPORT ERROR <<<", error);
                MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);
                mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);
            }
        }
                .retryWith(policy)
                .execute(executor);
    }

    private AppInstance getSystemDataForBackend() {
        SystemData systemDataForReport = mobileMessagingCore.systemDataForReport(true);
        if (systemDataForReport == null) {
            return null;
        }
        Installation installation = mobileMessagingCore.populateInstallationWithSystemData(systemDataForReport, new Installation());
        return InstallationMapper.toBackend(installation);
    }

    public void reportCustomEvent(final CustomEvent customEvent, final MobileMessaging.ResultListener<CustomEvent> listener) {
        if (mobileMessagingCore.isDepersonalizeInProgress()) {
            MobileMessagingLogger.w("Depersonalization is in progress, will report custom event later");
            if (listener != null) {
                listener.onResult(new Result<>(customEvent, InternalSdkError.DEPERSONALIZATION_IN_PROGRESS.getError()));
            }
            return;
        }

        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration not available yet, not reporting provided custom event");
            if (listener != null) {
                listener.onResult(new Result<>(customEvent, InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
            return;
        }

        final UserCustomEventBody userCustomEventBody = UserEventsRequestMapper.createCustomEventRequest(customEvent);
        if (userCustomEventBody == null) {
            MobileMessagingLogger.w("Attempt to save empty custom event, will do nothing");
            if (listener != null) {
                listener.onResult(new Result<>(customEvent, InternalSdkError.ERROR_SAVING_EMPTY_OBJECT.getError()));
            }
            return;
        }

        new MAsyncTask<Void, Void>() {

            @Override
            public Void run(Void[] voids) {
                MobileMessagingLogger.v("CUSTOM EVENT REPORT >>>", userCustomEventBody);
                mobileApiAppInstance.sendUserCustomEvents(mobileMessagingCore.getPushRegistrationId(), true, userCustomEventBody);
                return null;
            }

            @Override
            public void after(Void aVoid) {
                MobileMessagingLogger.v("CUSTOM EVENT REPORT DONE <<<");
                broadcaster.customEventsReported();
                if (listener != null) {
                    listener.onResult(new Result<>(customEvent));
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("CUSTOM EVENT REPORT ERROR <<<", error);
                MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);
                mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);
                broadcaster.error(mobileMessagingError);
                if (listener != null) {
                    listener.onResult(new Result<>(customEvent, mobileMessagingError));
                }
            }

        }.execute(executor);
    }

    public void reportCustomEvents() {
        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration not available yet, will report custom event later");
            return;
        }

        batchReporter.put(new Runnable() {
            @Override
            public void run() {
                new MAsyncTask<Void, Void>() {

                    @Override
                    public Void run(Void[] voids) {
                        final UserCustomEventBody userCustomEventBody = new UserCustomEventBody(mobileMessagingCore.getUnreportedUserCustomEvents());
                        MobileMessagingLogger.v("CUSTOM EVENT REPORT >>>", userCustomEventBody);
                        mobileApiAppInstance.sendUserCustomEvents(mobileMessagingCore.getPushRegistrationId(), false, userCustomEventBody);
                        return null;
                    }

                    @Override
                    public void after(Void aVoid) {
                        MobileMessagingLogger.v("CUSTOM EVENT REPORT DONE <<<");
                        mobileMessagingCore.setUserCustomEventsReported();
                        broadcaster.customEventsReported();
                    }

                    @Override
                    public void error(Throwable error) {
                        MobileMessagingLogger.e("CUSTOM EVENT REPORT ERROR <<<", error);
                        MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);
                        mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);
                        broadcaster.error(mobileMessagingError);
                    }

                }.execute(executor);
            }
        });
    }
}
