/*
 * IMAsyncTask.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi.common;

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class IMAsyncTask<IN, OUT> {

    /**
     * Executed on UI thread before background processing.
     */
    public void before() {
    }

    /**
     * Executed on background thread as a last step before running the main block
     *
     * @return return true to skip execution, in this case only the {@link IMAsyncTask#cancelled(Object[])} will be called.
     */
    public boolean shouldCancel() {
        return false;
    }

    /**
     * Executed in background thread.
     * If there is any exception in background callback,
     * it will be propagated to {@link IMAsyncTask#error(Throwable)}.
     *
     * @param ins input parameters
     * @return result of operation
     */
    public abstract OUT run(IN[] ins);

    /**
     * Executed on background thread right after main block
     *
     * @param out results
     */
    public void afterBackground(OUT out) {

    }

    /**
     * Executed on UI thread after successful processing.
     * Not executed in case of error.
     *
     * @param out result of background operation.
     */
    public void after(OUT out) {

    }

    /**
     * Executed on UI thread in case of error.
     *
     * @param error error that happened during background execution.
     */
    public void error(Throwable error) {

    }

    /**
     * Executed on UI thread in case of error.
     *
     * @param ins   original input parameters.
     * @param error error that happened during background execution.
     */
    public void error(IN[] ins, Throwable error) {

    }

    /**
     * Executed on UI thread if execution was cancelled ({@link IMAsyncTask#shouldCancel()} returned `true`).
     *
     * @param ins input parameters
     */
    public void cancelled(IN[] ins) {

    }
}
