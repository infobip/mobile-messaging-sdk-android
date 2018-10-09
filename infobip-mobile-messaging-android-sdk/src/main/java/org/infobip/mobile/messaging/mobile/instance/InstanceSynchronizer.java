package org.infobip.mobile.messaging.mobile.instance;

import org.infobip.mobile.messaging.api.instance.Instance;
import org.infobip.mobile.messaging.api.instance.MobileApiInstance;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.common.MRetryPolicy;
import org.infobip.mobile.messaging.mobile.common.MRetryableTask;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 20/06/2018.
 */
public class InstanceSynchronizer {

    private final Executor executor;
    private final MobileApiInstance mobileApiInstance;
    private final MRetryPolicy retryPolicy;

    public InstanceSynchronizer(
            Executor executor,
            MobileApiInstance mobileApiInstance,
            MRetryPolicy retryPolicy) {

        this.executor = executor;
        this.mobileApiInstance = mobileApiInstance;
        this.retryPolicy = retryPolicy;
    }

    public void fetch(final InstanceActionListener actionListener) {
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
                    if (actionListener != null) {
                        actionListener.onError(error);
                    }
                    MobileMessagingLogger.v("GET PRIMARY ERROR <<<", error);
                }

                @Override
                public void after(Boolean primary) {
                    if (actionListener != null) {
                        actionListener.onSuccess(primary);
                    }
                }
            }
            .retryWith(retryPolicy)
            .execute(executor);
    }

    public void sync(final Boolean primary, final InstanceActionListener actionListener) {
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
                    actionListener.onSuccess(primary);
                }
            }

            @Override
            public void error(Throwable error) {
                MobileMessagingLogger.v("UPDATE PRIMARY ERROR <<<", error);
                if (actionListener != null) {
                    actionListener.onError(error);
                }
            }
        }
        .retryWith(retryPolicy)
        .execute(executor, primary);
    }
}
