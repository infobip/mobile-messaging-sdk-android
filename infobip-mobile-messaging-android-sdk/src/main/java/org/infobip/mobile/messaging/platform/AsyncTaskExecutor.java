package org.infobip.mobile.messaging.platform;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.mobileapi.common.MMAsyncTask;

import java.util.concurrent.Executor;

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
