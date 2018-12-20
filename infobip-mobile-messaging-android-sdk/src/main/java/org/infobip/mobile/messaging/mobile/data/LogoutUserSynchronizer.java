package org.infobip.mobile.messaging.mobile.data;


import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;

import java.util.concurrent.Executor;

public class LogoutUserSynchronizer {

    private final MobileMessagingCore mobileMessagingCore;
    private final MobileApiAppInstance mobileApiAppInstance;
    private final Executor executor;
    private final BatchReporter batchReporter;
    private final MRetryPolicy policy;
    private final LogoutServerListener serverListener;

    private class LogoutTask extends MRetryableTask<String, Void> {
        @Override
        public Void run(String[] pushRegIds) {
            MobileMessagingLogger.v("LOGOUT USER >>>");
            mobileApiAppInstance.logoutUser(pushRegIds[0]);
            MobileMessagingLogger.v("LOGOUT USER <<<");
            return null;
        }
    }

    public LogoutUserSynchronizer(
            MobileMessagingCore mobileMessagingCore,
            MobileApiAppInstance mobileApiAppInstance,
            MRetryPolicy policy,
            Executor executor,
            BatchReporter batchReporter,
            LogoutServerListener serverListener) {

        this.mobileMessagingCore = mobileMessagingCore;
        this.mobileApiAppInstance = mobileApiAppInstance;
        this.policy = policy;
        this.executor = executor;
        this.batchReporter = batchReporter;
        this.serverListener = serverListener;
    }

    public void logout() {
        batchReporter.put(new Runnable() {
            @Override
            public void run() {
                new LogoutTask() {

                    @Override
                    public void before() {
                        serverListener.onServerLogoutStarted();
                    }

                    @Override
                    public void error(Throwable error) {
                        MobileMessagingLogger.v("LOGOUT USER ERROR <<<", error);
                        serverListener.onServerLogoutFailed(error);
                    }

                    @Override
                    public void after(Void aVoid) {
                        serverListener.onServerLogoutCompleted();
                    }
                }
                        .retryWith(policy)
                        .execute(executor, mobileMessagingCore.getUnreportedLogoutPushRegId());
            }
        });
    }

    public void logout(final LogoutActionListener actionListener) {
        new LogoutTask() {
            @Override
            public void after(Void objects) {
                if (actionListener != null) {
                    actionListener.onUserInitiatedLogoutCompleted();
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.v("LOGOUT USER ERROR <<<", error);
                if (actionListener != null) {
                    actionListener.onUserInitiatedLogoutFailed(error);
                }
            }
        }
                .retryWith(policy)
                .execute(executor, mobileMessagingCore.getUnreportedLogoutPushRegId());
    }
}
