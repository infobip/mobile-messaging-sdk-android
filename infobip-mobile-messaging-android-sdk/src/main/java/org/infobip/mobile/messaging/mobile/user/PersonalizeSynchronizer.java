package org.infobip.mobile.messaging.mobile.user;


import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;

import java.util.concurrent.Executor;

public class PersonalizeSynchronizer {

    private final MobileMessagingCore mobileMessagingCore;
    private final MobileApiAppInstance mobileApiAppInstance;
    private final Executor executor;
    private final BatchReporter batchReporter;
    private final MRetryPolicy policy;
    private final DepersonalizeServerListener serverListener;

    private class DepersonalizeTask extends MRetryableTask<String, Void> {
        @Override
        public Void run(String[] pushRegIds) {
            MobileMessagingLogger.v("DEPERSONALIZE >>>");
            mobileApiAppInstance.depersonalize(pushRegIds[0]);
            MobileMessagingLogger.v("DEPERSONALIZE <<<");
            return null;
        }
    }

    private class PersonalizeTask extends MRetryableTask<Boolean, Void> {
        @Override
        public Void run(Boolean[] forceDepersonalize) {
            MobileMessagingLogger.v("PERSONALIZE >>>");
            mobileApiAppInstance.personalize(mobileMessagingCore.getPushRegistrationId(), forceDepersonalize[0]);
            MobileMessagingLogger.v("PERSONALIZE <<<");
            return null;
        }
    }

    public PersonalizeSynchronizer(
            MobileMessagingCore mobileMessagingCore,
            MobileApiAppInstance mobileApiAppInstance,
            MRetryPolicy policy,
            Executor executor,
            BatchReporter batchReporter,
            DepersonalizeServerListener serverListener) {

        this.mobileMessagingCore = mobileMessagingCore;
        this.mobileApiAppInstance = mobileApiAppInstance;
        this.policy = policy;
        this.executor = executor;
        this.batchReporter = batchReporter;
        this.serverListener = serverListener;
    }

    public void personalize(boolean forceDepersonalize) {
        new PersonalizeTask() {

        }.retryWith(policy)
                .execute(executor, forceDepersonalize);
    }

    public void depersonalize() {
        batchReporter.put(new Runnable() {
            @Override
            public void run() {
                new DepersonalizeTask() {

                    @Override
                    public void before() {
                        serverListener.onServerDepersonalizeStarted();
                    }

                    @Override
                    public void error(Throwable error) {
                        MobileMessagingLogger.v("DEPERSONALIZE ERROR <<<", error);
                        serverListener.onServerDepersonalizeFailed(error);
                    }

                    @Override
                    public void after(Void aVoid) {
                        serverListener.onServerDepersonalizeCompleted();
                    }
                }
                        .retryWith(policy)
                        .execute(executor, mobileMessagingCore.getPushRegistrationId());
            }
        });
    }

    public void depersonalize(String unreportedDepersonalizedPushRegId, final DepersonalizeActionListener actionListener) {
        new DepersonalizeTask() {
            @Override
            public void after(Void objects) {
                if (actionListener != null) {
                    actionListener.onUserInitiatedDepersonalizeCompleted();
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.v("DEPERSONALIZE ERROR <<<", error);
                if (actionListener != null) {
                    actionListener.onUserInitiatedDepersonalizeFailed(error);
                }
            }
        }
                .retryWith(policy)
                .execute(executor, unreportedDepersonalizedPushRegId);
    }
}
