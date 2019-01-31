package org.infobip.mobile.messaging.mobile.user;


import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserAttributes;
import org.infobip.mobile.messaging.UserIdentity;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.api.appinstance.UserPersonalizeBody;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.Result;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendInvalidParameterException;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

public class PersonalizeSynchronizer {

    private final MobileMessagingCore mobileMessagingCore;
    private final Broadcaster broadcaster;
    private final MobileApiAppInstance mobileApiAppInstance;
    private final Executor executor;
    private final BatchReporter batchReporter;
    private final MRetryPolicy policy;
    private final DepersonalizeServerListener serverListener;

    public PersonalizeSynchronizer(
            MobileMessagingCore mobileMessagingCore,
            Broadcaster broadcaster,
            MobileApiAppInstance mobileApiAppInstance,
            MRetryPolicy policy,
            Executor executor,
            BatchReporter batchReporter,
            DepersonalizeServerListener serverListener) {

        this.mobileMessagingCore = mobileMessagingCore;
        this.broadcaster = broadcaster;
        this.mobileApiAppInstance = mobileApiAppInstance;
        this.policy = policy;
        this.executor = executor;
        this.batchReporter = batchReporter;
        this.serverListener = serverListener;
    }

    public void personalize(final UserIdentity userIdentity, final UserAttributes userAttributes, final boolean forceDepersonalize, final MobileMessaging.ResultListener<User> listener) {
        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration not available yet, will patch user data later");
            if (listener != null) {
                listener.onResult(new Result<>(mobileMessagingCore.getUser(), InternalSdkError.NO_VALID_REGISTRATION.getError()));
            }
            return;
        }

        final UserPersonalizeBody userPersonalizeBody = new UserPersonalizeBody();
        userPersonalizeBody.setUserIdentity(userIdentity.getMap());
        if (userAttributes != null && userAttributes.hasDataToReport()) {
            userPersonalizeBody.setUserAttributes(userAttributes.getMap());
        }

        new MRetryableTask<UserPersonalizeBody, Void>() {

            @Override
            public Void run(UserPersonalizeBody[] userPersonalizeBodies) {
                MobileMessagingLogger.v("PERSONALIZE >>>", userPersonalizeBody);
                mobileApiAppInstance.personalize(mobileMessagingCore.getPushRegistrationId(), forceDepersonalize, userPersonalizeBody);
                return null;
            }

            @Override
            public void after(Void aVoid) {
                MobileMessagingLogger.v("PERSONALIZE <<<");
                mobileMessagingCore.setUserDataReported(new User(userIdentity, userAttributes), true);

                User userToReturn = mobileMessagingCore.getUser();
                broadcaster.personalized(userToReturn);

                if (listener != null) {
                    listener.onResult(new Result<>(userToReturn));
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.v("PERSONALIZE ERROR <<<", error);
                MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);

                broadcaster.error(mobileMessagingError);

                if (listener != null) {
                    listener.onResult(new Result<>(mobileMessagingCore.getUser(), mobileMessagingError));
                }

                if (error instanceof BackendInvalidParameterException) {
                    mobileMessagingCore.setUserDataReportedWithError();
                }
            }
        }
                .retryWith(policy)
                .execute(executor, userPersonalizeBody);
    }

    public void depersonalize() {
        batchReporter.put(new Runnable() {
            @Override
            public void run() {
                new MRetryableTask<String, Void>() {

                    @Override
                    public Void run(String[] pushRegIds) {
                        MobileMessagingLogger.v("DEPERSONALIZE >>>");
                        mobileApiAppInstance.depersonalize(pushRegIds[0]);
                        return null;
                    }

                    @Override
                    public void before() {
                        serverListener.onServerDepersonalizeStarted();
                    }

                    @Override
                    public void after(Void aVoid) {
                        MobileMessagingLogger.v("DEPERSONALIZE <<<");
                        serverListener.onServerDepersonalizeCompleted();
                        broadcaster.depersonalized();
                    }

                    @Override
                    public void error(Throwable error) {
                        MobileMessagingLogger.v("DEPERSONALIZE ERROR <<<", error);
                        serverListener.onServerDepersonalizeFailed(error);
                        broadcaster.error(MobileMessagingError.createFrom(error));
                    }
                }
                        .retryWith(policy)
                        .execute(executor, mobileMessagingCore.getPushRegistrationId());
            }
        });
    }

    public void depersonalize(String unreportedDepersonalizedPushRegId, final DepersonalizeActionListener actionListener) {
        new MRetryableTask<String, Void>() {

            @Override
            public Void run(String[] pushRegIds) {
                MobileMessagingLogger.v("DEPERSONALIZE >>>");
                mobileApiAppInstance.depersonalize(pushRegIds[0]);
                return null;
            }

            @Override
            public void after(Void objects) {
                MobileMessagingLogger.v("DEPERSONALIZE <<<");
                if (actionListener != null) {
                    actionListener.onUserInitiatedDepersonalizeCompleted();
                }
                broadcaster.depersonalized();
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.v("DEPERSONALIZE ERROR <<<", error);
                if (actionListener != null) {
                    actionListener.onUserInitiatedDepersonalizeFailed(error);
                }
                broadcaster.error(MobileMessagingError.createFrom(error));
            }
        }
                .retryWith(policy)
                .execute(executor, unreportedDepersonalizedPushRegId);
    }
}
