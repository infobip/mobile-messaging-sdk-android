package org.infobip.mobile.messaging;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * @author sslavin
 * @since 09/09/16.
 */
public class BootReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MobileMessagingLogger.i("Received boot completed intent");
        MobileMessagingCore.getInstance(context).handleBootCompleted();
    }
}
