package org.infobip.mobile.messaging.mobileapi.user;


import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserAttributes;
import org.infobip.mobile.messaging.UserIdentity;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.api.appinstance.UserPersonalizeBody;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.BatchReporter;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.mobileapi.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobileapi.common.MRetryableTask;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendInvalidParameterException;
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

    public void personalize(final UserIdentity userIdentity, final UserAttributes userAttributes, final boolean forceDepersonalize, boolean keepAsLead, final MobileMessaging.ResultListener<User> listener) {
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
                mobileApiAppInstance.personalize(mobileMessagingCore.getPushRegistrationId(), forceDepersonalize, keepAsLead, userPersonalizeBody);
                return null;
            }

            @Override
            public void after(Void aVoid) {
                MobileMessagingLogger.v("PERSONALIZE DONE <<<");
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

                mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);
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
                        MobileMessagingLogger.v("DEPERSONALIZE DONE <<<");
                        serverListener.onServerDepersonalizeCompleted();
                        broadcaster.depersonalized();
                    }

                    @Override
                    public void error(Throwable error) {
                        MobileMessagingLogger.v("DEPERSONALIZE ERROR <<<", error);
                        MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);
                        serverListener.onServerDepersonalizeFailed(error);
                        broadcaster.error(mobileMessagingError);
                        mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);
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
                MobileMessagingLogger.v("DEPERSONALIZE DONE <<<");
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

    public void repersonalize() {
        final User reportedUser = mobileMessagingCore.getUser();
        if (reportedUser == null) {
            // no user data is present on SDK storage, old token was invalidated by backend (push reg ID not registered), new token isn't provided
            // forcing generating of new token to update it
            mobileMessagingCore.resetCloudToken(false);
            return;
        }

        final UserIdentity userIdentity = new UserIdentity();
        userIdentity.setExternalUserId(reportedUser.getExternalUserId());
        userIdentity.setEmails(reportedUser.getEmails());
        userIdentity.setPhones(reportedUser.getPhones());
        final UserAttributes userAttributes = new UserAttributes(
                reportedUser.getFirstName(),
                reportedUser.getLastName(),
                reportedUser.getMiddleName(),
                reportedUser.getGender(),
                null,
                reportedUser.getTags(),
                reportedUser.getCustomAttributes());
        userAttributes.setBirthday(reportedUser.getBirthday());

        final UserPersonalizeBody userPersonalizeBody = new UserPersonalizeBody();
        userPersonalizeBody.setUserIdentity(userIdentity.getMap());
        if (userAttributes.hasDataToReport()) {
            userPersonalizeBody.setUserAttributes(userAttributes.getMap());
        }

        if (!userIdentity.hasDataToReport() && !userAttributes.hasDataToReport() ||
                userPersonalizeBody.getUserIdentity().isEmpty() && userPersonalizeBody.getUserAttributes().isEmpty()) {
            // no user data is present on SDK storage, old token was invalidated by backend (push reg ID not registered), new token isn't provided
            // forcing generating of new token to update it
            mobileMessagingCore.resetCloudToken(false);
            return;
        }

        if (StringUtils.isBlank(mobileMessagingCore.getPushRegistrationId())) {
            MobileMessagingLogger.w("Registration not available yet, will repersonalize user later");
            return;
        }

        new MRetryableTask<UserPersonalizeBody, Void>() {

            @Override
            public Void run(UserPersonalizeBody[] userPersonalizeBodies) {
                MobileMessagingLogger.v("REPERSONALIZE >>>", userPersonalizeBody);
                mobileApiAppInstance.repersonalize(mobileMessagingCore.getPushRegistrationId(), userPersonalizeBody);
                MobileMessagingLogger.v("REPERSONALIZE DONE <<<");
                return null;
            }

            @Override
            public void after(Void aVoid) {
                mobileMessagingCore.setShouldRepersonalize(false);
                mobileMessagingCore.setUserDataReported(new User(userIdentity, userAttributes), true);

                User userToReturn = mobileMessagingCore.getUser();
                broadcaster.personalized(userToReturn);
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.e("MobileMessaging API returned error (repersonalize)! ", error);
                MobileMessagingError mobileMessagingError = MobileMessagingError.createFrom(error);

                if (error instanceof BackendInvalidParameterException) {
                    mobileMessagingCore.handleNoRegistrationError(mobileMessagingError);
                    mobileMessagingCore.setUserDataReportedWithError();
                } else {
                    MobileMessagingLogger.v("Repersonalization will be postponed to a later time due to communication error");
                }
            }
        }
                .retryWith(policy)
                .execute(executor, userPersonalizeBody);
    }
}
