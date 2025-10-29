/*
 * MRetryableTask.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.common;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;

/**
 * @author sslavin
 * @since 23/07/2017.
 */

public abstract class MRetryableTask<IN, OUT> extends IMAsyncTask<IN, OUT> {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private volatile ExecutionContext executionContext;
    private MRetryPolicy retryPolicy;

    private class ExecutionContext {
        final Executor executor;
        final IN[] args;
        final MRetryPolicy retryPolicy;
        int attempts;

        ExecutionContext(Executor executor, IN[] args, MRetryPolicy retryPolicy) {
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
            public boolean shouldCancel() {
                return MRetryableTask.this.shouldCancel();
            }

            @Override
            public OUT run(IN[] ins) {
                return MRetryableTask.this.run(ins);
            }

            @Override
            public void afterBackground(OUT out) {
                MRetryableTask.this.afterBackground(out);
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
                }, TimeUnit.SECONDS.toMillis((long) executionContext.attempts * executionContext.attempts * executionContext.retryPolicy.getBackoffMultiplier()));
            }

            @Override
            public void cancelled(IN[] ins) {
                MRetryableTask.this.cancelled(ins);
            }
        };

        if (executionContext.executor != null) {
            asyncTask.execute(executionContext.executor, executionContext.args);
        } else {
            asyncTask.execute(executionContext.args);
        }
    }
}
