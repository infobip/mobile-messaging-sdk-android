package org.infobip.mobile.messaging.interactive.inapp.view;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.inapp.InAppWebViewMessage;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 13/04/2018.
 */
public class InAppViewFactory {

    private final Executor uiThreadExecutor;

    private class UiThreadExecutor implements Executor {

        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            handler.postDelayed(command, 100);
        }
    }

    public InAppViewFactory() {
        this.uiThreadExecutor = new UiThreadExecutor();
    }

    public InAppWebView create(Activity activity, InAppView.Callback callback, InAppWebViewMessage message) {
        return new InAppWebViewDialog(callback, new ActivityWrapper(activity));
    }

    public InAppNativeView create(Activity activity, InAppView.Callback callback, Message message) {
        return new InAppViewDialog(callback, uiThreadExecutor, new ActivityWrapper(activity));
    }
}
