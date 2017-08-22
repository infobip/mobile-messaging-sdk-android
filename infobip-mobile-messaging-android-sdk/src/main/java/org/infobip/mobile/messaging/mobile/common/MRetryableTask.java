package org.infobip.mobile.messaging.mobile.common;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 23/07/2017.
 */

public abstract class MRetryableTask<IN, OUT> {

    private Handler handler = new Handler(Looper.getMainLooper());

    private volatile ExecutionContext executionContext;
    private MRetryPolicy retryPolicy;

    private class ExecutionContext {
        Executor executor;
        IN args[];
        int attempts;
        MRetryPolicy retryPolicy;

        ExecutionContext(Executor executor, IN args[], MRetryPolicy retryPolicy) {
            this.executor = executor;
            this.args = args;
            if (retryPolicy == null) {
                this.retryPolicy = new MRetryPolicy.Builder().build();
            } else {
                this.retryPolicy = retryPolicy;
            }
        }
    }

    /**
     * Executed on UI thread before background processing.
     */
    public void before() {
    }

    /**
     * Main background work should be done in this method.
     * @param ins input arguments
     * @return result
     */
    public abstract OUT run(IN ins[]);

    /**
     * Executed on UI thread after successful processing.
     * Not executed in case of error.
     *
     * @param out result of background operation.
     */
    public void after(OUT out) {

    }

    /**
     * Executed after all retries were exhausted or no more retries planned
     *
     * @param error final error
     */
    public void error(Throwable error) {

    }

    /**
     * Executed after all retries were exhausted or no more retries planned
     * @param ins original input arguments
     * @param error final error
     */
    public void error(IN ins[], Throwable error) {

    }

    /**
     * Specifies policy to use when performing retries
     *
     * @param retryPolicy required policy
     * @return self
     */
    @SuppressWarnings("unused")
    public MRetryableTask<IN, OUT> retryWith(MRetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        return this;
    }

    @SafeVarargs
    public final void execute(IN... ins) {
        executionContext = new ExecutionContext(null, ins, retryPolicy);
        execute();
    }

    @SafeVarargs
    public final void execute(@Nullable Executor executor, IN... ins) {
        executionContext = new ExecutionContext(executor, ins, retryPolicy);
        execute();
    }

    private void execute() {
        MAsyncTask<IN, OUT> asyncTask = new MAsyncTask<IN, OUT>() {

            @Override
            public void before() {
                if (executionContext.attempts > 0) {
                    return;
                }

                MRetryableTask.this.before();
            }

            @Override
            public OUT run(IN[] ins) {
                return MRetryableTask.this.run(ins);
            }

            @Override
            public void after(OUT out) {
                MRetryableTask.this.after(out);
            }

            @Override
            public void error(Throwable error) {
                if (!executionContext.retryPolicy.shouldRetry(error, executionContext.attempts)) {
                    MRetryableTask.this.error(error);
                    MRetryableTask.this.error(executionContext.args, error);
                    executionContext = null;
                    return;
                }

                executionContext.attempts++;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MRetryableTask.this.execute();
                    }
                }, TimeUnit.SECONDS.toMillis(executionContext.attempts * executionContext.attempts * executionContext.retryPolicy.getBackoffMultiplier()));
            }
        };

        if (executionContext.executor != null) {
            asyncTask.execute(executionContext.executor, executionContext.args);
        } else {
            asyncTask.execute(executionContext.args);
        }
    }
}
