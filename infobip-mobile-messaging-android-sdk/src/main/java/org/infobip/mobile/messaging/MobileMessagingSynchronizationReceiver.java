package org.infobip.mobile.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author sslavin
 * @since 31/05/2017.
 */

public class MobileMessagingSynchronizationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LocalEvent.APPLICATION_FOREGROUND.getKey().equals(intent.getAction())) {
            MobileMessagingCore.getInstance(context).sync();
        }
    }
}
