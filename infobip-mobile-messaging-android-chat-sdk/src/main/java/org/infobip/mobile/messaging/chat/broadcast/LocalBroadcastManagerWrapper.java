package org.infobip.mobile.messaging.chat.broadcast;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Needed for proper testing
 *
 * @author sslavin
 * @since 10/10/2017.
 */


public class LocalBroadcastManagerWrapper {

    private final LocalBroadcastManager localBroadcastManager;

    public LocalBroadcastManagerWrapper(Context context) {
        localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    public void sendBroadcast(Intent intent) {
        localBroadcastManager.sendBroadcast(intent);
    }
}
