package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.crashlytics.android.Crashlytics;

/**
 * @author mstipanov
 * @since 04.04.2016.
 */
public class ApiExceptionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Throwable e = (Throwable) intent.getSerializableExtra("exception");
        if (null == e) {
            return;
        }

        Crashlytics.logException(e);
    }
}
