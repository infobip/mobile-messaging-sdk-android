package org.infobip.mobile.messaging.mobile.data;


import org.infobip.mobile.messaging.api.data.MobileApiData;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;

import java.util.concurrent.Executor;

public class LogoutUserSynchronizer {

    private final MobileApiData mobileApiData;
    private final Executor executor;
    private final BatchReporter batchReporter;
    private final MRetryPolicy policy;
    private final LogoutServerListener serverListener;

    private class LogoutTask extends MRetryableTask<Void, Void> {
        @Override
        public Void run(Void[] voids) {
            MobileMessagingLogger.v("LOGOUT USER >>>");
            mobileApiData.logoutUser();
            MobileMessagingLogger.v("LOGOUT USER <<<");
            return null;
        }
    }

    public LogoutUserSynchronizer(MobileApiData mobileApiData, MRetryPolicy policy, Executor executor, BatchReporter batchReporter, LogoutServerListener serverListener) {
        this.executor = executor;
        this.batchReporter = batchReporter;
        this.policy = policy;
        this.mobileApiData = mobileApiData;
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
                }.retryWith(policy).execute(executor);
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
        }.retryWith(policy).execute(executor);
    }
}
