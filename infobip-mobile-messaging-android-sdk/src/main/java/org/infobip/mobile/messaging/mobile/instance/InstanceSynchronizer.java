package org.infobip.mobile.messaging.mobile.instance;

import org.infobip.mobile.messaging.api.instance.Instance;
import org.infobip.mobile.messaging.api.instance.MobileApiInstance;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 20/06/2018.
 */
public class InstanceSynchronizer {

    public interface ServerListener {
        void onPrimaryFetchedFromServer(boolean primary);
    }

    public interface ActionListener {
        void onPrimarySetSuccess();
        void onPrimarySetError(Throwable error);
    }

    private final ServerListener serverListener;
    private final Executor executor;
    private final MobileApiInstance mobileApiInstance;
    private final BatchReporter batchReporter;
    private final MRetryPolicy retryPolicy;

    public InstanceSynchronizer(
            ServerListener serverListener,
            Executor executor,
            MobileApiInstance mobileApiInstance,
            BatchReporter batchReporter,
            MRetryPolicy retryPolicy) {

        this.serverListener = serverListener;
        this.executor = executor;
        this.mobileApiInstance = mobileApiInstance;
        this.batchReporter = batchReporter;
        this.retryPolicy = retryPolicy;
    }

    public void sync() {
        batchReporter.put(new Runnable() {
            @Override
            public void run() {

                new MRetryableTask<Void, Boolean>() {

                    @Override
                    public Boolean run(Void[] voids) {
                        MobileMessagingLogger.v("GET PRIMARY >>>");
                        Instance instance =  mobileApiInstance.get();
                        MobileMessagingLogger.v("GET PRIMARY <<<", instance);
                        return instance.getPrimary();
                    }

                    @Override
                    public void error(Throwable error) {
                        MobileMessagingLogger.v("GET PRIMARY ERROR <<<", error);
                    }

                    @Override
                    public void after(Boolean primary) {
                        serverListener.onPrimaryFetchedFromServer(primary);
                    }
                }
                .retryWith(retryPolicy)
                .execute(executor);
            }
        });
    }

    public void sendPrimary(Boolean primary) {
        sendPrimary(primary, null);
    }

    public void sendPrimary(final Boolean primary, final ActionListener actionListener) {
        new MRetryableTask<Boolean, Void>() {

            @Override
            public Void run(Boolean[] params) {
                Instance instance = new Instance(params[0]);
                MobileMessagingLogger.v("UPDATE PRIMARY >>>", instance);
                mobileApiInstance.update(instance);
                MobileMessagingLogger.v("UPDATE PRIMARY <<<");
                return null;
            }

            @Override
            public void after(Void aVoid) {
                if (actionListener != null) {
                    actionListener.onPrimarySetSuccess();
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.v("UPDATE PRIMARY ERROR <<<", error);
                if (actionListener != null) {
                    actionListener.onPrimarySetError(error);
                }
            }
        }
        .retryWith(retryPolicy)
        .execute(executor, primary);
    }
}
