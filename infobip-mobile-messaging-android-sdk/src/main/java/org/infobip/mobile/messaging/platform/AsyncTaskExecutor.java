package org.infobip.mobile.messaging.platform;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

public class AsyncTaskExecutor implements Executor {

    @SuppressLint("StaticFieldLeak")
    @Override
    public void execute(@NonNull final Runnable command) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                command.run();
                return null;
            }
        }.execute();
    }
}
