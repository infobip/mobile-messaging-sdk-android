/*
 * AsyncTaskExecutor.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.platform;

import android.annotation.SuppressLint;

import org.infobip.mobile.messaging.mobileapi.common.MMAsyncTask;

import java.util.concurrent.Executor;

import androidx.annotation.NonNull;

public class AsyncTaskExecutor implements Executor {

    @SuppressLint("StaticFieldLeak")
    @Override
    public void execute(@NonNull final Runnable command) {
        new MMAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                command.run();
                return null;
            }
        }.execute();
    }
}
